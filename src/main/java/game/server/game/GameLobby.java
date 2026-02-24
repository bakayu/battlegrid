package game.server.game;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages game sessions and player matchmaking.
 * Maintains a waiting queue — the first player waits, the second joins and
 * starts a session.
 */
public class GameLobby {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameLobby.class);

    /** Active game sessions, keyed by session ID */
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    /** Maps WebSocket session ID → GameSession */
    private final Map<String, GameSession> playerSessionMap = new ConcurrentHashMap<>();

    /** The session currently waiting for a second player (null if none) */
    private volatile GameSession waitingSession;

    /**
     * Attempts to place a player into a game.
     * If a session is waiting for an opponent, the player joins it.
     * Otherwise, a new session is created and the player waits.
     *
     * @param wsSessionId the WebSocket session ID
     * @param username    the player's username
     * @return a JoinResult indicating what happened
     */
    public synchronized JoinResult joinPlayer(String wsSessionId, String username) {
        if (waitingSession != null) {
            // Join the existing waiting session
            GameSession session = waitingSession;
            int playerIndex = session.addPlayer(wsSessionId, username);

            if (playerIndex < 0) {
                // Shouldn't happen, but handle gracefully
                LOGGER.error("Failed to join waiting session {}", session.getSessionId());
                return createNewWaitingSession(wsSessionId, username);
            }

            playerSessionMap.put(wsSessionId, session);
            waitingSession = null; // Session is now full

            LOGGER.info("Player {} ({}) joined session {} as player {}",
                    username, wsSessionId, session.getSessionId(), playerIndex);

            return new JoinResult(session, playerIndex, true);
        } else {
            // Create a new session and wait
            return createNewWaitingSession(wsSessionId, username);
        }
    }

    private JoinResult createNewWaitingSession(String wsSessionId, String username) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(sessionId);
        int playerIndex = session.addPlayer(wsSessionId, username);

        sessions.put(sessionId, session);
        playerSessionMap.put(wsSessionId, session);
        waitingSession = session;

        LOGGER.info("Player {} ({}) created session {} and is waiting",
                username, wsSessionId, sessionId);

        return new JoinResult(session, playerIndex, false);
    }

    /**
     * Finds the game session for a given WebSocket session ID.
     */
    public Optional<GameSession> getSessionForPlayer(String wsSessionId) {
        return Optional.ofNullable(playerSessionMap.get(wsSessionId));
    }

    /**
     * Removes a player from their session (on disconnect).
     * Returns the session they were in, if any.
     */
    public synchronized Optional<GameSession> removePlayer(String wsSessionId) {
        GameSession session = playerSessionMap.remove(wsSessionId);
        if (session == null) {
            return Optional.empty();
        }

        int playerIndex = session.getPlayerIndex(wsSessionId);
        if (playerIndex >= 0) {
            session.getGameState().playerDisconnected(playerIndex);
            session.cancelTurnTimeout();
        }

        // If this was the waiting session and it's now empty, clean it up
        if (session == waitingSession) {
            waitingSession = null;
            sessions.remove(session.getSessionId());
            LOGGER.info("Waiting session {} cleaned up after player disconnect",
                    session.getSessionId());
        }

        return Optional.of(session);
    }

    /**
     * Completely removes a finished session and unmaps both players.
     */
    public synchronized void cleanupSession(GameSession session) {
        session.cancelTurnTimeout();
        for (int i = 0; i < 2; i++) {
            String key = session.getSessionKey(i);
            if (key != null) {
                playerSessionMap.remove(key);
            }
        }
        sessions.remove(session.getSessionId());
        if (waitingSession == session) {
            waitingSession = null;
        }
        LOGGER.info("Session {} fully cleaned up", session.getSessionId());
    }

    /**
     * Re-queues a player into the lobby (for play again when opponent declined).
     */
    public JoinResult requeuePlayer(String wsSessionId, String username) {
        // Make sure old mapping is cleared
        playerSessionMap.remove(wsSessionId);
        return joinPlayer(wsSessionId, username);
    }

    /**
     * Returns the number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Result of attempting to join the lobby.
     */
    public record JoinResult(GameSession session, int playerIndex, boolean gameReady) {
    }
}
