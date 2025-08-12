package game.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.glassfish.tyrus.server.Server;

public class ServerRunner {

    /**
     * Attempts to find the local LAN IP address of the machine.
     *
     * @return The site-local IPv4 address, or null if not found.
     */
    private static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()
                            && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while getting local IP address: " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        String hostIp = getLocalIpAddress();
        if (hostIp == null) {
            System.err.println("Could not find a local network IP. Server might not be accessible from LAN.");
            hostIp = "localhost"; // Fallback
        }

        // Bind to 0.0.0.0 to listen on all available network interfaces
        Server server = new Server("0.0.0.0", 8025, "/websockets", null, GameServerEndpoint.class);

        try {
            server.start();
            System.out.println("Server started. Clients can connect to: " + hostIp);
            System.out.println("Press any key to stop the server...");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}