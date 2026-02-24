package game.server.game;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import game.common.Constants;
import game.common.model.Board;
import game.common.model.CellState;
import game.common.model.Coordinate;
import game.common.model.Direction;
import game.common.model.GameMode;
import game.common.model.Ship;
import game.common.model.WeaponType;

/**
 * Manages a single game session between two players.
 * Translates between JSON messages and GameState operations.
 */
public class GameSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameSession.class);

    private final String sessionId;
    private GameState gameState;
    private final String[] sessionKeys; // WebSocket session IDs
    private GameMode[] modeVotes; // Each player's mode vote

    // Play again
    private final Boolean[] playAgainVotes = new Boolean[2];

    // Turn timeout
    private static final ScheduledExecutorService TIMER_POOL = new ScheduledThreadPoolExecutor(2, r -> {
        Thread t = new Thread(r, "turn-timer");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> turnTimeoutFuture;
    private Runnable onTurnTimeout; // callback set by the endpoint

    public GameSession(String sessionId) {
        this.sessionId = sessionId;
        this.gameState = new GameState();
        this.sessionKeys = new String[2];
        this.modeVotes = new GameMode[2];
    }

    public String getSessionId() {
        return sessionId;
    }

    public GameState getGameState() {
        return gameState;
    }

    // --- Player Management ---

    /**
     * Adds a player to this session.
     *
     * @return player index (0 or 1), or -1 if full
     */
    public int addPlayer(String wsSessionId, String username) {
        int index = gameState.addPlayer(username);
        if (index >= 0) {
            sessionKeys[index] = wsSessionId;
        }
        return index;
    }

    /**
     * Returns the player index for a given WebSocket session ID, or -1.
     */
    public int getPlayerIndex(String wsSessionId) {
        for (int i = 0; i < 2; i++) {
            if (wsSessionId.equals(sessionKeys[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the WebSocket session ID for a given player index.
     */
    public String getSessionKey(int playerIndex) {
        return sessionKeys[playerIndex];
    }

    public boolean isFull() {
        return gameState.isFull();
    }

    // --- Mode Selection ---

    /**
     * Records a player's mode vote. Returns resolved mode if both voted, null
     * otherwise.
     */
    public GameMode voteMode(int playerIndex, GameMode mode) {
        modeVotes[playerIndex] = mode;
        LOGGER.info("Player {} voted for {}", playerIndex, mode.getDisplayName());

        if (modeVotes[0] != null && modeVotes[1] != null) {
            return resolveMode();
        }
        return null;
    }

    private GameMode resolveMode() {
        if (modeVotes[0] == modeVotes[1]) {
            return modeVotes[0];
        }
        // Different votes — pick randomly
        return Math.random() < 0.5 ? modeVotes[0] : modeVotes[1];
    }

    // --- Game Actions ---

    /**
     * Starts the game with the given mode.
     */
    public void startGame(GameMode mode) {
        gameState.setup(mode);
    }

    /**
     * Processes an attack from a player.
     *
     * @return the AttackResult, or null if invalid
     */
    public AttackResult processAttack(int playerIndex, WeaponType weapon,
            Coordinate target, Direction direction) {
        cancelTurnTimeout();
        return gameState.executeAttack(playerIndex, weapon, target, direction);
    }

    // --- Turn Timeout ---

    /**
     * Sets a callback to be invoked when a turn times out.
     */
    public void setOnTurnTimeout(Runnable callback) {
        this.onTurnTimeout = callback;
    }

    /**
     * Starts the turn timeout timer. When it fires, the current player forfeits.
     */
    public void startTurnTimeout() {
        cancelTurnTimeout();
        turnTimeoutFuture = TIMER_POOL.schedule(() -> {
            LOGGER.info("Session {}: Turn timeout for player {}",
                    sessionId, gameState.getCurrentTurnIndex());
            if (onTurnTimeout != null) {
                onTurnTimeout.run();
            }
        }, Constants.TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Cancels the current turn timeout timer.
     */
    public void cancelTurnTimeout() {
        if (turnTimeoutFuture != null && !turnTimeoutFuture.isDone()) {
            turnTimeoutFuture.cancel(false);
            turnTimeoutFuture = null;
        }
    }

    // --- Play Again ---

    /**
     * Records a play-again vote.
     *
     * @return null if waiting for other player, true if both want to play again,
     *         false if either declined
     */
    public Boolean votePlayAgain(int playerIndex, boolean wantsToPlay) {
        playAgainVotes[playerIndex] = wantsToPlay;
        LOGGER.info("Player {} voted play again: {}", playerIndex, wantsToPlay);

        if (playAgainVotes[0] != null && playAgainVotes[1] != null) {
            return playAgainVotes[0] && playAgainVotes[1];
        }
        return null; // still waiting
    }

    /**
     * Resets the session for a new game (same players).
     */
    public void resetForNewGame() {
        String username0 = gameState.getPlayer(0).getUsername();
        String username1 = gameState.getPlayer(1).getUsername();

        this.gameState = new GameState();
        gameState.addPlayer(username0);
        gameState.addPlayer(username1);

        this.modeVotes = new GameMode[2];
        this.playAgainVotes[0] = null;
        this.playAgainVotes[1] = null;

        LOGGER.info("Session {} reset for new game: {} vs {}",
                sessionId, username0, username1);
    }

    /**
     * Returns true if the game is over.
     */
    public boolean isGameOver() {
        return gameState.getPhase() == GameState.Phase.GAME_OVER;
    }

    // --- JSON Message Builders ---

    /**
     * Builds the lobby_waiting message.
     */
    public JsonObject buildLobbyWaitingMessage() {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_LOBBY_WAITING);
        payload.addProperty("message", "Waiting for an opponent to join...");
        return payload;
    }

    /**
     * Builds the mode selection prompt.
     */
    public JsonObject buildModeSelectMessage(String opponentName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_LOBBY_MODE_SELECT);
        payload.addProperty("opponentName", opponentName);

        JsonArray modes = new JsonArray();
        for (GameMode mode : GameMode.values()) {
            JsonObject modeObj = new JsonObject();
            modeObj.addProperty("name", mode.name());
            modeObj.addProperty("displayName", mode.getDisplayName());
            modeObj.addProperty("gridSize", mode.getGridSize());
            modeObj.addProperty("shipCount", mode.getShipCount());
            modes.add(modeObj);
        }
        payload.add("modes", modes);
        return payload;
    }

    /**
     * Builds the game_start message for a specific player.
     */
    public JsonObject buildGameStartMessage(int playerIndex) {
        PlayerState player = gameState.getPlayer(playerIndex);
        int opponentIndex = 1 - playerIndex;
        PlayerState opponent = gameState.getPlayer(opponentIndex);

        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_GAME_START);
        payload.addProperty("mode", gameState.getMode().name());
        payload.addProperty("gridSize", gameState.getMode().getGridSize());
        payload.addProperty("opponentName", opponent.getUsername());

        // Send this player's ship positions
        payload.add("yourShips", serializeShips(player.getBoard().getShips()));

        // Send this player's full board
        payload.add("yourBoard", serializeBoard(player.getBoard(), true));

        return payload;
    }

    /**
     * Builds the your_turn message.
     */
    public JsonObject buildYourTurnMessage(int playerIndex) {
        PlayerState player = gameState.getPlayer(playerIndex);

        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_YOUR_TURN);
        payload.addProperty("turnNumber", gameState.getTurnNumber());
        payload.addProperty("timeoutSeconds", Constants.TURN_TIMEOUT_SECONDS);

        // Available weapons
        List<WeaponType> available = player.getCooldownManager().getAvailableWeapons(player.getBoard());
        JsonArray weaponsArray = new JsonArray();
        for (WeaponType weapon : WeaponType.values()) {
            JsonObject weaponObj = new JsonObject();
            weaponObj.addProperty("name", weapon.name());
            weaponObj.addProperty("displayName", weapon.getDisplayName());
            weaponObj.addProperty("description", weapon.getDescription());
            weaponObj.addProperty("available", available.contains(weapon));
            weaponObj.addProperty("cooldownRemaining",
                    player.getCooldownManager().getRemainingCooldown(weapon));
            weaponObj.addProperty("needsDirection", weapon == WeaponType.LINE_BARRAGE);
            weaponsArray.add(weaponObj);
        }
        payload.add("weapons", weaponsArray);

        // Fleet status
        payload.add("fleetStatus", serializeFleetStatus(player.getBoard()));

        // Recent messages
        payload.add("messages", serializeMessages());

        return payload;
    }

    /**
     * Builds the wait_turn message.
     */
    public JsonObject buildWaitTurnMessage(int playerIndex) {
        int opponentIndex = 1 - playerIndex;
        String opponentName = gameState.getPlayer(opponentIndex).getUsername();

        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_WAIT_TURN);
        payload.addProperty("message", "Waiting for " + opponentName + "'s move...");
        payload.addProperty("turnNumber", gameState.getTurnNumber());

        // Still send fleet status and messages so the HUD can update
        payload.add("fleetStatus", serializeFleetStatus(
                gameState.getPlayer(playerIndex).getBoard()));
        payload.add("messages", serializeMessages());

        return payload;
    }

    /**
     * Builds the attack_result message sent to the attacker.
     */
    public JsonObject buildAttackResultMessage(AttackResult result, int attackerIndex) {
        PlayerState attacker = gameState.getPlayer(attackerIndex);
        int defenderIndex = 1 - attackerIndex;
        PlayerState defender = gameState.getPlayer(defenderIndex);

        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_ATTACK_RESULT);
        payload.addProperty("weapon", result.getWeapon().name());
        payload.addProperty("target", result.getTarget().toDisplayString());

        // Tile results
        JsonArray tilesArray = new JsonArray();
        for (AttackResult.TileResult tr : result.getTileResults()) {
            JsonObject tile = new JsonObject();
            tile.addProperty("coordinate", tr.coordinate().toDisplayString());
            tile.addProperty("outcome", tr.outcome().name());
            tilesArray.add(tile);
        }
        payload.add("tileResults", tilesArray);

        // Sunk ships
        JsonArray sunkArray = new JsonArray();
        for (Ship ship : result.getSunkShips()) {
            JsonObject sunkObj = new JsonObject();
            sunkObj.addProperty("id", ship.getId());
            sunkObj.addProperty("type", ship.getType().name());
            sunkObj.addProperty("displayName", ship.getType().getDisplayName());
            sunkArray.add(sunkObj);
        }
        payload.add("sunkShips", sunkArray);

        payload.addProperty("hits", result.hitCount());
        payload.addProperty("misses", result.missCount());
        payload.addProperty("gameOver", result.isGameOver());

        // Updated enemy tracking board (what attacker knows about defender)
        payload.add("enemyBoard", serializeBoard(defender.getBoard(), false));

        // Updated own fleet status
        payload.add("fleetStatus", serializeFleetStatus(attacker.getBoard()));

        // Messages
        payload.add("messages", serializeMessages());

        return payload;
    }

    /**
     * Builds the incoming_attack message sent to the defender.
     */
    public JsonObject buildIncomingAttackMessage(AttackResult result, int defenderIndex) {
        PlayerState defender = gameState.getPlayer(defenderIndex);
        int attackerIndex = 1 - defenderIndex;
        String attackerName = gameState.getPlayer(attackerIndex).getUsername();

        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_INCOMING_ATTACK);
        payload.addProperty("attackerName", attackerName);
        payload.addProperty("weapon", result.getWeapon().name());
        payload.addProperty("target", result.getTarget().toDisplayString());

        // Tile results
        JsonArray tilesArray = new JsonArray();
        for (AttackResult.TileResult tr : result.getTileResults()) {
            JsonObject tile = new JsonObject();
            tile.addProperty("coordinate", tr.coordinate().toDisplayString());
            tile.addProperty("outcome", tr.outcome().name());
            tilesArray.add(tile);
        }
        payload.add("tileResults", tilesArray);

        // Sunk ships
        JsonArray sunkArray = new JsonArray();
        for (Ship ship : result.getSunkShips()) {
            JsonObject sunkObj = new JsonObject();
            sunkObj.addProperty("id", ship.getId());
            sunkObj.addProperty("type", ship.getType().name());
            sunkObj.addProperty("displayName", ship.getType().getDisplayName());
            sunkArray.add(sunkObj);
        }
        payload.add("sunkShips", sunkArray);

        payload.addProperty("hits", result.hitCount());
        payload.addProperty("misses", result.missCount());
        payload.addProperty("gameOver", result.isGameOver());

        // Updated own board
        payload.add("yourBoard", serializeBoard(defender.getBoard(), true));

        // Updated fleet status
        payload.add("fleetStatus", serializeFleetStatus(defender.getBoard()));

        // Messages
        payload.add("messages", serializeMessages());

        return payload;
    }

    /**
     * Builds the game_over message for a specific player.
     */
    public JsonObject buildGameOverMessage(int playerIndex) {
        PlayerState player = gameState.getPlayer(playerIndex);
        int opponentIndex = 1 - playerIndex;
        PlayerState opponent = gameState.getPlayer(opponentIndex);
        boolean isWinner = player.getUsername().equals(gameState.getWinnerUsername());

        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_GAME_OVER);
        payload.addProperty("winner", gameState.getWinnerUsername());
        payload.addProperty("youWon", isWinner);

        // Stats for this player
        JsonObject stats = new JsonObject();
        stats.addProperty("turnsTaken", player.getTurnsTaken());
        stats.addProperty("shotsFired", player.getShotsFired());
        stats.addProperty("shotsHit", player.getShotsHit());
        stats.addProperty("hitRate", String.format("%.1f%%", player.getHitRate()));
        stats.addProperty("shipsLost", player.shipsLost());
        stats.addProperty("totalShips", player.totalShips());
        payload.add("yourStats", stats);

        // Opponent stats
        JsonObject oppStats = new JsonObject();
        oppStats.addProperty("turnsTaken", opponent.getTurnsTaken());
        oppStats.addProperty("shotsFired", opponent.getShotsFired());
        oppStats.addProperty("shotsHit", opponent.getShotsHit());
        oppStats.addProperty("hitRate", String.format("%.1f%%", opponent.getHitRate()));
        oppStats.addProperty("shipsLost", opponent.shipsLost());
        oppStats.addProperty("totalShips", opponent.totalShips());
        payload.add("opponentStats", oppStats);

        // Messages
        payload.add("messages", serializeMessages());

        return payload;
    }

    /**
     * Builds the play-again prompt message.
     */
    public JsonObject buildPlayAgainPromptMessage() {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_PLAY_AGAIN_PROMPT);
        payload.addProperty("message", "Play again? (yes/no)");
        return payload;
    }

    /**
     * Builds the play-again waiting message.
     */
    public JsonObject buildPlayAgainWaitingMessage() {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_PLAY_AGAIN_WAITING);
        payload.addProperty("message", "Waiting for opponent's decision...");
        return payload;
    }

    /**
     * Builds an error message.
     */
    public static JsonObject buildErrorMessage(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_ERROR);
        payload.addProperty("message", message);
        return payload;
    }

    /**
     * Builds an opponent_disconnected message.
     */
    public static JsonObject buildOpponentDisconnectedMessage(String opponentName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_OPPONENT_DISCONNECTED);
        payload.addProperty("message", opponentName + " has disconnected.");
        return payload;
    }

    // --- Serialization Helpers ---

    private JsonArray serializeShips(List<Ship> ships) {
        JsonArray array = new JsonArray();
        for (Ship ship : ships) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", ship.getId());
            obj.addProperty("type", ship.getType().name());
            obj.addProperty("displayName", ship.getType().getDisplayName());
            obj.addProperty("sunk", ship.isSunk());
            obj.addProperty("health", ship.remainingHealth());
            obj.addProperty("maxHealth", ship.getOccupiedCells().size());

            JsonArray cells = new JsonArray();
            for (Coordinate c : ship.getOccupiedCells()) {
                cells.add(c.toDisplayString());
            }
            obj.add("cells", cells);

            JsonArray hitCells = new JsonArray();
            for (Coordinate c : ship.getHitCells()) {
                hitCells.add(c.toDisplayString());
            }
            obj.add("hitCells", hitCells);

            array.add(obj);
        }
        return array;
    }

    /**
     * Serializes a board to JSON.
     *
     * @param board    the board to serialize
     * @param fullView if true, shows all cell states (own board);
     *                 if false, hides SHIP cells as EMPTY (enemy tracking view)
     */
    private JsonObject serializeBoard(Board board, boolean fullView) {
        JsonObject obj = new JsonObject();
        obj.addProperty("size", board.getSize());

        JsonArray rows = new JsonArray();
        for (int r = 0; r < board.getSize(); r++) {
            JsonArray row = new JsonArray();
            for (int c = 0; c < board.getSize(); c++) {
                CellState state = board.getCell(r, c);
                if (!fullView && state == CellState.SHIP) {
                    state = CellState.EMPTY; // fog of war
                }
                row.add(state.name());
            }
            rows.add(row);
        }
        obj.add("grid", rows);
        return obj;
    }

    private JsonArray serializeFleetStatus(Board board) {
        JsonArray array = new JsonArray();
        for (Ship ship : board.getShips()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", ship.getId());
            obj.addProperty("type", ship.getType().getCode());
            obj.addProperty("alive", !ship.isSunk());
            obj.addProperty("health", ship.remainingHealth());
            obj.addProperty("maxHealth", ship.getOccupiedCells().size());
            array.add(obj);
        }
        return array;
    }

    private JsonArray serializeMessages() {
        JsonArray array = new JsonArray();
        for (String msg : gameState.getRecentMessages(Constants.MAX_RECENT_MESSAGES)) {
            array.add(msg);
        }
        return array;
    }
}
