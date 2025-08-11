package game.client;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.net.URI;
import java.util.Scanner;

@ClientEndpoint
public class GameClient {

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to battlegrid server!");
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Server> " + message);
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the server IP address: ");
        String serverIp = scanner.nextLine();

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = new URI(serverIp);
            GameClient client = new GameClient();
            container.connectToServer(client, uri);

            System.out.println("Connected. Type messages and press Enter to send. Type 'exit' to quit.");
            String input;
            do {
                input = scanner.nextLine();
                if (!"exit".equalsIgnoreCase(input)) {
                    client.sendMessage(input);
                }
            } while (!"exit".equalsIgnoreCase(input));

        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }
}