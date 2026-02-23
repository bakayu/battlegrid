package game.server;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import game.common.Box;
import game.common.BoxCodec;
import game.common.Constants;
import game.common.CryptoUtils;
import game.common.model.Coordinate;
import game.common.model.Direction;
import game.common.model.GameMode;
import game.common.model.WeaponType;
import game.server.game.AttackResult;
import game.server.game.GameLobby;
import game.server.game.GameSession;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/battlegrid")
public class GameServerEndpoint {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameServerEndpoint.class);

	// Shared across all endpoint instances
	private static final GameLobby LOBBY = new GameLobby();
	private static final Map<String, SecretKey> SESSION_AES_KEYS = new ConcurrentHashMap<>();
	private static final Map<String, Session> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
	private static final Map<String, Boolean> HANDSHAKE_COMPLETE = new ConcurrentHashMap<>();
	private static final Map<String, String> SESSION_USERNAMES = new ConcurrentHashMap<>();

	// RSA key pair for handshake (one per server instance)
	private static final KeyPair RSA_KEY_PAIR;

	static {
		try {
			RSA_KEY_PAIR = CryptoUtils.generateRSAKeyPair();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate RSA key pair", e);
		}
	}

	@OnOpen
	public void onOpen(Session session) {
		String sessionId = session.getId();
		ACTIVE_SESSIONS.put(sessionId, session);
		HANDSHAKE_COMPLETE.put(sessionId, false);
		LOGGER.info("New connection: {}", sessionId);

		// Send RSA public key for handshake
		try {
			JsonObject payload = new JsonObject();
			payload.addProperty("type", Constants.MSG_HANDSHAKE_RSA_KEY);
			payload.addProperty("publicKey",
					CryptoUtils.publicKeyToString(RSA_KEY_PAIR.getPublic()));

			String message = new BoxCodec().encode(new Box(payload));
			session.getBasicRemote().sendText(message);
			LOGGER.info("Sent RSA public key to {}", sessionId);
		} catch (Exception e) {
			LOGGER.error("Failed to send handshake to {}", sessionId, e);
		}
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		String sessionId = session.getId();

		try {
			if (!Boolean.TRUE.equals(HANDSHAKE_COMPLETE.get(sessionId))) {
				handleHandshake(message, session);
			} else {
				handleGameMessage(message, session);
			}
		} catch (Exception e) {
			LOGGER.error("Error processing message from {}", sessionId, e);
			sendPlainJson(session, GameSession.buildErrorMessage("Internal server error."));
		}
	}

	@OnClose
	public void onClose(Session session) {
		String sessionId = session.getId();
		LOGGER.info("Connection closed: {}", sessionId);

		LOBBY.getSessionForPlayer(sessionId).ifPresent(gameSession -> {
			int playerIndex = gameSession.getPlayerIndex(sessionId);
			int opponentIndex = 1 - playerIndex;
			String opponentSessionKey = gameSession.getSessionKey(opponentIndex);

			LOBBY.removePlayer(sessionId);

			// Notify opponent
			if (opponentSessionKey != null) {
				Session opponentWsSession = ACTIVE_SESSIONS.get(opponentSessionKey);
				if (opponentWsSession != null && opponentWsSession.isOpen()) {
					String username = SESSION_USERNAMES.getOrDefault(sessionId, "Opponent");
					sendEncrypted(opponentWsSession, opponentSessionKey,
							GameSession.buildOpponentDisconnectedMessage(username));
				}
			}
		});

		// Cleanup
		SESSION_AES_KEYS.remove(sessionId);
		ACTIVE_SESSIONS.remove(sessionId);
		HANDSHAKE_COMPLETE.remove(sessionId);
		SESSION_USERNAMES.remove(sessionId);
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		LOGGER.error("WebSocket error for session {}: {}",
				session.getId(), throwable.getMessage(), throwable);
	}

	// --- Handshake ---

	private void handleHandshake(String message, Session session) throws Exception {
		String sessionId = session.getId();

		// Client sends: { username, encryptedAesKey }
		Box box = new BoxCodec().decode(message);
		JsonObject payload = box.getPayload();

		String username = payload.has("username") ? payload.get("username").getAsString() : "Player";
		String encryptedAesKeyStr = payload.get("encryptedAesKey").getAsString();

		// Decrypt the AES key with our RSA private key
		byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyStr);
		byte[] aesKeyBytes = CryptoUtils.rsaDecrypt(encryptedAesKey, RSA_KEY_PAIR.getPrivate());
		SecretKey aesKey = CryptoUtils.bytesToAesKey(aesKeyBytes);

		SESSION_AES_KEYS.put(sessionId, aesKey);
		SESSION_USERNAMES.put(sessionId, username);
		HANDSHAKE_COMPLETE.put(sessionId, true);

		LOGGER.info("Handshake complete with {} ({})", username, sessionId);

		// Send handshake confirmation
		JsonObject confirmPayload = new JsonObject();
		confirmPayload.addProperty("type", Constants.MSG_HANDSHAKE_COMPLETE);
		confirmPayload.addProperty("message", "Welcome, " + username + "!");
		sendEncrypted(session, sessionId, confirmPayload);

		// Join the lobby
		joinLobby(session, sessionId, username);
	}

	// --- Lobby ---

	private void joinLobby(Session session, String sessionId, String username) {
		GameLobby.JoinResult result = LOBBY.joinPlayer(sessionId, username);
		GameSession gameSession = result.session();

		if (!result.gameReady()) {
			// First player — wait for opponent
			sendEncrypted(session, sessionId, gameSession.buildLobbyWaitingMessage());
			LOGGER.info("{} is waiting for an opponent in session {}",
					username, gameSession.getSessionId());
		} else {
			// Second player — both are ready, start mode selection
			LOGGER.info("Session {} is full. Starting mode selection.", gameSession.getSessionId());

			String player0SessionKey = gameSession.getSessionKey(0);
			String player1SessionKey = gameSession.getSessionKey(1);
			Session player0Session = ACTIVE_SESSIONS.get(player0SessionKey);
			Session player1Session = ACTIVE_SESSIONS.get(player1SessionKey);

			String player0Name = gameSession.getGameState().getPlayer(0).getUsername();
			String player1Name = gameSession.getGameState().getPlayer(1).getUsername();

			if (player0Session != null) {
				sendEncrypted(player0Session, player0SessionKey,
						gameSession.buildModeSelectMessage(player1Name));
			}
			if (player1Session != null) {
				sendEncrypted(player1Session, player1SessionKey,
						gameSession.buildModeSelectMessage(player0Name));
			}
		}
	}

	// --- Game Message Routing ---

	private void handleGameMessage(String message, Session session) throws Exception {
		String sessionId = session.getId();

		// Decrypt the message
		SecretKey aesKey = SESSION_AES_KEYS.get(sessionId);
		byte[] encryptedBytes = Base64.getDecoder().decode(message);
		byte[] decryptedBytes = CryptoUtils.aesDecrypt(encryptedBytes, aesKey);
		String jsonString = new String(decryptedBytes);

		JsonObject payload = JsonParser.parseString(jsonString).getAsJsonObject();
		// Also try Box format
		if (payload.has("payload")) {
			payload = payload.getAsJsonObject("payload");
		}

		String type = payload.get("type").getAsString();

		GameSession gameSession = LOBBY.getSessionForPlayer(sessionId).orElse(null);
		if (gameSession == null) {
			sendEncrypted(session, sessionId,
					GameSession.buildErrorMessage("You are not in a game session."));
			return;
		}

		int playerIndex = gameSession.getPlayerIndex(sessionId);

		switch (type) {
			case Constants.MSG_SELECT_MODE -> handleModeSelect(gameSession, playerIndex, payload);
			case Constants.MSG_ATTACK -> handleAttack(gameSession, playerIndex, payload);
			case Constants.MSG_FORFEIT -> handleForfeit(gameSession, playerIndex);
			default -> {
				LOGGER.warn("Unknown message type from {}: {}", sessionId, type);
				sendEncrypted(session, sessionId,
						GameSession.buildErrorMessage("Unknown message type: " + type));
			}
		}
	}

	private void handleModeSelect(GameSession gameSession, int playerIndex, JsonObject payload) {
		String modeName = payload.get("mode").getAsString();
		GameMode mode;
		try {
			mode = GameMode.valueOf(modeName.toUpperCase());
		} catch (IllegalArgumentException e) {
			sendToPlayer(gameSession, playerIndex,
					GameSession.buildErrorMessage("Invalid game mode: " + modeName));
			return;
		}

		GameMode resolvedMode = gameSession.voteMode(playerIndex, mode);

		if (resolvedMode != null) {
			// Both voted — start the game
			LOGGER.info("Session {}: Mode resolved to {}",
					gameSession.getSessionId(), resolvedMode.getDisplayName());

			gameSession.startGame(resolvedMode);

			// Send game_start to both players
			sendToPlayer(gameSession, 0, gameSession.buildGameStartMessage(0));
			sendToPlayer(gameSession, 1, gameSession.buildGameStartMessage(1));

			// Send turn messages
			int currentTurn = gameSession.getGameState().getCurrentTurnIndex();
			sendToPlayer(gameSession, currentTurn, gameSession.buildYourTurnMessage(currentTurn));
			sendToPlayer(gameSession, 1 - currentTurn, gameSession.buildWaitTurnMessage(1 - currentTurn));
		}
		// If only one has voted, they just wait
	}

	private void handleAttack(GameSession gameSession, int playerIndex, JsonObject payload) {
		// Parse weapon
		String weaponName = payload.get("weapon").getAsString();
		WeaponType weapon;
		try {
			weapon = WeaponType.valueOf(weaponName.toUpperCase());
		} catch (IllegalArgumentException e) {
			sendToPlayer(gameSession, playerIndex,
					GameSession.buildErrorMessage("Invalid weapon: " + weaponName));
			return;
		}

		// Parse target
		String targetStr = payload.get("target").getAsString();
		Coordinate target;
		try {
			target = Coordinate.fromInput(targetStr);
		} catch (IllegalArgumentException e) {
			sendToPlayer(gameSession, playerIndex,
					GameSession.buildErrorMessage("Invalid target: " + targetStr));
			return;
		}

		// Parse direction (optional, for LINE_BARRAGE)
		Direction direction = Direction.HORIZONTAL;
		if (payload.has("direction")) {
			try {
				direction = Direction.valueOf(payload.get("direction").getAsString().toUpperCase());
			} catch (IllegalArgumentException e) {
				// Default to horizontal
			}
		}

		// Execute the attack
		AttackResult result = gameSession.processAttack(playerIndex, weapon, target, direction);

		if (result == null) {
			sendToPlayer(gameSession, playerIndex,
					GameSession.buildErrorMessage(
							"Invalid attack. Check turn order, weapon availability, and coordinates."));
			return;
		}

		int defenderIndex = 1 - playerIndex;

		// Send results to both players
		sendToPlayer(gameSession, playerIndex,
				gameSession.buildAttackResultMessage(result, playerIndex));
		sendToPlayer(gameSession, defenderIndex,
				gameSession.buildIncomingAttackMessage(result, defenderIndex));

		// Check game over
		if (result.isGameOver()) {
			sendToPlayer(gameSession, 0, gameSession.buildGameOverMessage(0));
			sendToPlayer(gameSession, 1, gameSession.buildGameOverMessage(1));
		} else {
			// Send turn messages for the next turn
			int currentTurn = gameSession.getGameState().getCurrentTurnIndex();
			sendToPlayer(gameSession, currentTurn,
					gameSession.buildYourTurnMessage(currentTurn));
			sendToPlayer(gameSession, 1 - currentTurn,
					gameSession.buildWaitTurnMessage(1 - currentTurn));
		}
	}

	private void handleForfeit(GameSession gameSession, int playerIndex) {
		gameSession.getGameState().forfeit(playerIndex);

		sendToPlayer(gameSession, 0, gameSession.buildGameOverMessage(0));
		sendToPlayer(gameSession, 1, gameSession.buildGameOverMessage(1));
	}

	// --- Sending Helpers ---

	/**
	 * Sends an encrypted JSON message to a specific player in a game session.
	 */
	private void sendToPlayer(GameSession gameSession, int playerIndex, JsonObject payload) {
		String wsSessionId = gameSession.getSessionKey(playerIndex);
		if (wsSessionId == null)
			return;

		Session wsSession = ACTIVE_SESSIONS.get(wsSessionId);
		if (wsSession == null || !wsSession.isOpen())
			return;

		sendEncrypted(wsSession, wsSessionId, payload);
	}

	/**
	 * Encrypts a JSON payload with the session's AES key and sends it.
	 */
	private void sendEncrypted(Session wsSession, String wsSessionId, JsonObject payload) {
		try {
			SecretKey aesKey = SESSION_AES_KEYS.get(wsSessionId);
			if (aesKey == null) {
				LOGGER.warn("No AES key for session {}, sending plain", wsSessionId);
				sendPlainJson(wsSession, payload);
				return;
			}

			String json = payload.toString();
			byte[] encrypted = CryptoUtils.aesEncrypt(json.getBytes(), aesKey);
			String encoded = Base64.getEncoder().encodeToString(encrypted);
			wsSession.getBasicRemote().sendText(encoded);
		} catch (Exception e) {
			LOGGER.error("Failed to send encrypted message to {}", wsSessionId, e);
		}
	}

	/**
	 * Sends a plain (unencrypted) JSON message — only used before handshake.
	 */
	private void sendPlainJson(Session wsSession, JsonObject payload) {
		try {
			String message = new BoxCodec().encode(new Box(payload));
			wsSession.getBasicRemote().sendText(message);
		} catch (Exception e) {
			LOGGER.error("Failed to send plain message to {}", wsSession.getId(), e);
		}
	}

	// --- For testing ---

	static GameLobby getLobby() {
		return LOBBY;
	}
}
