// Reading Order: 00010110
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.net;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import sv.dark.core.AAACertified; // 00000100
import sv.dark.core.DarkLogger;
import sv.dark.admin.AdminController;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.state.DarkStateLayout;

/**
 * Blind HTTP Gateway (Output Peripheral).
 * 
 * <p>A "Blind" HTTP server that acts as an Output Peripheral.
 * It has no business logic, does not format Strings, and does not access the Kernel.
 * It simply transfers bytes from the AdminController to the Socket.
 * 
 * <p>AAA+ STANDARD:
 * <ul>
 *   <li>Zero-Allocation (uses pre-cooked bytes)</li>
 *   <li>Pure Responsibility (Transport Only)</li>
 * </ul>
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 100_000, minThroughput = 1000, alignment = 0, lockFree = true, offHeap = false, notes = "Blind HTTP Gateway (Zero-Allocation)")
public final class DarkMetricsServer {

    private final HttpServer server;

    public DarkMetricsServer(int port, SectorMemoryVault vault) throws IOException {
        // The server no longer needs the Kernel. It is a pure infrastructure component.
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        // Endpoints
        server.createContext("/", new RootHandler());
        server.createContext("/metrics", new MetricsHandler());
        server.createContext("/api/state", new StateHandler(vault));
        server.createContext("/editor", new EditorHandler());

        // Configuration: Default executor (null creates a default one)
        // For real AAA production, we would use a Virtual Threads Executor or similar,
        // but for the "Blind Server" standard, this is sufficient.
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        DarkLogger.info("METRICS GATEWAY", "Listening on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        DarkLogger.info("METRICS GATEWAY", "Stopped");
    }

    /**
     * "Blind" Handler - Zero Allocation
     */
    private static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 1. Static headers (no conditional logic)
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            // 2. Get the latest metrics "Snapshot" already formatted by the AdminConsumer
            // (Atomic reference read - Cost ~Ns)
            byte[] response = AdminController.getLatestSnapshot();

            // 3. Send raw, no concatenation, no processing
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    private static class StateHandler implements HttpHandler {
        private final SectorMemoryVault vault;
        public StateHandler(SectorMemoryVault vault) { this.vault = vault; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                try {
                    String[] params = query.split("&");
                    for (String param : params) {
                        String[] pair = param.split("=");
                        if (pair.length == 2) {
                            if (pair[0].equals("x")) vault.writeInt(DarkStateLayout.PLAYER_X, Integer.parseInt(pair[1]));
                            if (pair[0].equals("y")) vault.writeInt(DarkStateLayout.PLAYER_Y, Integer.parseInt(pair[1]));
                        }
                    }
                } catch (Exception e) {}
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        }
    }

    private static class EditorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            try (InputStream is = getClass().getResourceAsStream("/sv/dark/admin/editor.html");
                 OutputStream os = exchange.getResponseBody()) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                exchange.sendResponseHeaders(200, 0);
                is.transferTo(os);
            }
        }
    }

    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Only handle exactly "/"
            if (!exchange.getRequestURI().getPath().equals("/")) {
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
                return;
            }
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            try (InputStream is = getClass().getResourceAsStream("/sv/dark/admin/index.html");
                 OutputStream os = exchange.getResponseBody()) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                exchange.sendResponseHeaders(200, 0);
                is.transferTo(os);
            }
        }
    }
}
