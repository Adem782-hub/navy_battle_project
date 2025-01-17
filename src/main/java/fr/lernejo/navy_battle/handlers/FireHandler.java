package fr.lernejo.navy_battle.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class FireHandler implements HttpHandler {

    private final Map<String, Boolean> gameBoard;

    public FireHandler(Map<String, Boolean> gameBoard) {
        this.gameBoard = gameBoard;
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("cell=")) {
            exchange.sendResponseHeaders(400, -1); // Bad Request
            return;
        }

        String cell = query.split("=")[1].toUpperCase(); // Ensure cell is uppercase
        ObjectMapper mapper = new ObjectMapper();

        System.out.println("Incoming fire request on cell: " + cell);

        if (gameBoard.containsKey(cell)) {
            boolean alreadyFired = !gameBoard.get(cell); // Check if cell has already been fired

            if (alreadyFired) {
                System.out.println("Cell " + cell + " was already fired.");
                String response = mapper.writeValueAsString(Map.of(
                    "consequence", "miss",
                    "shipLeft", gameBoard.containsValue(true)
                ));
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            boolean hit = gameBoard.get(cell); // Check if it's a hit
            gameBoard.put(cell, false); // Mark the cell as fired
            System.out.println("Cell " + cell + " was a " + (hit ? "HIT" : "MISS"));

            boolean shipLeft = gameBoard.containsValue(true); // Check if any ship is left
            String consequence = hit ? (shipLeft ? "hit" : "sunk") : "miss";

            String response = mapper.writeValueAsString(Map.of(
                "consequence", consequence,
                "shipLeft", shipLeft
            ));
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } else {
            System.out.println("Cell " + cell + " is invalid or out of bounds.");
            String response = mapper.writeValueAsString(Map.of(
                "consequence", "miss",
                "shipLeft", gameBoard.containsValue(true)
            ));
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}

