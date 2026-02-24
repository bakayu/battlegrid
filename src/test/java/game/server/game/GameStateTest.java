package game.server.game;

import game.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState();
    }

    @Test
    void testAddPlayers() {
        assertEquals(0, gameState.addPlayer("Alice"));
        assertFalse(gameState.isFull());
        assertEquals(1, gameState.addPlayer("Bob"));
        assertTrue(gameState.isFull());
    }

    @Test
    void testAddPlayer_full() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        assertEquals(-1, gameState.addPlayer("Charlie"));
    }

    @Test
    void testSetup() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        assertEquals(GameState.Phase.IN_PROGRESS, gameState.getPhase());
        assertEquals(GameMode.BLITZ, gameState.getMode());
        assertEquals(0, gameState.getCurrentTurnIndex());
        assertNotNull(gameState.getPlayer(0).getBoard());
        assertNotNull(gameState.getPlayer(1).getBoard());
        assertEquals(3, gameState.getPlayer(0).getBoard().getShips().size());
        assertEquals(3, gameState.getPlayer(1).getBoard().getShips().size());
    }

    @Test
    void testSetup_failsWithoutPlayers() {
        gameState.addPlayer("Alice");
        assertThrows(IllegalStateException.class, () -> gameState.setup(GameMode.BLITZ));
    }

    @Test
    void testExecuteAttack_wrongTurn() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        // Player 1 tries to attack but it's player 0's turn
        AttackResult result = gameState.executeAttack(
                1, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);
        assertNull(result);
    }

    @Test
    void testExecuteAttack_outOfBounds() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ); // 8x8

        AttackResult result = gameState.executeAttack(
                0, WeaponType.STANDARD_SHOT, new Coordinate(10, 10), Direction.HORIZONTAL);
        assertNull(result);
    }

    @Test
    void testExecuteAttack_turnAlternates() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        assertEquals(0, gameState.getCurrentTurnIndex());

        // Player 0 attacks
        AttackResult result = gameState.executeAttack(
                0, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);
        assertNotNull(result);

        // Should now be player 1's turn
        assertEquals(1, gameState.getCurrentTurnIndex());

        // Player 1 attacks
        result = gameState.executeAttack(
                1, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);
        assertNotNull(result);

        // Back to player 0
        assertEquals(0, gameState.getCurrentTurnIndex());
    }

    @Test
    void testExecuteAttack_cooldownApplied() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.STRIKE); // has all weapon types

        // Player 0 fires line barrage
        AttackResult result = gameState.executeAttack(
                0, WeaponType.LINE_BARRAGE, new Coordinate(3, 3), Direction.HORIZONTAL);

        if (result != null) {
            // Cooldown should be active
            CooldownManager cm = gameState.getPlayer(0).getCooldownManager();
            // After firing, CD = 2. After turn switch and back, it gets ticked.
            // But we need to wait for it to be player 0's turn again to check.
            assertFalse(cm.isReady(WeaponType.LINE_BARRAGE));
        }
    }

    @Test
    void testForfeit() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        gameState.forfeit(0);

        assertEquals(GameState.Phase.GAME_OVER, gameState.getPhase());
        assertEquals("Bob", gameState.getWinnerUsername());
    }

    @Test
    void testPlayerDisconnected_inProgress() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        gameState.playerDisconnected(1);

        assertEquals(GameState.Phase.GAME_OVER, gameState.getPhase());
        assertEquals("Alice", gameState.getWinnerUsername());
    }

    @Test
    void testPlayerDisconnected_inLobby() {
        gameState.addPlayer("Alice");
        gameState.playerDisconnected(0);

        assertFalse(gameState.isFull());
        assertEquals(GameState.Phase.LOBBY, gameState.getPhase());
    }

    @Test
    void testRecentMessages() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        List<String> messages = gameState.getRecentMessages(5);
        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().anyMatch(m -> m.contains("Game started")));
    }

    @Test
    void testGetAvailableWeapons_blitz() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        List<WeaponType> available = gameState.getAvailableWeaponsForCurrentPlayer();
        // Blitz has PB, SUB, DS — so STANDARD_SHOT and LINE_BARRAGE
        assertTrue(available.contains(WeaponType.STANDARD_SHOT));
        assertTrue(available.contains(WeaponType.LINE_BARRAGE));
        // No battleship or carrier in blitz
        assertFalse(available.contains(WeaponType.CROSS_BOMBER));
        assertFalse(available.contains(WeaponType.NUKE));
    }

    @Test
    void testStats_tracking() {
        gameState.addPlayer("Alice");
        gameState.addPlayer("Bob");
        gameState.setup(GameMode.BLITZ);

        // Fire a shot
        gameState.executeAttack(
                0, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);

        PlayerState alice = gameState.getPlayer(0);
        assertEquals(1, alice.getShotsFired());
        assertEquals(1, alice.getTurnsTaken());
    }
}
