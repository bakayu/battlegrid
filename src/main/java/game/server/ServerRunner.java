package game.server;

import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ServerRunner {
    public static void main(String[] args) {
        // Scan for classes annotated with @ServerEndpoint
        Server server = new Server("localhost", 8025, "/websockets", null, GameServerEndpoint.class);

        try {
            server.start();
            System.out.println("Press any key to stop the server...");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}
