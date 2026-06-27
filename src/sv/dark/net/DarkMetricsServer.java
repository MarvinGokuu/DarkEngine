// Reading Order: 00010110
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.net;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.admin.AdminController;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.state.DarkStateLayout;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;

/**
 * Pure NIO Async HTTP Gateway (Output Peripheral).
 * 
 * <p>Reemplaza el I/O bloqueante tradicional por Sockets Asíncronos NIO.
 * It has no business logic, does not format Strings, and does not access the Kernel.
 * It simply transfers bytes from the AdminController to the Socket non-blocking.
 * 
 * <p>AAA+ STANDARD:
 * <ul>
 *   <li>Non-Blocking I/O (AsynchronousServerSocketChannel)</li>
 *   <li>Pure Responsibility (Transport Only)</li>
 * </ul>
 * 
 * @author Marvin Alexander Flores Canales
 * @since 3.0
 */
@AAACertified(date = "2026-06-27", maxLatencyNs = 50_000, minThroughput = 5000, alignment = 0, lockFree = true, offHeap = false, notes = "Pure NIO Asynchronous HTTP Gateway")
public final class DarkMetricsServer {

    private AsynchronousServerSocketChannel serverChannel;
    private final int port;
    private final SectorMemoryVault vault;
    private volatile boolean running = false;

    public DarkMetricsServer(int port, SectorMemoryVault vault) {
        this.port = port;
        this.vault = vault;
        
        // Start Native WebSocket Server for Web Editor (port 13001)
        try {
            new DarkWebEditorSocket(13001, vault).start();
        } catch (Exception e) {
            DarkLogger.error("WEB EDITOR", "Failed to start WebSocket server: " + e.getMessage());
        }
    }

    public void start() {
        try {
            serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverChannel.bind(new InetSocketAddress(port));
            running = true;
            DarkLogger.info("METRICS GATEWAY", "Listening on NIO port " + port);
            acceptNext();
        } catch (IOException e) {
            DarkLogger.error("METRICS GATEWAY", "Failed to start NIO server: " + e.getMessage());
        }
    }

    private void acceptNext() {
        if (!running) return;
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel client, Void attachment) {
                acceptNext(); // Accept next immediately
                handleClient(client);
            }
            @Override
            public void failed(Throwable exc, Void attachment) {
                if (running) {
                    DarkLogger.error("METRICS GATEWAY", "Accept failed: " + exc.getMessage());
                    acceptNext();
                }
            }
        });
    }

    private void handleClient(AsynchronousSocketChannel client) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        client.read(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead > 0) {
                    buffer.flip();
                    String request = StandardCharsets.UTF_8.decode(buffer).toString();
                    processRequest(client, request);
                } else {
                    closeClient(client);
                }
            }
            @Override
            public void failed(Throwable exc, Void attachment) {
                closeClient(client);
            }
        });
    }

    private void processRequest(AsynchronousSocketChannel client, String request) {
        try {
            String[] lines = request.split("\r\n");
            if (lines.length == 0) return;
            String[] parts = lines[0].split(" ");
            if (parts.length < 2) return;
            String path = parts[1];

            if (path.equals("/metrics")) {
                byte[] response = AdminController.getLatestSnapshot();
                sendResponse(client, "200 OK", "application/json", response);
            } else if (path.startsWith("/api/state?")) {
                String query = path.substring("/api/state?".length());
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if (pair[0].equals("x")) vault.writeInt(DarkStateLayout.PLAYER_X, Integer.parseInt(pair[1]));
                        if (pair[0].equals("y")) vault.writeInt(DarkStateLayout.PLAYER_Y, Integer.parseInt(pair[1]));
                    }
                }
                sendResponse(client, "200 OK", "text/plain", new byte[0]);
            } else if (path.equals("/editor")) {
                serveResource(client, "/sv/dark/admin/editor.html");
            } else if (path.equals("/")) {
                serveResource(client, "/sv/dark/admin/index.html");
            } else {
                sendResponse(client, "404 Not Found", "text/plain", new byte[0]);
            }
        } catch (Exception e) {
            closeClient(client);
        }
    }

    private void serveResource(AsynchronousSocketChannel client, String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                sendResponse(client, "404 Not Found", "text/plain", new byte[0]);
                return;
            }
            byte[] data = is.readAllBytes();
            sendResponse(client, "200 OK", "text/html", data);
        } catch (IOException e) {
            closeClient(client);
        }
    }

    private void sendResponse(AsynchronousSocketChannel client, String status, String contentType, byte[] body) {
        String header = "HTTP/1.1 " + status + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + body.length + "\r\n\r\n";
        
        ByteBuffer buffer = ByteBuffer.allocate(header.length() + body.length);
        buffer.put(header.getBytes(StandardCharsets.UTF_8));
        buffer.put(body);
        buffer.flip();
        
        client.write(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                if (buffer.hasRemaining()) {
                    client.write(buffer, null, this);
                } else {
                    closeClient(client);
                }
            }
            @Override
            public void failed(Throwable exc, Void attachment) {
                closeClient(client);
            }
        });
    }

    private void closeClient(AsynchronousSocketChannel client) {
        try { client.close(); } catch (IOException ignored) {}
    }

    public void stop() {
        running = false;
        try {
            if (serverChannel != null) serverChannel.close();
        } catch (IOException ignored) {}
        DarkLogger.info("METRICS GATEWAY", "Stopped");
    }
}
