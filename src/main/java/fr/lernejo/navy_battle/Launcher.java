package fr.lernejo.navy_battle;

import com.sun.net.httpserver.HttpServer;
import fr.lernejo.navy_battle.handlers.StartGameHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.Executors;

public class Launcher {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -jar <app.jar> <port> [adversary_url]");
            return;
        }

        // Parse port
        int port = Integer.parseInt(args[0]);
        String serverId = UUID.randomUUID().toString(); // Unique server ID
        String serverUrl = "http://localhost:" + port;

        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/ping", exchange -> {
            String body = "OK";
            exchange.sendResponseHeaders(200, body.length());
            try (var os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        });
        server.createContext("/api/game/start", new StartGameHandler(serverId, serverUrl));
        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
        System.out.println("Server started on port " + port);

        // If adversary URL is provided, send a POST request
        if (args.length == 2) {
            String adversaryUrl = args[1];
            sendStartRequest(adversaryUrl, serverId, serverUrl, "I will crush you!");
        }
    }

    private static void sendStartRequest(String adversaryUrl, String id, String url, String message) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = String.format("{\"id\":\"%s\", \"url\":\"%s\", \"message\":\"%s\"}", id, url, message);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(adversaryUrl + "/api/game/start"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response from adversary: " + response.body());
        } catch (Exception e) {
            System.err.println("Failed to send start request: " + e.getMessage());
        }
    }
}

