package game.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import game.client.render.BoardRenderer;
import game.client.render.HudRenderer;
import game.common.Box;
import game.common.BoxCodec;
import game.common.Constants;
import game.common.CryptoUtils;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint
public class GameClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameClient.class);

    private Session session;
    private SecretKey aesKey;
    private boolean handshakeComplete = false;
    private String username;

    final ClientGameState gameState = new ClientGameState();
    volatile boolean running = true;
    private volatile boolean waitingForInput = false;
    private volatile boolean pendingForfeitConfirm = false;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        LOGGER.info("Connected to server. Waiting for handshake...");
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            if (!handshakeComplete) {
                handleHandshakeMessage(message);
            } else {
                handleEncryptedMessage(message);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing message from server.", e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        LOGGER.info("Disconnected from server: {}", reason.getReasonPhrase());
        running = false;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.error("WebSocket error: {}", throwable.getMessage());
    }

    // --- Handshake ---

    private void handleHandshakeMessage(String message) throws Exception {
        Box handshakeBox = new BoxCodec().decode(message);
        JsonObject payload = handshakeBox.getPayload();
        String type = payload.has("type") ? payload.get("type").getAsString() : "";

        if (Constants.MSG_HANDSHAKE_RSA_KEY.equals(type)) {
            String serverRsaPublicKeyStr = payload.get("publicKey").getAsString();
            PublicKey serverRsaPublicKey = CryptoUtils.stringToPublicKey(serverRsaPublicKeyStr);

            this.aesKey = CryptoUtils.generateAESKey();
            byte[] encryptedAesKey = CryptoUtils.rsaEncrypt(aesKey.getEncoded(), serverRsaPublicKey);
            String encryptedAesKeyString = Base64.getEncoder().encodeToString(encryptedAesKey);

            JsonObject handshakeResponse = new JsonObject();
            handshakeResponse.addProperty("username", this.username);
            handshakeResponse.addProperty("encryptedAesKey", encryptedAesKeyString);

            Box responseBox = new Box(handshakeResponse);
            session.getBasicRemote().sendText(new BoxCodec().encode(responseBox));

            handshakeComplete = true;
            gameState.setPhase(ClientGameState.ClientPhase.HANDSHAKE);
            LOGGER.info("Handshake complete. Secure connection established as [{}].", this.username);
        }
    }

    // --- Encrypted Message Handling ---

    private void handleEncryptedMessage(String message) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(message);
        byte[] decryptedBytes = CryptoUtils.aesDecrypt(encryptedBytes, aesKey);
        String json = new String(decryptedBytes);

        JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
        String type = payload.get("type").getAsString();

        switch (type) {
            case Constants.MSG_HANDSHAKE_COMPLETE -> handleHandshakeComplete(payload);
            case Constants.MSG_LOBBY_WAITING -> handleLobbyWaiting(payload);
            case Constants.MSG_LOBBY_MODE_SELECT -> handleModeSelect(payload);
            case Constants.MSG_GAME_START -> handleGameStart(payload);
            case Constants.MSG_YOUR_TURN -> handleYourTurn(payload);
            case Constants.MSG_WAIT_TURN -> handleWaitTurn(payload);
            case Constants.MSG_ATTACK_RESULT -> handleAttackResult(payload);
            case Constants.MSG_INCOMING_ATTACK -> handleIncomingAttack(payload);
            case Constants.MSG_GAME_OVER -> handleGameOver(payload);
            case Constants.MSG_ERROR -> handleError(payload);
            case Constants.MSG_OPPONENT_DISCONNECTED -> handleOpponentDisconnected(payload);
            case Constants.MSG_PLAY_AGAIN_PROMPT -> handlePlayAgainPrompt(payload);
            case Constants.MSG_PLAY_AGAIN_WAITING -> handlePlayAgainWaiting(payload);
            default -> LOGGER.warn("Unknown message type: {}", type);
        }
    }

    // --- Message Handlers ---

    private void handleHandshakeComplete(JsonObject payload) {
        String msg = payload.get("message").getAsString();
        print(HudRenderer.clearScreen());
        print("\n  " + msg + "\n");
    }

    private void handleLobbyWaiting(JsonObject payload) {
        gameState.setPhase(ClientGameState.ClientPhase.LOBBY_WAITING);
        String msg = payload.get("message").getAsString();
        print(HudRenderer.renderWaiting(msg));
    }

    private void handleModeSelect(JsonObject payload) {
        gameState.resetForNewGame();
        gameState.applyModeSelect(payload);
        renderModeSelection();
    }

    private void handleGameStart(JsonObject payload) {
        gameState.applyGameStart(payload);

        print(HudRenderer.clearScreen());
        print("\n");
        print("  ⚔ Game started! Mode: " + gameState.getMode()
                + " (" + gameState.getGridSize() + "×" + gameState.getGridSize() + ")\n");
        print("  Opponent: " + gameState.getOpponentName() + "\n\n");

        renderBoards();
    }

    private void handleYourTurn(JsonObject payload) {
        gameState.applyYourTurn(payload);
        renderFullHud(null);
        waitingForInput = true;
        pendingForfeitConfirm = false;
    }

    private void handleWaitTurn(JsonObject payload) {
        gameState.applyWaitTurn(payload);
        renderFullHud(null);
        print(HudRenderer.renderWaiting(
                payload.has("message") ? payload.get("message").getAsString()
                        : "Waiting for opponent..."));
    }

    private void handleAttackResult(JsonObject payload) {
        gameState.applyAttackResult(payload);

        print(HudRenderer.clearScreen());

        // Attack summary header
        int hits = payload.get("hits").getAsInt();
        int misses = payload.get("misses").getAsInt();
        String weapon = payload.get("weapon").getAsString();
        String target = payload.get("target").getAsString();

        StringBuilder summary = new StringBuilder();
        summary.append("\n  ── YOUR ATTACK ──\n");
        summary.append("  ").append(weapon).append(" → ").append(target).append(": ");
        if (hits > 0) {
            summary.append("\033[31m\033[1m").append(hits).append(" HIT(s)\033[0m");
        }
        if (misses > 0) {
            if (hits > 0)
                summary.append(", ");
            summary.append("\033[34m").append(misses).append(" miss(es)\033[0m");
        }
        summary.append("\n");

        var sunkShips = payload.getAsJsonArray("sunkShips");
        for (int i = 0; i < sunkShips.size(); i++) {
            JsonObject sunk = sunkShips.get(i).getAsJsonObject();
            summary.append("  \033[31m\033[1m💥 ").append(sunk.get("displayName").getAsString())
                    .append(" SUNK!\033[0m\n");
        }
        summary.append("\n");

        print(summary.toString());
        renderBoards();
    }

    private void handleIncomingAttack(JsonObject payload) {
        gameState.applyIncomingAttack(payload);

        String attackerName = payload.get("attackerName").getAsString();
        int hits = payload.get("hits").getAsInt();

        print(HudRenderer.clearScreen());

        StringBuilder summary = new StringBuilder();
        summary.append("\n  ── INCOMING ATTACK ──\n");
        summary.append("  ").append(attackerName).append(" fired ").append(payload.get("weapon").getAsString())
                .append(" at ").append(payload.get("target").getAsString()).append("\n");

        if (hits > 0) {
            summary.append("  \033[31m\033[1m").append(hits).append(" of your tiles were hit!\033[0m\n");
        } else {
            summary.append("  \033[34mAll missed!\033[0m\n");
        }

        var sunkShips = payload.getAsJsonArray("sunkShips");
        for (int i = 0; i < sunkShips.size(); i++) {
            JsonObject sunk = sunkShips.get(i).getAsJsonObject();
            summary.append("  \033[31m\033[1m💥 Your ").append(sunk.get("displayName").getAsString())
                    .append(" was SUNK!\033[0m\n");
        }
        summary.append("\n");

        print(summary.toString());
        renderBoards();
    }

    private void handleGameOver(JsonObject payload) {
        gameState.applyGameOver(payload);
        print(HudRenderer.clearScreen());
        print(HudRenderer.renderGameOver(payload));
        // Don't set running=false — wait for play-again prompt
    }

    private void handlePlayAgainPrompt(JsonObject payload) {
        gameState.applyPlayAgainPrompt();
        print("\n\033[1m\033[33m  Play again? (yes/no): \033[0m");
    }

    private void handlePlayAgainWaiting(JsonObject payload) {
        gameState.applyPlayAgainWaiting();
        String msg = payload.has("message") ? payload.get("message").getAsString()
                : "Waiting for opponent...";
        print(HudRenderer.renderWaiting(msg));
    }

    private void handleError(JsonObject payload) {
        String msg = payload.get("message").getAsString();
        if (gameState.getPhase() == ClientGameState.ClientPhase.IN_GAME_YOUR_TURN) {
            print(HudRenderer.updateErrorInPlace(msg));
        } else {
            print("\n  \033[31m✖ " + msg + "\033[0m\n");
        }
    }

    private void handleOpponentDisconnected(JsonObject payload) {
        String msg = payload.get("message").getAsString();
        print("\n  \033[33m⚠ " + msg + "\033[0m\n");
    }

    // --- Sending ---

    private void sendEncrypted(JsonObject payload) {
        if (!handshakeComplete || aesKey == null) {
            LOGGER.warn("Cannot send — handshake not complete.");
            return;
        }
        try {
            String json = payload.toString();
            byte[] encrypted = CryptoUtils.aesEncrypt(json.getBytes(), aesKey);
            session.getBasicRemote().sendText(Base64.getEncoder().encodeToString(encrypted));
        } catch (Exception e) {
            LOGGER.error("Failed to send encrypted message.", e);
        }
    }

    void sendModeVote(int modeIndex) {
        var modes = gameState.getAvailableModes();
        if (modes == null || modeIndex < 0 || modeIndex >= modes.size()) {
            print("\n  \033[31mInvalid choice.\033[0m\n");
            return;
        }

        String modeName = modes.get(modeIndex).getAsJsonObject().get("name").getAsString();

        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_SELECT_MODE);
        payload.addProperty("mode", modeName);
        sendEncrypted(payload);

        print(HudRenderer.renderWaiting("Vote submitted. Waiting for opponent's choice..."));
    }

    void sendAttack(String weaponName, String target, String direction) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_ATTACK);
        payload.addProperty("weapon", weaponName);
        payload.addProperty("target", target);
        payload.addProperty("direction", direction);
        sendEncrypted(payload);
    }

    void sendForfeit() {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_FORFEIT);
        sendEncrypted(payload);
    }

    void sendPlayAgain(boolean wantsToPlay) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", Constants.MSG_PLAY_AGAIN);
        payload.addProperty("answer", wantsToPlay);
        sendEncrypted(payload);
    }

    // --- Rendering ---

    private void renderBoards() {
        if (gameState.getYourBoard() != null && gameState.getEnemyBoard() != null) {
            print(BoardRenderer.renderSideBySide(
                    "YOUR FLEET", gameState.getYourBoard(), true,
                    "ENEMY WATERS", gameState.getEnemyBoard(), false));
        }
    }

    private void renderFullHud(String errorMessage) {
        print(HudRenderer.clearScreen());
        print(HudRenderer.renderHeader(gameState.getTurnNumber(),
                gameState.getMode(), gameState.getOpponentName()));

        renderBoards();

        if (gameState.getFleetStatus() != null) {
            print(HudRenderer.renderFleetStatus("YOUR FLEET", gameState.getFleetStatus()));
        }

        if (gameState.getMessages() != null) {
            print(HudRenderer.renderMessages(gameState.getMessages()));
        }

        if (gameState.getPhase() == ClientGameState.ClientPhase.IN_GAME_YOUR_TURN
                && gameState.getWeapons() != null) {
            print(HudRenderer.renderWeapons(gameState.getWeapons()));
            print(HudRenderer.renderInputBlock(errorMessage));
        }
    }

    private void renderModeSelection() {
        print(HudRenderer.clearScreen());
        print("\n  \033[1m\033[36m⚔ BattleGrid — Mode Selection ⚔\033[0m\n");
        print("  Opponent: " + gameState.getOpponentName() + "\n\n");

        var modes = gameState.getAvailableModes();
        for (int i = 0; i < modes.size(); i++) {
            JsonObject mode = modes.get(i).getAsJsonObject();
            print(String.format("  \033[1m\033[32m[%d]\033[0m %s — %dx%d grid, %d ships\n",
                    i + 1,
                    mode.get("displayName").getAsString(),
                    mode.get("gridSize").getAsInt(),
                    mode.get("gridSize").getAsInt(),
                    mode.get("shipCount").getAsInt()));
        }
        print("\n  \033[1mSelect mode (1-" + modes.size() + "): \033[0m");
    }

    // --- Input Processing ---

    void processInput(String input) {
        ClientGameState.ClientPhase phase = gameState.getPhase();

        switch (phase) {
            case MODE_SELECT -> {
                try {
                    int choice = Integer.parseInt(input.trim()) - 1;
                    sendModeVote(choice);
                } catch (NumberFormatException e) {
                    print("\n  \033[31mEnter a number (1-"
                            + gameState.getAvailableModes().size() + ")\033[0m\n");
                }
            }
            case IN_GAME_YOUR_TURN -> {
                if (!waitingForInput) {
                    return;
                }

                // Handle forfeit confirmation
                if (pendingForfeitConfirm) {
                    if (input.equalsIgnoreCase("yes")) {
                        sendForfeit();
                        waitingForInput = false;
                        pendingForfeitConfirm = false;
                    } else {
                        pendingForfeitConfirm = false;
                        renderFullHud(null);
                    }
                    return;
                }

                InputParser.ParsedInput parsed = InputParser.parse(input, gameState.getWeapons());

                if (parsed.isForfeit()) {
                    pendingForfeitConfirm = true;
                    print(HudRenderer.updateErrorInPlace("Are you sure? Type 'yes' to confirm forfeit."));
                    return;
                }

                if (!parsed.isValid()) {
                    print(HudRenderer.updateErrorInPlace(parsed.errorMessage()));
                    return;
                }

                sendAttack(parsed.weaponName(), parsed.target(), parsed.direction());
                waitingForInput = false;
                pendingForfeitConfirm = false;
            }
            case PLAY_AGAIN_PROMPT -> {
                if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y")) {
                    sendPlayAgain(true);
                } else if (input.equalsIgnoreCase("no") || input.equalsIgnoreCase("n")) {
                    sendPlayAgain(false);
                    running = false;
                } else {
                    print("\n  \033[31mType 'yes' or 'no'.\033[0m\n");
                    print("\033[1m\033[33m  Play again? (yes/no): \033[0m");
                }
            }
            case GAME_OVER -> {
                // Ignore input while waiting for play-again prompt from server
            }
            default -> {
                // Ignore input in other phases
            }
        }
    }

    // --- Utility ---

    private void print(String text) {
        System.out.print(text);
        System.out.flush();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // --- Main Entry Point ---

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("\033[2J\033[H"); // Clear screen
        System.out.println("\033[1m\033[36m");
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║          ⚔  BattleGrid  ⚔           ║");
        System.out.println("  ║     Naval Warfare Terminal Game      ║");
        System.out.println("  ╚══════════════════════════════════════╝");
        System.out.println("\033[0m");

        try {
            System.out.print("  Enter server IP or hostname (default: localhost): ");
            String serverHost = reader.readLine().trim();
            if (serverHost.isEmpty())
                serverHost = "localhost";

            System.out.print("  Enter server port (default: " + Constants.SERVER_PORT + "): ");
            String portInput = reader.readLine().trim();
            int port = portInput.isEmpty() ? Constants.SERVER_PORT : Integer.parseInt(portInput);

            System.out.print("  Enter your username: ");
            String usernameInput = reader.readLine().trim();

            GameClient client = new GameClient();
            client.setUsername(usernameInput.isEmpty() ? Constants.DEFAULT_USERNAME : usernameInput);

            URI uri = new URI("ws://" + serverHost + ":" + port + Constants.WEBSOCKET_PATH);

            System.out.println("\n  Connecting to " + uri + "...");

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(client, uri);

            // Main input loop
            while (client.running) {
                if (!reader.ready()) {
                    Thread.sleep(100);
                    continue;
                }

                String input = reader.readLine();
                if (input == null)
                    break;
                input = input.trim();

                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    break;
                }

                client.processInput(input);
            }
        } catch (Exception e) {
            System.err.println("\n  \033[31mConnection failed: " + e.getMessage() + "\033[0m");
            e.printStackTrace();
        }

        System.out.println("\n  Goodbye!\n");
    }
}
