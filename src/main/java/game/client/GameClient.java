package game.client;

import java.net.URI;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import game.common.Box;
import game.common.BoxCodec;
import game.common.Constants;
import game.common.CryptoUtils;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint
public class GameClient {

    private Session session;
    private SecretKey aesKey;
    private boolean handshakeComplete = false;
    private String username;

    private static final Logger LOGGER = LoggerFactory.getLogger(GameClient.class);

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        LOGGER.info("Connection established. Waiting for server handshake...");
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            if (!handshakeComplete) {
                // This is the first message: the server's public RSA key
                Box handshakeBox = new BoxCodec().decode(message);
                String serverRsaPublicKeyStr = handshakeBox.getPayload().get("publicKey").getAsString();
                PublicKey serverRsaPublicKey = CryptoUtils.stringToPublicKey(serverRsaPublicKeyStr);

                // AES Key is generated, encrypted with server's public RSA key, and finally
                // sent back
                this.aesKey = CryptoUtils.generateAESKey();
                byte[] encryptedAesKey = CryptoUtils.rsaEncrypt(aesKey.getEncoded(), serverRsaPublicKey);
                String encryptedAesKeyString = Base64.getEncoder().encodeToString(encryptedAesKey);

                // the handshake response payload with username and encrypted key.
                JsonObject handshakeResponsePayload = new JsonObject();
                handshakeResponsePayload.addProperty("username", this.username);
                handshakeResponsePayload.addProperty("encryptedAesKey", encryptedAesKeyString);

                // Wrap in a Box and send to the server.
                Box responseBox = new Box(handshakeResponsePayload);
                session.getBasicRemote().sendText(new BoxCodec().encode(responseBox));

                handshakeComplete = true;
                LOGGER.info("Handshake complete. Secure connection established as [{}].", this.username);
                LOGGER.info("Type messages and press Enter to send. Type 'exit' to quit.");

            } else {
                // Subsequent messages are AES-encrypted game data
                byte[] decryptedBytes = CryptoUtils.aesDecrypt(Base64.getDecoder().decode(message), this.aesKey);
                String decryptedJson = new String(decryptedBytes);
                Box incomingBox = new BoxCodec().decode(decryptedJson);

                JsonObject payload = incomingBox.getPayload();
                String type = payload.get("type").getAsString();
                String sender = payload.has("sender") ? payload.get("sender").getAsString() : "Server";

                switch (type) {
                    case "player_joined":
                    case "player_left":
                        LOGGER.info("[System] {}", payload.get("message").getAsString());
                        break;
                    case "chat":
                        if (!this.username.equals(sender)) {
                            LOGGER.info("{}: {}", sender, payload.get("text").getAsString());
                        }
                        break;
                    default:
                        LOGGER.info("{}> {}", sender, payload);
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing message from server.", e);
        }
    }

    public void sendMessage(String message) {
        if (!handshakeComplete) {
            LOGGER.warn("Cannot send message — handshake not complete.");
            return;
        }
        try {
            // Create a payload, then encrypt it with AES before sending.
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "chat");
            payload.addProperty("text", message);

            String jsonPayload = new BoxCodec().encode(new Box(payload));
            byte[] encryptedPayload = CryptoUtils.aesEncrypt(jsonPayload.getBytes(), this.aesKey);
            session.getBasicRemote().sendText(Base64.getEncoder().encodeToString(encryptedPayload));

        } catch (Exception e) {
            LOGGER.error("Failed to send message: {}", message, e);
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("Enter the server IP address (e.g., 192.168.1.5 or localhost): ");
        String serverIp = scanner.nextLine();

        LOGGER.info("Enter your username (or press Enter for default): ");
        String usernameInput = scanner.nextLine();

        try {
            GameClient client = new GameClient();

            if (usernameInput == null || usernameInput.trim().isEmpty()) {
                client.setUsername(Constants.DEFAULT_USERNAME);
            } else {
                client.setUsername(usernameInput);
            }

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = new URI("ws://" + serverIp + ":8025/websockets/battlegrid");
            container.connectToServer(client, uri);

            // The main loop now just waits for the user to type 'exit'
            // All message sending happens after the handshake is complete.
            String input;
            do {
                input = scanner.nextLine();
                if (client.handshakeComplete && !"exit".equalsIgnoreCase(input)) {
                    client.sendMessage(input);
                }
            } while (!"exit".equalsIgnoreCase(input));

        } catch (Exception e) {
            LOGGER.error("Connection failed: {}", e.getMessage(), e);
        } finally {
            scanner.close();
        }
    }
}