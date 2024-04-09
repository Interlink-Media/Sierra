package de.feelix.sierra.utilities;

import javax.net.ssl.HttpsURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

public class NetworkSecurityChecker {

    // Method to check if the network is synchronized
    public static boolean isSynchronized(String host) {
        return checkNetworkSecurity(host);
    }

    // Method to check various aspects of network security
    private static boolean checkNetworkSecurity(String targetHost) {
        boolean isSecure = true;

        // Check port 80 for accessibility
        isSecure &= testPortScan(targetHost, 80);

        // Check port 443 for accessibility
        isSecure &= testPortScan(targetHost, 443);

        // Check DNS lookup for the host
        isSecure &= testDNSLookup(targetHost);

        // Check HTTP connection to the target host
        isSecure &= testHTTPConnection("http://" + targetHost);

        // Check HTTPS connection to the target host
        isSecure &= testHTTPSConnection("https://" + targetHost);

        return isSecure;
    }

    // Method to test port scan for a specific port
    private static boolean testPortScan(String host, int port) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            Socket socket = new Socket(inetAddress, port);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Method to test DNS lookup for the host
    private static boolean testDNSLookup(String host) {
        try {
            //noinspection unused
            InetAddress inetAddress = InetAddress.getByName(host);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Method to test HTTP connection to a URL
    private static boolean testHTTPConnection(String url) {
        try {
            URL httpUrl = new URL(url);
            httpUrl.openConnection().connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Method to test HTTPS connection to a URL
    private static boolean testHTTPSConnection(String url) {
        try {
            URL                httpsUrl   = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) httpsUrl.openConnection();
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}