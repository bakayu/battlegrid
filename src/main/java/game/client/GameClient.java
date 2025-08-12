package game.client;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Scanner;

@ClientEndpoint
public class GameClient {

    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Connected to battlegrid server!");
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        logger.info("Server> {}", message);
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            logger.error("Failed to send message: {}", message, e);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        logger.info("Enter the server IP address: ");
        String serverIp = scanner.nextLine();

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = new URI(serverIp);
            GameClient client = new GameClient();
            container.connectToServer(client, uri);

            logger.info("Connected. Type messages and press Enter to send. Type 'exit' to quit.");
            String input;
            do {
                input = scanner.nextLine();
                if (!"exit".equalsIgnoreCase(input)) {
                    client.sendMessage(input);
                }
            } while (!"exit".equalsIgnoreCase(input));

        } catch (Exception e) {
            logger.error("Connection failed.", e);
        }
    }
}