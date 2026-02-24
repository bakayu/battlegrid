package game.server.game;

import com.google.gson.JsonObject;
import game.common.Constants;
import game.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession("test-001");
    }

    @Test
    void testAddPlayers() {
        assertEquals(0, session.addPlayer("ws-1", "Alice"));
        assertFalse(session.isFull());
        assertEquals(1, session.addPlayer("ws-2", "Bob"));
        assertTrue(session.isFull());
    }

    @Test
    void testAddPlayer_full() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        assertEquals(-1, session.addPlayer("ws-3", "Charlie"));
    }

    @Test
    void testGetPlayerIndex() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");

        assertEquals(0, session.getPlayerIndex("ws-1"));
        assertEquals(1, session.getPlayerIndex("ws-2"));
        assertEquals(-1, session.getPlayerIndex("ws-unknown"));
    }

    @Test
    void testModeVoting_same() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");

        assertNull(session.voteMode(0, GameMode.STRIKE));
        GameMode resolved = session.voteMode(1, GameMode.STRIKE);
        assertEquals(GameMode.STRIKE, resolved);
    }

    @Test
    void testModeVoting_different() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");

        assertNull(session.voteMode(0, GameMode.BLITZ));
        GameMode resolved = session.voteMode(1, GameMode.WAR);

        // Should be one of the two
        assertNotNull(resolved);
        assertTrue(resolved == GameMode.BLITZ || resolved == GameMode.WAR);
    }

    @Test
    void testStartGame() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        assertEquals(GameState.Phase.IN_PROGRESS, session.getGameState().getPhase());
    }

    @Test
    void testBuildLobbyWaitingMessage() {
        JsonObject msg = session.buildLobbyWaitingMessage();
        assertEquals(Constants.MSG_LOBBY_WAITING, msg.get("type").getAsString());
        assertTrue(msg.has("message"));
    }

    @Test
    void testBuildModeSelectMessage() {
        JsonObject msg = session.buildModeSelectMessage("Bob");
        assertEquals(Constants.MSG_LOBBY_MODE_SELECT, msg.get("type").getAsString());
        assertEquals("Bob", msg.get("opponentName").getAsString());
        assertEquals(3, msg.getAsJsonArray("modes").size()); // BLITZ, STRIKE, WAR
    }

    @Test
    void testBuildGameStartMessage() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        JsonObject msg = session.buildGameStartMessage(0);
        assertEquals(Constants.MSG_GAME_START, msg.get("type").getAsString());
        assertEquals("BLITZ", msg.get("mode").getAsString());
        assertEquals(8, msg.get("gridSize").getAsInt());
        assertEquals("Bob", msg.get("opponentName").getAsString());
        assertTrue(msg.has("yourShips"));
        assertTrue(msg.has("yourBoard"));
    }

    @Test
    void testBuildYourTurnMessage() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        JsonObject msg = session.buildYourTurnMessage(0);
        assertEquals(Constants.MSG_YOUR_TURN, msg.get("type").getAsString());
        assertTrue(msg.has("weapons"));
        assertTrue(msg.has("fleetStatus"));
        assertTrue(msg.has("messages"));
    }

    @Test
    void testBuildWaitTurnMessage() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        JsonObject msg = session.buildWaitTurnMessage(1);
        assertEquals(Constants.MSG_WAIT_TURN, msg.get("type").getAsString());
        assertTrue(msg.get("message").getAsString().contains("Alice"));
    }

    @Test
    void testBuildAttackResultMessage() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        AttackResult result = session.processAttack(
                0, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);

        assertNotNull(result);

        JsonObject msg = session.buildAttackResultMessage(result, 0);
        assertEquals(Constants.MSG_ATTACK_RESULT, msg.get("type").getAsString());
        assertTrue(msg.has("tileResults"));
        assertTrue(msg.has("enemyBoard"));
        assertTrue(msg.has("fleetStatus"));
    }

    @Test
    void testBuildIncomingAttackMessage() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        AttackResult result = session.processAttack(
                0, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);

        assertNotNull(result);

        JsonObject msg = session.buildIncomingAttackMessage(result, 1);
        assertEquals(Constants.MSG_INCOMING_ATTACK, msg.get("type").getAsString());
        assertEquals("Alice", msg.get("attackerName").getAsString());
        assertTrue(msg.has("yourBoard"));
    }

    @Test
    void testBuildGameOverMessage() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        session.getGameState().forfeit(1);

        JsonObject msgWinner = session.buildGameOverMessage(0);
        assertEquals(Constants.MSG_GAME_OVER, msgWinner.get("type").getAsString());
        assertTrue(msgWinner.get("youWon").getAsBoolean());
        assertTrue(msgWinner.has("yourStats"));
        assertTrue(msgWinner.has("opponentStats"));

        JsonObject msgLoser = session.buildGameOverMessage(1);
        assertFalse(msgLoser.get("youWon").getAsBoolean());
    }

    @Test
    void testBuildErrorMessage() {
        JsonObject msg = GameSession.buildErrorMessage("Oops");
        assertEquals(Constants.MSG_ERROR, msg.get("type").getAsString());
        assertEquals("Oops", msg.get("message").getAsString());
    }

    @Test
    void testEnemyBoardFogOfWar() {
        session.addPlayer("ws-1", "Alice");
        session.addPlayer("ws-2", "Bob");
        session.startGame(GameMode.BLITZ);

        AttackResult result = session.processAttack(
                0, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);
        assertNotNull(result);

        JsonObject msg = session.buildAttackResultMessage(result, 0);
        JsonObject enemyBoard = msg.getAsJsonObject("enemyBoard");

        // Check that enemy board doesn't reveal SHIP cells
        var grid = enemyBoard.getAsJsonArray("grid");
        for (int r = 0; r < grid.size(); r++) {
            var row = grid.get(r).getAsJsonArray();
            for (int c = 0; c < row.size(); c++) {
                String cellState = row.get(c).getAsString();
                assertNotEquals("SHIP", cellState,
                        "Enemy board should not reveal ship positions at (" + r + "," + c + ")");
            }
        }
    }

    @Test
    void testPlayAgainVoting_bothYes() {
        GameSession session = createFullSession();
        session.startGame(GameMode.BLITZ);

        assertNull(session.votePlayAgain(0, true));
        Boolean result = session.votePlayAgain(1, true);
        assertTrue(result);
    }

    @Test
    void testPlayAgainVoting_oneNo() {
        GameSession session = createFullSession();
        session.startGame(GameMode.BLITZ);

        assertNull(session.votePlayAgain(0, true));
        Boolean result = session.votePlayAgain(1, false);
        assertFalse(result);
    }

    @Test
    void testPlayAgainVoting_waitingForSecond() {
        GameSession session = createFullSession();
        session.startGame(GameMode.BLITZ);

        assertNull(session.votePlayAgain(0, true));
    }

    @Test
    void testResetForNewGame() {
        GameSession session = createFullSession();
        session.startGame(GameMode.BLITZ);

        session.resetForNewGame();

        assertEquals(GameState.Phase.LOBBY, session.getGameState().getPhase());
        assertTrue(session.getGameState().isFull());
    }

    @Test
    void testBuildPlayAgainPrompt() {
        GameSession session = createFullSession();
        JsonObject msg = session.buildPlayAgainPromptMessage();
        assertEquals(Constants.MSG_PLAY_AGAIN_PROMPT, msg.get("type").getAsString());
    }

    @Test
    void testTurnTimeout() throws InterruptedException {
        GameSession session = createFullSession();
        session.startGame(GameMode.BLITZ);

        boolean[] timeoutFired = { false };
        session.setOnTurnTimeout(() -> timeoutFired[0] = true);
        // We won't actually wait 60 seconds — just test start/cancel
        session.startTurnTimeout();
        session.cancelTurnTimeout();
        assertFalse(timeoutFired[0]);
    }

    private GameSession createFullSession() {
        GameSession session = new GameSession("test-session");
        session.addPlayer("ws-0", "Alice");
        session.addPlayer("ws-1", "Bob");
        return session;
    }
}
