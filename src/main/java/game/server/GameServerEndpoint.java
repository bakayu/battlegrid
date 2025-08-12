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

@ServerEndpoint(value = "/battlegrid", decoders = { BoxCodec.class })
public class GameServerEndpoint {

	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
	private static PrivateKey rsaPrivateKey;
	private static PublicKey rsaPublicKey;

	// Static initializer block to generate the server's RSA key pair once.
	static {
		try {
			KeyPair keyPair = CryptoUtils.generateRSAKeyPair();
			rsaPrivateKey = keyPair.getPrivate();
			rsaPublicKey = keyPair.getPublic();
			System.out.println("Server RSA KeyPair generated successfully.");
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate server RSA key pair", e);
		}
	}

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("New connection attempt from Session ID: " + session.getId());
		// Start the handshake by sending the server's public RSA key to the client.
		try {
			JsonObject handshakePayload = new JsonObject();
			handshakePayload.addProperty("type", "handshake_rsa_key");
			handshakePayload.addProperty("publicKey", CryptoUtils.keyToString(rsaPublicKey));

			Box handshakeBox = new Box(handshakePayload);
			// Use a plain text encoder for the initial handshake message
			session.getBasicRemote().sendText(new BoxCodec().encode(handshakeBox));
			System.out.println("Sent RSA public key to " + session.getId());
		} catch (Exception e) {
			System.err.println("Handshake failed for session " + session.getId());
			e.printStackTrace();
			try {
				session.close();
			} catch (IOException ioException) {
				// Ignore
			}
		}
	}

	@OnMessage
	public void onMessage(String rawMessage, Session session) {
		// We check if the session has an AES key. If not, this must be the handshake
		// response.
		if (session.getUserProperties().get("aesKey") == null) {
			handleHandshakeResponse(rawMessage, session);
		} else {
			handleGameMessage(rawMessage, session);
		}
	}

	private void handleHandshakeResponse(String encryptedAesKeyString, Session session) {
		try {
			// Decrypt the AES key sent from the client using our private RSA key.
			byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyString);
			byte[] decryptedAesKeyBytes = CryptoUtils.rsaDecrypt(encryptedAesKey, rsaPrivateKey);
			SecretKey aesKey = CryptoUtils.stringToAesKey(new String(Base64.getEncoder().encode(decryptedAesKeyBytes)));

			// Store the AES key in the session's properties for future use.
			session.getUserProperties().put("aesKey", aesKey);

			// The client also sends its username in this first message
			// For simplicity, we'll assume the username is sent plain text after the key
			// In a real app, this would be part of an encrypted payload.
			// Let's just assign a default name for now.
			int playerNumber = sessions.size() + 1;
			String username = "Player" + playerNumber;
			session.getUserProperties().put("username", username);

			sessions.add(session); // Officially add the session after successful handshake
			System.out.println("Handshake complete for " + session.getId() + ". User: " + username);

			// Notify all players that a new user has joined.
			JsonObject joinPayload = new JsonObject();
			joinPayload.addProperty("type", "player_joined");
			joinPayload.addProperty("message", username + " has joined the battlefield.");
			joinPayload.addProperty("username", username);
			broadcast(new Box(joinPayload));

		} catch (Exception e) {
			System.err.println("AES key exchange failed for session " + session.getId());
			e.printStackTrace();
		}
	}

	private void handleGameMessage(String encryptedMessage, Session session) {
		try {
			SecretKey aesKey = (SecretKey) session.getUserProperties().get("aesKey");
			byte[] decryptedBytes = CryptoUtils.aesDecrypt(Base64.getDecoder().decode(encryptedMessage), aesKey);
			String decryptedJson = new String(decryptedBytes);

			Box incomingBox = new BoxCodec().decode(decryptedJson);
			JsonObject payload = incomingBox.getPayload();
			String username = (String) session.getUserProperties().get("username");

			System.out.println("Message from " + username + ": " + payload);
			payload.addProperty("sender", username); // Add sender info

			broadcast(new Box(payload));
		} catch (Exception e) {
			System.err.println("Failed to process game message for session " + session.getId());
			e.printStackTrace();
		}
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session);
		String username = (String) session.getUserProperties().getOrDefault("username", "A player");
		System.out.println("Connection closed for: " + username);

		JsonObject leavePayload = new JsonObject();
		leavePayload.addProperty("type", "player_left");
		leavePayload.addProperty("message", username + " has left the battlefield.");
		leavePayload.addProperty("username", username);
		broadcast(new Box(leavePayload));
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		System.err.println("Error for session " + session.getId() + ": " + throwable.getMessage());
		sessions.remove(session);
	}

	private void broadcast(Box box) {
		sessions.forEach(session -> {
			try {
				// Encrypt the message with each client's unique AES key before sending
				SecretKey aesKey = (SecretKey) session.getUserProperties().get("aesKey");
				if (aesKey != null) {
					String jsonPayload = new BoxCodec().encode(box);
					byte[] encryptedPayload = CryptoUtils.aesEncrypt(jsonPayload.getBytes(), aesKey);
					session.getBasicRemote().sendText(Base64.getEncoder().encodeToString(encryptedPayload));
				}
			} catch (Exception e) {
				System.err.println("Failed to broadcast to session " + session.getId());
			}
		});
	}
}