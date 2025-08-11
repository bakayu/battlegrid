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
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = new URI("ws://localhost:8025/websockets/battlegrid");
            GameClient client = new GameClient();
            container.connectToServer(client, uri);

            System.out.println("Connected. Type messages and press Enter to send. Type 'exit' to quit.");
            Scanner scanner = new Scanner(System.in);
            String input;
            do {
                input = scanner.nextLine();
                if (!"exit".equalsIgnoreCase(input)) {
                    client.sendMessage(input);
                }
            } while (!"exit".equalsIgnoreCase(input));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

