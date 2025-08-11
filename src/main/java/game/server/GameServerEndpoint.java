package game.server;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the WebSocket server endpoint for the battlefield game.
 * It handles lifecycle events of a WebSocket connection.
 */
@ServerEndpoint(value = "/battlegrid")
public class GameServerEndpoint {

    // A thread-safe set to store all active client sessions.
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    /**
     * Called when a new client connects to the server.
     *
     * @param session The session for the new connection.
     */
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("New connection! Session ID: " + session.getId());
        broadcast("Player " + session.getId() + " has joined the battlefield.");
    }

    /**
     * Called when a message is received from a client.
     *
     * @param message The message received.
     * @param session The session that sent the message.
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Message from " + session.getId() + ": " + message);
        // For a real game, you would parse this message to handle game actions.
        // For now, we'll just broadcast it as a chat message.
        broadcast("Player " + session.getId() + ": " + message);
    }

    /**
     * Called when a client connection is closed.
     *
     * @param session The session that was closed.
     */
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("Connection closed. Session ID: " + session.getId());
        broadcast("Player " + session.getId() + " has left the battlefield.");
    }

    /**
     * Called when an error occurs.
     *
     * @param session   The session where the error occurred.
     * @param throwable The throwable error object.
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error for session " + session.getId() + ": " + throwable.getMessage());
        // You might want to remove the session on error as well.
        sessions.remove(session);
    }

    /**
     * Helper method to send a message to all connected clients.
     *
     * @param message The message to broadcast.
     */
    private void broadcast(String message) {
        sessions.forEach(session -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                System.err.println("Failed to send message to session " + session.getId());
                // Consider removing the session if sending fails.
            }
        });
    }
}
