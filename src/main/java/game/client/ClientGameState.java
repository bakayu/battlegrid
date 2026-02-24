package game.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Client-side cache of the game state received from the server.
 * Stores the latest data for rendering the HUD.
 */
public class ClientGameState {

    public enum ClientPhase {
        CONNECTING,
        HANDSHAKE,
        LOBBY_WAITING,
        MODE_SELECT,
        IN_GAME_YOUR_TURN,
        IN_GAME_WAIT_TURN,
        GAME_OVER,
        PLAY_AGAIN_PROMPT,
        PLAY_AGAIN_WAITING
    }

    private ClientPhase phase = ClientPhase.CONNECTING;
    private String opponentName;
    private String mode;
    private int gridSize;

    // Boards
    private JsonObject yourBoard;
    private JsonObject enemyBoard;

    // Ships & fleet
    private JsonArray yourShips;
    private JsonArray fleetStatus;

    // Turn info
    private int turnNumber;
    private JsonArray weapons;
    private JsonArray messages;

    // Mode selection
    private JsonArray availableModes;

    // Game over
    private JsonObject gameOverData;

    // --- Phase ---

    public ClientPhase getPhase() {
        return phase;
    }

    public void setPhase(ClientPhase phase) {
        this.phase = phase;
    }

    // --- Lobby ---

    public void applyModeSelect(JsonObject payload) {
        this.phase = ClientPhase.MODE_SELECT;
        this.opponentName = payload.get("opponentName").getAsString();
        this.availableModes = payload.getAsJsonArray("modes");
    }

    public JsonArray getAvailableModes() {
        return availableModes;
    }

    // --- Game Start ---

    public void applyGameStart(JsonObject payload) {
        this.mode = payload.get("mode").getAsString();
        this.gridSize = payload.get("gridSize").getAsInt();
        this.opponentName = payload.get("opponentName").getAsString();
        this.yourShips = payload.getAsJsonArray("yourShips");
        this.yourBoard = payload.getAsJsonObject("yourBoard");

        // Initialize empty enemy board
        this.enemyBoard = createEmptyBoard(gridSize);
    }

    // --- Turn Updates ---

    public void applyYourTurn(JsonObject payload) {
        this.phase = ClientPhase.IN_GAME_YOUR_TURN;
        this.turnNumber = payload.get("turnNumber").getAsInt();
        this.weapons = payload.getAsJsonArray("weapons");
        if (payload.has("fleetStatus")) {
            this.fleetStatus = payload.getAsJsonArray("fleetStatus");
        }
        if (payload.has("messages")) {
            this.messages = payload.getAsJsonArray("messages");
        }
    }

    public void applyWaitTurn(JsonObject payload) {
        this.phase = ClientPhase.IN_GAME_WAIT_TURN;
        this.turnNumber = payload.get("turnNumber").getAsInt();
        if (payload.has("fleetStatus")) {
            this.fleetStatus = payload.getAsJsonArray("fleetStatus");
        }
        if (payload.has("messages")) {
            this.messages = payload.getAsJsonArray("messages");
        }
    }

    // --- Attack Results ---

    public void applyAttackResult(JsonObject payload) {
        if (payload.has("enemyBoard")) {
            this.enemyBoard = payload.getAsJsonObject("enemyBoard");
        }
        if (payload.has("fleetStatus")) {
            this.fleetStatus = payload.getAsJsonArray("fleetStatus");
        }
        if (payload.has("messages")) {
            this.messages = payload.getAsJsonArray("messages");
        }
    }

    public void applyIncomingAttack(JsonObject payload) {
        if (payload.has("yourBoard")) {
            this.yourBoard = payload.getAsJsonObject("yourBoard");
        }
        if (payload.has("fleetStatus")) {
            this.fleetStatus = payload.getAsJsonArray("fleetStatus");
        }
        if (payload.has("messages")) {
            this.messages = payload.getAsJsonArray("messages");
        }
    }

    // --- Game Over ---

    public void applyGameOver(JsonObject payload) {
        this.phase = ClientPhase.GAME_OVER;
        this.gameOverData = payload;
        if (payload.has("messages")) {
            this.messages = payload.getAsJsonArray("messages");
        }
    }

    // --- Play Again ---

    public void applyPlayAgainPrompt() {
        this.phase = ClientPhase.PLAY_AGAIN_PROMPT;
    }

    public void applyPlayAgainWaiting() {
        this.phase = ClientPhase.PLAY_AGAIN_WAITING;
    }

    /**
     * Resets client state for a new game (same connection).
     */
    public void resetForNewGame() {
        this.yourBoard = null;
        this.enemyBoard = null;
        this.yourShips = null;
        this.fleetStatus = null;
        this.weapons = null;
        this.messages = null;
        this.gameOverData = null;
        this.turnNumber = 0;
        this.mode = null;
        this.gridSize = 0;
        this.availableModes = null;
    }

    // --- Getters ---

    public String getOpponentName() {
        return opponentName;
    }

    public String getMode() {
        return mode;
    }

    public int getGridSize() {
        return gridSize;
    }

    public JsonObject getYourBoard() {
        return yourBoard;
    }

    public JsonObject getEnemyBoard() {
        return enemyBoard;
    }

    public JsonArray getYourShips() {
        return yourShips;
    }

    public JsonArray getFleetStatus() {
        return fleetStatus;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public JsonArray getWeapons() {
        return weapons;
    }

    public JsonArray getMessages() {
        return messages;
    }

    public JsonObject getGameOverData() {
        return gameOverData;
    }

    // --- Helpers ---

    private JsonObject createEmptyBoard(int size) {
        JsonObject board = new JsonObject();
        board.addProperty("size", size);
        JsonArray grid = new JsonArray();
        for (int r = 0; r < size; r++) {
            JsonArray row = new JsonArray();
            for (int c = 0; c < size; c++) {
                row.add("EMPTY");
            }
            grid.add(row);
        }
        board.add("grid", grid);
        return board;
    }
}
