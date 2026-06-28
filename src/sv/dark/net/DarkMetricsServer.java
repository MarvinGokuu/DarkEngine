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

    // -------------------------------------------------------------------------
    // PRE-COMPILED HTTP HEADERS (Zero-GC: no String concat, no byte[] per request)
    // -------------------------------------------------------------------------
    // WHY static final byte[]: sendResponse() was previously doing:
    //   String header = "HTTP/1.1 " + status + ...  (heap String + implicit StringBuilder)
    //   ByteBuffer.allocate(header.length() + body.length) (new ByteBuffer per request)
    //   header.getBytes(...) (new byte[] per request)
    // = 3 GC objects per HTTP response. At 60 FPS metrics polling = 180 allocs/sec.
    // Pre-compiling to byte[] reduces this to 1 ByteBuffer of exact size.
    private static final byte[] HDR_200_JSON = ("HTTP/1.1 200 OK\r\n" +
        "Access-Control-Allow-Origin: *\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: ").getBytes(StandardCharsets.UTF_8);
    private static final byte[] HDR_200_TEXT = ("HTTP/1.1 200 OK\r\n" +
        "Access-Control-Allow-Origin: *\r\n" +
        "Content-Type: text/plain\r\n" +
        "Content-Length: ").getBytes(StandardCharsets.UTF_8);
    private static final byte[] HDR_200_HTML = ("HTTP/1.1 200 OK\r\n" +
        "Access-Control-Allow-Origin: *\r\n" +
        "Content-Type: text/html\r\n" +
        "Content-Length: ").getBytes(StandardCharsets.UTF_8);
    private static final byte[] HDR_404 = ("HTTP/1.1 404 Not Found\r\n" +
        "Content-Length: 0\r\n\r\n").getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRLF_CRLF = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    // Empty body constant — avoids new byte[0] per-request allocation.
    private static final byte[] EMPTY_BODY = new byte[0];

    // Per-thread scratch buffer for Content-Length ASCII encoding.
    // WHY ThreadLocal and NOT static byte[]: AsynchronousServerSocketChannel uses an
    // internal thread pool. Multiple CompletionHandler.completed() calls can execute
    // in parallel on different threads. A shared static byte[] would be a data race:
    // two threads writing CL_SCRATCH simultaneously → corrupted Content-Length.
    // ThreadLocal.get() is O(1) — one deref on Thread.threadLocals. No contention.
    private static final ThreadLocal<byte[]> CL_SCRATCH_TL =
        ThreadLocal.withInitial(() -> new byte[10]);



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
        // allocateDirect: native memory, outside the Java heap.
        // The GC does not trace or move this buffer. Reduces heap pressure.
        // NIO async API requires one buffer per in-flight read — cannot share.
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        client.read(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead > 0) {
                    buffer.flip();
                    processRequest(client, buffer);
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

    private boolean matchPathExact(ByteBuffer buffer, int start, int end, String expected) {
        if (end - start != expected.length()) return false;
        for (int i = 0; i < expected.length(); i++) {
            if (buffer.get(start + i) != (byte) expected.charAt(i)) return false;
        }
        return true;
    }

    private boolean matchPathPrefix(ByteBuffer buffer, int start, int end, String expected) {
        if (end - start < expected.length()) return false;
        for (int i = 0; i < expected.length(); i++) {
            if (buffer.get(start + i) != (byte) expected.charAt(i)) return false;
        }
        return true;
    }

    private int parseAsciiInt(ByteBuffer buffer, int start, int end) {
        int result = 0;
        int sign = 1;
        for (int i = start; i < end; i++) {
            byte b = buffer.get(i);
            if (b == '-') sign = -1;
            else if (b >= '0' && b <= '9') {
                result = result * 10 + (b - '0');
            } else {
                break; // Stop at first non-digit
            }
        }
        return result * sign;
    }

    private void processRequest(AsynchronousSocketChannel client, ByteBuffer buffer) {
        try {
            int limit = buffer.limit();
            int firstSpace = -1;
            int secondSpace = -1;
            
            for (int i = 0; i < limit; i++) {
                byte b = buffer.get(i);
                if (b == ' ') {
                    if (firstSpace == -1) firstSpace = i;
                    else if (secondSpace == -1) {
                        secondSpace = i;
                        break;
                    }
                } else if (b == '\r' || b == '\n') {
                    if (secondSpace == -1) secondSpace = i;
                    break;
                }
            }
            
            if (firstSpace == -1 || secondSpace == -1) {
                closeClient(client);
                return;
            }
            
            int pathStart = firstSpace + 1;
            int pathEnd = secondSpace;

            if (matchPathExact(buffer, pathStart, pathEnd, "/metrics")) {
                byte[] response = AdminController.getLatestSnapshot();
                sendResponseRaw(client, HDR_200_JSON, response);
            } else if (matchPathPrefix(buffer, pathStart, pathEnd, "/api/state?")) {
                int queryStart = pathStart + "/api/state?".length();
                // Zero-GC query parsing (e.g. x=10&y=20)
                int currentKeyStart = queryStart;
                while (currentKeyStart < pathEnd) {
                    int eqIdx = -1;
                    int ampIdx = pathEnd;
                    for (int i = currentKeyStart; i < pathEnd; i++) {
                        byte b = buffer.get(i);
                        if (b == '=') eqIdx = i;
                        else if (b == '&') { ampIdx = i; break; }
                    }
                    if (eqIdx != -1) {
                        int valStart = eqIdx + 1;
                        if (buffer.get(currentKeyStart) == 'x') {
                            vault.writeInt(DarkStateLayout.PLAYER_X, parseAsciiInt(buffer, valStart, ampIdx));
                        } else if (buffer.get(currentKeyStart) == 'y') {
                            vault.writeInt(DarkStateLayout.PLAYER_Y, parseAsciiInt(buffer, valStart, ampIdx));
                        }
                    }
                    currentKeyStart = ampIdx + 1;
                }
                sendResponseRaw(client, HDR_200_TEXT, EMPTY_BODY);
            } else if (matchPathExact(buffer, pathStart, pathEnd, "/editor")) {
                serveResource(client, "/sv/dark/admin/editor.html");
            } else if (matchPathExact(buffer, pathStart, pathEnd, "/")) {
                serveResource(client, "/sv/dark/admin/index.html");
            } else {
                // 404: send pre-compiled header directly (no body)
                ByteBuffer resp404 = ByteBuffer.allocate(HDR_404.length);
                resp404.put(HDR_404);
                resp404.flip();
                client.write(resp404, null, new CompletionHandler<Integer, Void>() {
                    @Override public void completed(Integer r, Void a) {
                        if (resp404.hasRemaining()) client.write(resp404, null, this);
                        else closeClient(client);
                    }
                    @Override public void failed(Throwable exc, Void a) { closeClient(client); }
                });
            }
        } catch (Exception e) {
            closeClient(client);
        }
    }


    private void serveResource(AsynchronousSocketChannel client, String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                ByteBuffer resp404 = ByteBuffer.allocate(HDR_404.length);
                resp404.put(HDR_404);
                resp404.flip();
                client.write(resp404, null, new CompletionHandler<Integer, Void>() {
                    @Override public void completed(Integer r, Void a) {
                        if (resp404.hasRemaining()) client.write(resp404, null, this);
                        else closeClient(client);
                    }
                    @Override public void failed(Throwable exc, Void a) { closeClient(client); }
                });
                return;
            }
            byte[] data = is.readAllBytes();
            sendResponseRaw(client, HDR_200_HTML, data);
        } catch (IOException e) {
            closeClient(client);
        }
    }

    /**
     * Zero-GC HTTP response writer.
     *
     * Assembles the response using pre-compiled header bytes + Content-Length
     * encoded as ASCII bytes without String.valueOf() or Integer.toString().
     * Only ONE ByteBuffer is allocated (sized to the exact response).
     *
     * BEFORE (3 GC objects per call):
     *   String header = "HTTP/1.1 " + status + ...  // String alloc
     *   byte[] hbytes = header.getBytes(...)         // byte[] alloc
     *   ByteBuffer buf = ByteBuffer.allocate(...)    // ByteBuffer alloc
     *
     * AFTER (1 GC object per call):
     *   ByteBuffer buf = ByteBuffer.allocate(exact)  // Only this
     *
     * @param client      The NIO channel to write to.
     * @param headerPrefix Pre-compiled header up to and including "Content-Length: ".
     * @param body        The response body bytes.
     */
    private void sendResponseRaw(AsynchronousSocketChannel client, byte[] headerPrefix, byte[] body) {
        // Encode body.length as ASCII digits using a per-thread scratch buffer.
        // ThreadLocal avoids the data race that a static byte[] would introduce
        // under concurrent connections on the async channel group's thread pool.
        byte[] clScratch = CL_SCRATCH_TL.get();
        int len = body.length;
        int digits = 0;
        if (len == 0) {
            clScratch[0] = '0';
            digits = 1;
        } else {
            int tmp = len;
            while (tmp > 0) { tmp /= 10; digits++; }
            int rem = len;
            for (int i = digits - 1; i >= 0; i--) {
                clScratch[i] = (byte) ('0' + rem % 10);
                rem /= 10;
            }
        }

        int totalSize = headerPrefix.length + digits + CRLF_CRLF.length + body.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(headerPrefix);
        buffer.put(clScratch, 0, digits);
        buffer.put(CRLF_CRLF);
        if (body.length > 0) buffer.put(body);
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
