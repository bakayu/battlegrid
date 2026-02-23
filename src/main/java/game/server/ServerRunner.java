package game.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import game.common.Constants;

/**
 * Starts the BattleGrid WebSocket server.
 */
public class ServerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRunner.class);

    public static void main(String[] args) {
        Server server = new Server("0.0.0.0", Constants.SERVER_PORT, "/websockets",
                null, GameServerEndpoint.class);

        try {
            server.start();

            LOGGER.info("========================================");
            LOGGER.info("  BattleGrid Server started!");
            LOGGER.info("  Port: {}", Constants.SERVER_PORT);
            LOGGER.info("  Endpoint: ws://<ip>:{}{}", Constants.SERVER_PORT, Constants.WEBSOCKET_PATH);
            LOGGER.info("----------------------------------------");
            printNetworkAddresses();
            LOGGER.info("========================================");
            LOGGER.info("Waiting for players to connect...");
            LOGGER.info("Press ENTER to stop the server.");

            // Block until user presses Enter
            System.in.read();

        } catch (Exception e) {
            LOGGER.error("Failed to start server", e);
        } finally {
            server.stop();
            LOGGER.info("Server stopped.");
        }
    }

    private static void printNetworkAddresses() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (ni.isLoopback() || !ni.isUp())
                    continue;

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.getHostAddress().contains(":"))
                        continue; // Skip IPv6
                    LOGGER.info("  Connect with: ws://{}:{}{}", addr.getHostAddress(),
                            Constants.SERVER_PORT, Constants.WEBSOCKET_PATH);
                }
            }
            LOGGER.info("  Local:        ws://localhost:{}{}", Constants.SERVER_PORT,
                    Constants.WEBSOCKET_PATH);
        } catch (Exception e) {
            LOGGER.warn("Could not detect network addresses: {}", e.getMessage());
        }
    }
}
