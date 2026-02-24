package game.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientGameStateTest {

    private ClientGameState state;

    @BeforeEach
    void setUp() {
        state = new ClientGameState();
    }

    @Test
    void testInitialPhase() {
        assertEquals(ClientGameState.ClientPhase.CONNECTING, state.getPhase());
    }

    @Test
    void testApplyModeSelect() {
        JsonObject payload = new JsonObject();
        payload.addProperty("opponentName", "Bob");
        JsonArray modes = new JsonArray();
        JsonObject mode = new JsonObject();
        mode.addProperty("name", "BLITZ");
        mode.addProperty("displayName", "Blitz");
        mode.addProperty("gridSize", 8);
        mode.addProperty("shipCount", 3);
        modes.add(mode);
        payload.add("modes", modes);

        state.applyModeSelect(payload);

        assertEquals(ClientGameState.ClientPhase.MODE_SELECT, state.getPhase());
        assertEquals("Bob", state.getOpponentName());
        assertEquals(1, state.getAvailableModes().size());
    }

    @Test
    void testApplyGameStart() {
        JsonObject payload = new JsonObject();
        payload.addProperty("mode", "BLITZ");
        payload.addProperty("gridSize", 8);
        payload.addProperty("opponentName", "Bob");
        payload.add("yourShips", new JsonArray());

        JsonObject board = createBoard(8);
        payload.add("yourBoard", board);

        state.applyGameStart(payload);

        assertEquals("BLITZ", state.getMode());
        assertEquals(8, state.getGridSize());
        assertEquals("Bob", state.getOpponentName());
        assertNotNull(state.getYourBoard());
        assertNotNull(state.getEnemyBoard());
    }

    @Test
    void testApplyYourTurn() {
        JsonObject payload = new JsonObject();
        payload.addProperty("turnNumber", 5);
        payload.add("weapons", new JsonArray());
        payload.add("fleetStatus", new JsonArray());
        payload.add("messages", new JsonArray());

        state.applyYourTurn(payload);

        assertEquals(ClientGameState.ClientPhase.IN_GAME_YOUR_TURN, state.getPhase());
        assertEquals(5, state.getTurnNumber());
    }

    @Test
    void testApplyWaitTurn() {
        JsonObject payload = new JsonObject();
        payload.addProperty("turnNumber", 6);
        payload.add("fleetStatus", new JsonArray());
        payload.add("messages", new JsonArray());

        state.applyWaitTurn(payload);

        assertEquals(ClientGameState.ClientPhase.IN_GAME_WAIT_TURN, state.getPhase());
        assertEquals(6, state.getTurnNumber());
    }

    @Test
    void testApplyAttackResult() {
        // First set up game
        JsonObject startPayload = new JsonObject();
        startPayload.addProperty("mode", "BLITZ");
        startPayload.addProperty("gridSize", 8);
        startPayload.addProperty("opponentName", "Bob");
        startPayload.add("yourShips", new JsonArray());
        startPayload.add("yourBoard", createBoard(8));
        state.applyGameStart(startPayload);

        // Apply attack result with updated enemy board
        JsonObject payload = new JsonObject();
        payload.add("enemyBoard", createBoard(8));
        payload.add("fleetStatus", new JsonArray());
        payload.add("messages", new JsonArray());

        state.applyAttackResult(payload);

        assertNotNull(state.getEnemyBoard());
        assertNotNull(state.getFleetStatus());
    }

    @Test
    void testApplyIncomingAttack() {
        JsonObject payload = new JsonObject();
        payload.add("yourBoard", createBoard(8));
        payload.add("fleetStatus", new JsonArray());
        payload.add("messages", new JsonArray());

        state.applyIncomingAttack(payload);

        assertNotNull(state.getYourBoard());
    }

    @Test
    void testApplyGameOver() {
        JsonObject payload = new JsonObject();
        payload.addProperty("winner", "Alice");
        payload.addProperty("youWon", true);
        payload.add("yourStats", createStats());
        payload.add("opponentStats", createStats());
        payload.add("messages", new JsonArray());

        state.applyGameOver(payload);

        assertEquals(ClientGameState.ClientPhase.GAME_OVER, state.getPhase());
        assertNotNull(state.getGameOverData());
    }

    private JsonObject createBoard(int size) {
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

    private JsonObject createStats() {
        JsonObject stats = new JsonObject();
        stats.addProperty("turnsTaken", 10);
        stats.addProperty("shotsFired", 15);
        stats.addProperty("shotsHit", 8);
        stats.addProperty("hitRate", "53.3%");
        stats.addProperty("shipsLost", 2);
        stats.addProperty("totalShips", 5);
        return stats;
    }
}
