package game.server.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GameLobbyTest {

    private GameLobby lobby;

    @BeforeEach
    void setUp() {
        lobby = new GameLobby();
    }

    @Test
    void testFirstPlayerWaits() {
        GameLobby.JoinResult result = lobby.joinPlayer("ws-1", "Alice");

        assertEquals(0, result.playerIndex());
        assertFalse(result.gameReady());
        assertNotNull(result.session());
    }

    @Test
    void testSecondPlayerJoinsAndGameReady() {
        GameLobby.JoinResult result1 = lobby.joinPlayer("ws-1", "Alice");
        GameLobby.JoinResult result2 = lobby.joinPlayer("ws-2", "Bob");

        assertEquals(1, result2.playerIndex());
        assertTrue(result2.gameReady());
        // Both should be in the same session
        assertSame(result1.session(), result2.session());
    }

    @Test
    void testThirdPlayerCreatesNewSession() {
        lobby.joinPlayer("ws-1", "Alice");
        lobby.joinPlayer("ws-2", "Bob");
        GameLobby.JoinResult result3 = lobby.joinPlayer("ws-3", "Charlie");

        assertEquals(0, result3.playerIndex()); // First in new session
        assertFalse(result3.gameReady());
    }

    @Test
    void testGetSessionForPlayer() {
        lobby.joinPlayer("ws-1", "Alice");
        lobby.joinPlayer("ws-2", "Bob");

        Optional<GameSession> session1 = lobby.getSessionForPlayer("ws-1");
        Optional<GameSession> session2 = lobby.getSessionForPlayer("ws-2");

        assertTrue(session1.isPresent());
        assertTrue(session2.isPresent());
        assertSame(session1.get(), session2.get());

        assertTrue(lobby.getSessionForPlayer("ws-unknown").isEmpty());
    }

    @Test
    void testRemovePlayer_waitingSession() {
        lobby.joinPlayer("ws-1", "Alice");

        Optional<GameSession> removed = lobby.removePlayer("ws-1");
        assertTrue(removed.isPresent());

        // Waiting session should be cleared — new player gets new session
        GameLobby.JoinResult result = lobby.joinPlayer("ws-2", "Bob");
        assertEquals(0, result.playerIndex());
        assertFalse(result.gameReady());
    }

    @Test
    void testRemovePlayer_inProgress() {
        lobby.joinPlayer("ws-1", "Alice");
        GameLobby.JoinResult result2 = lobby.joinPlayer("ws-2", "Bob");

        result2.session().startGame(game.common.model.GameMode.BLITZ);

        Optional<GameSession> removed = lobby.removePlayer("ws-1");
        assertTrue(removed.isPresent());
        assertEquals(GameState.Phase.GAME_OVER, removed.get().getGameState().getPhase());
        assertEquals("Bob", removed.get().getGameState().getWinnerUsername());
    }

    @Test
    void testRemovePlayer_notInLobby() {
        Optional<GameSession> removed = lobby.removePlayer("ws-nonexistent");
        assertTrue(removed.isEmpty());
    }

    @Test
    void testMultipleSessions() {
        lobby.joinPlayer("ws-1", "Alice");
        lobby.joinPlayer("ws-2", "Bob");
        lobby.joinPlayer("ws-3", "Charlie");
        lobby.joinPlayer("ws-4", "Diana");

        Optional<GameSession> session1 = lobby.getSessionForPlayer("ws-1");
        Optional<GameSession> session3 = lobby.getSessionForPlayer("ws-3");

        assertTrue(session1.isPresent());
        assertTrue(session3.isPresent());
        assertNotSame(session1.get(), session3.get());
    }
}
