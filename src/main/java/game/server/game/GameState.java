package game.server.game;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import game.common.model.Board;
import game.common.model.Coordinate;
import game.common.model.Direction;
import game.common.model.GameMode;
import game.common.model.WeaponType;

/**
 * Manages the full state of a single game between two players.
 * This is the main entry point for game logic on the server.
 */
public class GameState {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameState.class);

    public enum Phase {
        LOBBY,
        SETUP,
        IN_PROGRESS,
        GAME_OVER
    }

    private Phase phase;
    private GameMode mode;
    private final PlayerState[] players;
    private int currentTurnIndex; // 0 or 1
    private int turnNumber;
    private String winnerUsername;
    private final List<String> messageLog;

    private final ShipPlacer shipPlacer;
    private final AttackResolver attackResolver;

    public GameState() {
        this.phase = Phase.LOBBY;
        this.players = new PlayerState[2];
        this.currentTurnIndex = 0;
        this.turnNumber = 0;
        this.messageLog = new ArrayList<>();
        this.shipPlacer = new ShipPlacer();
        this.attackResolver = new AttackResolver();
    }

    // --- Lobby Phase ---

    /**
     * Adds a player to the game. Returns the player index (0 or 1).
     * Returns -1 if the game is already full.
     */
    public int addPlayer(String username) {
        if (phase != Phase.LOBBY) {
            return -1;
        }
        for (int i = 0; i < 2; i++) {
            if (players[i] == null) {
                // Player added with null board — board assigned during setup
                players[i] = new PlayerState(username, null);
                LOGGER.info("Player {} joined as player {}", username, i);
                return i;
            }
        }
        return -1; // full
    }

    /**
     * Returns true if both players have joined.
     */
    public boolean isFull() {
        return players[0] != null && players[1] != null;
    }

    // --- Setup Phase ---

    /**
     * Initializes the game with the given mode.
     * Generates boards with random ship placements for both players.
     */
    public void setup(GameMode mode) {
        if (!isFull()) {
            throw new IllegalStateException("Cannot start game without 2 players.");
        }

        this.mode = mode;
        this.phase = Phase.SETUP;

        // Generate boards with random ship placement
        Board board0 = shipPlacer.placeShips(mode);
        Board board1 = shipPlacer.placeShips(mode);

        // Recreate PlayerState with actual boards
        players[0] = new PlayerState(players[0].getUsername(), board0);
        players[1] = new PlayerState(players[1].getUsername(), board1);

        this.currentTurnIndex = 0;
        this.turnNumber = 1;
        this.phase = Phase.IN_PROGRESS;

        // Tick cooldowns for the first player (all start at 0 anyway)
        players[currentTurnIndex].getCooldownManager().tickCooldowns();

        addMessage("Game started! Mode: " + mode.getDisplayName());
        addMessage(players[0].getUsername() + " goes first.");

        LOGGER.info("Game setup complete. Mode: {}, Grid: {}x{}",
                mode.getDisplayName(), mode.getGridSize(), mode.getGridSize());
    }

    // --- In Progress Phase ---

    /**
     * Validates and executes an attack.
     *
     * @param playerIndex the index of the attacking player (0 or 1)
     * @param weapon      the weapon to fire
     * @param target      the target coordinate
     * @param direction   direction for directional weapons
     * @return the AttackResult, or null if the attack is invalid
     */
    public AttackResult executeAttack(int playerIndex, WeaponType weapon,
            Coordinate target, Direction direction) {
        // Validate game state
        if (phase != Phase.IN_PROGRESS) {
            LOGGER.warn("Attack attempted but game is not in progress (phase: {})", phase);
            return null;
        }

        if (playerIndex != currentTurnIndex) {
            LOGGER.warn("Player {} tried to attack but it's player {}'s turn",
                    playerIndex, currentTurnIndex);
            return null;
        }

        PlayerState attacker = players[playerIndex];
        int defenderIndex = 1 - playerIndex;
        PlayerState defender = players[defenderIndex];

        // Validate weapon availability
        if (!attacker.getCooldownManager().isWeaponAvailable(weapon, attacker.getBoard())) {
            LOGGER.warn("Player {} tried to use unavailable weapon: {}",
                    attacker.getUsername(), weapon.getDisplayName());
            return null;
        }

        // Validate target coordinates
        if (!target.isWithinBounds(mode.getGridSize())) {
            LOGGER.warn("Player {} targeted out-of-bounds coordinate: {}",
                    attacker.getUsername(), target.toDisplayString());
            return null;
        }

        // Resolve the attack
        AttackResult result = attackResolver.resolve(
                defender.getBoard(), weapon, target, direction);

        // Activate cooldown for the weapon used
        attacker.getCooldownManager().activateCooldown(weapon);

        // Update stats
        int totalTiles = result.getTileResults().size();
        int hits = result.hitCount();
        attacker.recordShots(totalTiles, hits);
        attacker.incrementTurns();

        // Log messages
        String attackMsg = attacker.getUsername() + " fired " + weapon.getDisplayName()
                + " at " + target.toDisplayString();
        addMessage(attackMsg);

        if (hits > 0) {
            addMessage("  → " + hits + " hit(s)!");
        } else {
            addMessage("  → All miss!");
        }

        for (var sunkShip : result.getSunkShips()) {
            addMessage("  → " + defender.getUsername() + "'s "
                    + sunkShip.getType().getDisplayName() + " (" + sunkShip.getId() + ") SUNK!");
        }

        // Check game over
        if (result.isGameOver()) {
            this.phase = Phase.GAME_OVER;
            this.winnerUsername = attacker.getUsername();
            addMessage(attacker.getUsername() + " wins the game!");
            LOGGER.info("Game over! Winner: {}", winnerUsername);
        } else {
            // Switch turns
            advanceTurn();
        }

        return result;
    }

    /**
     * Advances to the next player's turn.
     */
    private void advanceTurn() {
        currentTurnIndex = 1 - currentTurnIndex;
        turnNumber++;
        // Tick cooldowns for the new active player
        players[currentTurnIndex].getCooldownManager().tickCooldowns();
    }

    /**
     * Handles a player forfeiting the game.
     */
    public void forfeit(int playerIndex) {
        if (phase != Phase.IN_PROGRESS)
            return;

        int winnerIndex = 1 - playerIndex;
        this.phase = Phase.GAME_OVER;
        this.winnerUsername = players[winnerIndex].getUsername();
        addMessage(players[playerIndex].getUsername() + " forfeited.");
        addMessage(players[winnerIndex].getUsername() + " wins by forfeit!");
        LOGGER.info("{} forfeited. {} wins.", players[playerIndex].getUsername(), winnerUsername);
    }

    /**
     * Handles a player disconnecting.
     */
    public void playerDisconnected(int playerIndex) {
        if (phase == Phase.IN_PROGRESS) {
            int winnerIndex = 1 - playerIndex;
            this.phase = Phase.GAME_OVER;
            this.winnerUsername = players[winnerIndex].getUsername();
            addMessage(players[playerIndex].getUsername() + " disconnected.");
            addMessage(players[winnerIndex].getUsername() + " wins by disconnect!");
        } else if (phase == Phase.LOBBY) {
            players[playerIndex] = null;
        }
    }

    // --- Message Log ---

    private void addMessage(String message) {
        messageLog.add(message);
        // Keep only last 20 messages
        if (messageLog.size() > 20) {
            messageLog.remove(0);
        }
    }

    /**
     * Returns the last N messages from the game log.
     */
    public List<String> getRecentMessages(int count) {
        int start = Math.max(0, messageLog.size() - count);
        return new ArrayList<>(messageLog.subList(start, messageLog.size()));
    }

    // --- Getters ---

    public Phase getPhase() {
        return phase;
    }

    public GameMode getMode() {
        return mode;
    }

    public PlayerState getPlayer(int index) {
        return players[index];
    }

    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    /**
     * Returns the available weapons for the current player.
     */
    public List<WeaponType> getAvailableWeaponsForCurrentPlayer() {
        PlayerState current = players[currentTurnIndex];
        return current.getCooldownManager().getAvailableWeapons(current.getBoard());
    }
}
