package game.server;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import com.google.gson.JsonObject;

import game.common.Box;
import game.common.BoxCodec;
import game.common.CryptoUtils;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the WebSocket server endpoint for the battlefield game.
 * It handles lifecycle events of a WebSocket connection.
 */
@ServerEndpoint(value = "/battlegrid", decoders = { BoxCodec.class })
public class GameServerEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(GameServerEndpoint.class);
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static PrivateKey rsaPrivateKey;
    private static PublicKey rsaPublicKey;

    private static final String PROPERTY_AES_KEY = "aesKey";
    private static final String PROPERTY_USERNAME = "username";

    static {
        try {
            KeyPair keyPair = CryptoUtils.generateRSAKeyPair();
            rsaPrivateKey = keyPair.getPrivate();
            rsaPublicKey = keyPair.getPublic();
            logger.info("Server RSA KeyPair generated successfully.");
        } catch (Exception e) {
            logger.error("Failed to generate server RSA key pair", e);
            throw new RuntimeException("Failed to generate server RSA key pair", e);
        }
    }

    /**
     * Called when a new WebSocket connection is opened. Initiates handshake by sending
     * the server's public RSA key to the client.
     * @param session The WebSocket session for the connecting client.
     */
    @OnOpen
    public void onOpen(Session session) {
        logger.info("New connection attempt from Session ID: {}", session.getId());
        try {
            JsonObject handshakePayload = new JsonObject();
            handshakePayload.addProperty("type", "handshake_rsa_key");
            handshakePayload.addProperty("publicKey", CryptoUtils.keyToString(rsaPublicKey));

            Box handshakeBox = new Box(handshakePayload);
            session.getBasicRemote().sendText(new BoxCodec().encode(handshakeBox));
            logger.info("Sent RSA public key to {}", session.getId());
        } catch (Exception e) {
            logger.error("Handshake failed for session {}", session.getId(), e);
            try {
                session.close();
            } catch (IOException ioException) {
                // Ignore
            }
        }
    }

    /**
     * Called when a message is received from a client. Handles handshake response if
     * AES key is not set, otherwise processes game messages.
     * * @param rawMessage The raw message received from the client.
     * @param session The WebSocket session for the client.
     */
    @OnMessage
    public void onMessage(String rawMessage, Session session) {
        if (session.getUserProperties().get(PROPERTY_AES_KEY) == null) {
            handleHandshakeResponse(rawMessage, session);
        } else {
            handleGameMessage(rawMessage, session);
        }
    }

    /**
     * Handles the handshake response from a client, decrypts the AES key, assigns a
     * username, and notifies all players of the new user.
     * * @param encryptedAesKeyString The AES key encrypted with the server's public
     * RSA key.
     * @param session               The WebSocket session for the client.
     */
    private void handleHandshakeResponse(String encryptedAesKeyString, Session session) {
        try {
            byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyString);
            byte[] decryptedAesKeyBytes = CryptoUtils.rsaDecrypt(encryptedAesKey, rsaPrivateKey);
            SecretKey aesKey = CryptoUtils.stringToAesKey(new String(Base64.getEncoder().encode(decryptedAesKeyBytes)));

            session.getUserProperties().put(PROPERTY_AES_KEY, aesKey);

            int playerNumber = sessions.size() + 1;
            String username = "Player" + playerNumber;
            session.getUserProperties().put(PROPERTY_USERNAME, username);

            sessions.add(session);
            logger.info("Handshake complete for {}. User: {}", session.getId(), username);

            JsonObject joinPayload = new JsonObject();
            joinPayload.addProperty("type", "player_joined");
            joinPayload.addProperty("message", username + " has joined the battlefield.");
            joinPayload.addProperty(PROPERTY_USERNAME, username);
            broadcast(new Box(joinPayload));

        } catch (Exception e) {
            logger.error("AES key exchange failed for session {}", session.getId(), e);
        }
    }

    /**
     * Processes an incoming encrypted game message from a client, decrypts it,
     * adds sender info, and broadcasts it to all players.
     * * @param encryptedMessage The encrypted message from the client.
     * @param session          The WebSocket session for the client.
     */
    private void handleGameMessage(String encryptedMessage, Session session) {
        try {
            SecretKey aesKey = (SecretKey) session.getUserProperties().get(PROPERTY_AES_KEY);
            byte[] decryptedBytes = CryptoUtils.aesDecrypt(Base64.getDecoder().decode(encryptedMessage), aesKey);
            String decryptedJson = new String(decryptedBytes);

            Box incomingBox = new BoxCodec().decode(decryptedJson);
            JsonObject payload = incomingBox.getPayload();
            String username = (String) session.getUserProperties().get(PROPERTY_USERNAME);

            logger.info("Message from {}: {}", username, payload);
            payload.addProperty("sender", username);

            broadcast(new Box(payload));
        } catch (Exception e) {
            logger.error("Failed to process game message for session {}", session.getId(), e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        String username = (String) session.getUserProperties().getOrDefault(PROPERTY_USERNAME, "A player");
        logger.info("Connection closed for: {}", username);

        JsonObject leavePayload = new JsonObject();
        leavePayload.addProperty("type", "player_left");
        leavePayload.addProperty("message", username + " has left the battlefield.");
        leavePayload.addProperty(PROPERTY_USERNAME, username);
        broadcast(new Box(leavePayload));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Error for session {}: {}", session.getId(), throwable.getMessage(), throwable);
        sessions.remove(session);
    }

    /**
     * Broadcasts a message box to all connected sessions, encrypting the payload
     * with each client's unique AES key.
     * * @param box The message box to broadcast.
     */
    private void broadcast(Box box) {
        sessions.forEach(session -> {
            try {
                SecretKey aesKey = (SecretKey) session.getUserProperties().get(PROPERTY_AES_KEY);
                if (aesKey != null) {
                    String jsonPayload = new BoxCodec().encode(box);
                    byte[] encryptedPayload = CryptoUtils.aesEncrypt(jsonPayload.getBytes(), aesKey);
                    session.getBasicRemote().sendText(Base64.getEncoder().encodeToString(encryptedPayload));
                }
            } catch (Exception e) {
                logger.error("Failed to broadcast to session {}", session.getId(), e);
            }
        });
    }
}
