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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@ServerEndpoint(value = "/battlegrid", decoders = { BoxCodec.class })
public class GameServerEndpoint {

	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
	private static PrivateKey rsaPrivateKey;
	private static PublicKey rsaPublicKey;

	private static final String PROPERTY_AES_KEY = "aesKey";
	private static final String PROPERTY_USERNAME = "username";

	private static final Logger LOGGER = LoggerFactory.getLogger(GameServerEndpoint.class);

	// Static initializer block to generate the server's RSA key pair.
	static {
		try {
			KeyPair keyPair = CryptoUtils.generateRSAKeyPair();
			rsaPrivateKey = keyPair.getPrivate();
			rsaPublicKey = keyPair.getPublic();
			LOGGER.info("Server RSA KeyPair generated successfully.");
		} catch (Exception e) {
			LOGGER.error("Failed to generate server RSA key pair", e);
			throw new RuntimeException("Failed to generate server RSA key pair", e);
		}
	}

	/**
	 * Called when a new WebSocket connection is opened. Initiates handshake by
	 * sending
	 * the server's public RSA key to the client.
	 * 
	 * @param session The WebSocket session for the connecting client.
	 */
	@OnOpen
	public void onOpen(Session session) {
		LOGGER.info("New connection attempt from Session ID: {}", session.getId());
		// Start the handshake by sending the server's public RSA key to the client.
		try {
			JsonObject handshakePayload = new JsonObject();
			handshakePayload.addProperty("type", "handshake_rsa_key");
			handshakePayload.addProperty("publicKey", CryptoUtils.keyToString(rsaPublicKey));

			Box handshakeBox = new Box(handshakePayload);
			// Use a plain text encoder for the initial handshake message
			session.getBasicRemote().sendText(new BoxCodec().encode(handshakeBox));
			LOGGER.info("Sent RSA public key to session {}", session.getId());
		} catch (Exception e) {
			LOGGER.error("Handshake failed for session {}", session.getId(), e);
			try {
				session.close();
			} catch (IOException ioException) {
				LOGGER.warn("Failed to close session {}", session.getId(), ioException);
			}
		}
	}

	/**
	 * Called when a message is received from a client. Handles handshake response
	 * if AES key is not set, otherwise processes game messages.
	 * 
	 * @param rawMessage The raw message received from the client.
	 * @param session    The WebSocket session for the client.
	 */
	@OnMessage
	public void onMessage(String rawMessage, Session session) {
		// Check if the session has an AES key. If not, this must be the handshake
		// response.
		if (session.getUserProperties().get(PROPERTY_AES_KEY) == null) {
			handleHandshakeResponse(rawMessage, session);
		} else {
			handleGameMessage(rawMessage, session);
		}
	}

	/**
	 * Handles the handshake response from a client, decrypts the AES key, assigns a
	 * username, and notifies all players of the new user.
	 * 
	 * @param encryptedAesKeyString The AES key encrypted with the server's public
	 *                              RSA key.
	 * @param session               The WebSocket session for the client.
	 */
	private void handleHandshakeResponse(String encryptedAesKeyString, Session session) {
		try {
			// Decrypt the AES key sent from the client using our private RSA key.
			byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyString);
			byte[] decryptedAesKeyBytes = CryptoUtils.rsaDecrypt(encryptedAesKey, rsaPrivateKey);
			SecretKey aesKey = CryptoUtils.stringToAesKey(new String(Base64.getEncoder().encode(decryptedAesKeyBytes)));

			// Store the AES key in the session's properties for future use.
			session.getUserProperties().put(PROPERTY_AES_KEY, aesKey);

			// The client also sends its username in this first message
			// For simplicity, we'll assume the username is sent plain text after the key
			// In a real app, this would be part of an encrypted payload.
			// Let's just assign a default name for now.
			// TODO: Add feature to allow players to set their own usernames.
			int playerNumber = sessions.size() + 1;
			String username = "Player" + playerNumber;
			session.getUserProperties().put(PROPERTY_USERNAME, username);

			sessions.add(session); // Officially add the session after successful handshake
			LOGGER.info("Handshake complete for session {}. User: {}", session.getId(), username);

			// Notify all players that a new user has joined.
			JsonObject joinPayload = new JsonObject();
			joinPayload.addProperty("type", "player_joined");
			joinPayload.addProperty("message", username + " has joined the battlegird.");
			joinPayload.addProperty(PROPERTY_USERNAME, username);
			broadcast(new Box(joinPayload));

		} catch (Exception e) {
			LOGGER.error("AES key exchange failed for session {}", session.getId(), e);
		}
	}

	/**
	 * Processes an incoming encrypted game message from a client, decrypts it,
	 * adds sender info, and broadcasts it to all players.
	 * 
	 * @param encryptedMessage The encrypted message from the client.
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

			LOGGER.info("Message from {}: {}", username, payload);
			payload.addProperty("sender", username); // Add sender info

			broadcast(new Box(payload));
		} catch (Exception e) {
			LOGGER.error("Failed to process game message for session {}", session.getId(), e);
		}
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session);
		String username = (String) session.getUserProperties().getOrDefault(PROPERTY_USERNAME, "A player");
		LOGGER.info("Connection closed for: {}", username);

		JsonObject leavePayload = new JsonObject();
		leavePayload.addProperty("type", "player_left");
		leavePayload.addProperty("message", username + " has left the battlegird.");
		leavePayload.addProperty(PROPERTY_USERNAME, username);
		broadcast(new Box(leavePayload));
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		LOGGER.error("Error for session {}: {}", session.getId(), throwable.getMessage(), throwable);
		sessions.remove(session);
	}

	/**
	 * Broadcasts a message box to all connected sessions, encrypting the payload
	 * with each client's unique AES key.
	 * 
	 * @param box The message box to broadcast.
	 */
	private void broadcast(Box box) {
		sessions.forEach(session -> {
			try {
				// Encrypt the message with each client's unique AES key before sending
				SecretKey aesKey = (SecretKey) session.getUserProperties().get(PROPERTY_AES_KEY);
				if (aesKey != null) {
					String jsonPayload = new BoxCodec().encode(box);
					byte[] encryptedPayload = CryptoUtils.aesEncrypt(jsonPayload.getBytes(), aesKey);
					session.getBasicRemote().sendText(Base64.getEncoder().encodeToString(encryptedPayload));
				}
			} catch (Exception e) {
				LOGGER.error("Failed to broadcast to session {}", session.getId(), e);
			}
		});
	}
}