package fr.lernejo.navy_battle.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class StartGameHandler implements HttpHandler {
    private final String serverId;
    private final String serverUrl;

    public StartGameHandler(String serverId, String serverUrl) {
        this.serverId = serverId;
        this.serverUrl = serverUrl;
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            var body = new String(exchange.getRequestBody().readAllBytes());
            var request = mapper.readTree(body);

            String response = mapper.writeValueAsString(new GameStartResponse(serverId, serverUrl, "May the best code win"));
            exchange.sendResponseHeaders(202, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            exchange.sendResponseHeaders(400, -1);
        }
    }
}

record GameStartResponse(String id, String url, String message) { }

