package fr.lernejo.navy_battle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import fr.lernejo.navy_battle.handlers.FireHandler;
import fr.lernejo.navy_battle.handlers.StartGameHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class Launcher {

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -jar <app.jar> <port> [adversary_url]");
            return;
        }

        // Parse arguments
        int port = Integer.parseInt(args[0]);
        String serverId = UUID.randomUUID().toString(); // Unique server ID
        String serverUrl = "http://localhost:" + port;

        // Initialize game board
        Map<String, Boolean> gameBoard = initializeGameBoard();

        // Initialize adversary sea
        Map<String, String> adversarySea = initializeAdversarySea();

        // Start HTTP server
        HttpServer server = createHttpServer(port, gameBoard, serverId, serverUrl);
        server.start();
        System.out.println("Server started on port " + port);

        // Handle adversary if provided
        if (args.length == 2) {
            String adversaryUrl = args[1];
            playGame(adversaryUrl, serverId, serverUrl, adversarySea);
        }
    }

    private static Map<String, Boolean> initializeGameBoard() {
        Map<String, Boolean> gameBoard = new HashMap<>();
        gameBoard.put("B2", true);
        gameBoard.put("C2", true);
        gameBoard.put("D2", true);
        return gameBoard;
    }

    private static Map<String, String> initializeAdversarySea() {
        Map<String, String> adversarySea = new HashMap<>();
        for (char col = 'A'; col <= 'J'; col++) {
            for (int row = 1; row <= 10; row++) {
                adversarySea.put(col + String.valueOf(row), "unknown");
            }
        }
        return adversarySea;
    }

    private static HttpServer createHttpServer(int port, Map<String, Boolean> gameBoard, String serverId, String serverUrl) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/ping", exchange -> {
            String body = "OK";
            exchange.sendResponseHeaders(200, body.length());
            try (var os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        });
        server.createContext("/api/game/start", new StartGameHandler(serverId, serverUrl));
        server.createContext("/api/game/fire", new FireHandler(gameBoard));
        server.setExecutor(Executors.newFixedThreadPool(1));
        return server;
    }

    private static void playGame(String adversaryUrl, String serverId, String serverUrl, Map<String, String> adversarySea) {
        HttpClient client = HttpClient.newHttpClient();

        while (true) {
            // Take your turn
            String targetCell = chooseTargetCell(adversarySea);
            if (targetCell == null) {
                System.out.println("No more cells to target. Game over!");
                break;
            }

            System.out.println("Firing at: " + targetCell);
            String result = fireAtAdversary(client, adversaryUrl, targetCell, adversarySea);
            System.out.println("Result of firing: " + result);

            // Simulate waiting for the next turn
            try {
                Thread.sleep(1000); // Add delay for better visualization
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String chooseTargetCell(Map<String, String> adversarySea) {
        return adversarySea.entrySet().stream()
            .filter(entry -> "unknown".equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    private static String fireAtAdversary(HttpClient client, String adversaryUrl, String targetCell, Map<String, String> adversarySea) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(adversaryUrl + "/api/game/fire?cell=" + targetCell))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response from adversary: " + response.body());

            // Parse response and update adversary sea
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonResponse = mapper.readValue(response.body(), Map.class);

            String consequence = (String) jsonResponse.get("consequence");
            adversarySea.put(targetCell, consequence);

            boolean shipLeft = (boolean) jsonResponse.get("shipLeft");
            if (!shipLeft) {
                System.out.println("Adversary has no ships left. You win!");
                System.exit(0);
            }

            return consequence;
        } catch (Exception e) {
            System.err.println("Error firing at adversary: " + e.getMessage());
            return "error";
        }
    }
}

