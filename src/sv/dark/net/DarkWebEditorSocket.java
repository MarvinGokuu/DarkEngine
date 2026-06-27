// Reading Order: 00010111
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
package sv.dark.net;

import sv.dark.memory.SectorMemoryVault;
import sv.dark.state.DarkStateLayout;
import sv.dark.core.DarkLogger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AAA+ Zero-Garbage WebSocket Server for Real-Time RAM Mapping.
 */
public final class DarkWebEditorSocket extends Thread {

    private final ServerSocket server;
    private final SectorMemoryVault vault;
    private volatile boolean running = true;

    public DarkWebEditorSocket(int port, SectorMemoryVault vault) throws Exception {
        this.server = new ServerSocket(port);
        this.vault = vault;
        this.setDaemon(true);
        this.setName("WebEditor-WSServer");
    }

    @Override
    public void run() {
        DarkLogger.info("WEB EDITOR", "WebSocket Server listening on port " + server.getLocalPort());
        while (running && !server.isClosed()) {
            try {
                Socket client = server.accept();
                client.setTcpNoDelay(true);
                handleClient(client);
            } catch (Exception e) {
                if (running) DarkLogger.error("WEB EDITOR", "Client error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket client) throws Exception {
        InputStream in = client.getInputStream();
        OutputStream out = client.getOutputStream();
        
        // Read Handshake
        byte[] requestBytes = new byte[1024];
        int bytesRead = in.read(requestBytes);
        String request = new String(requestBytes, 0, bytesRead);
        
        Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(request);
        if (match.find()) {
            String key = match.group(1).trim();
            String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(magic.getBytes());
            String accept = Base64.getEncoder().encodeToString(sha1);
            
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
            out.write(response.getBytes());
            
            // Listen for frames
            while (!client.isClosed()) {
                int head = in.read();
                if (head == -1) break;
                
                int lengthByte = in.read();
                boolean masked = (lengthByte & 128) != 0;
                int length = lengthByte & 127;
                
                if (length == 126) {
                    in.read(); in.read(); // skip 16-bit length
                } else if (length == 127) {
                    for(int i=0; i<8; i++) in.read(); // skip 64-bit length
                }
                
                byte[] mask = new byte[4];
                if (masked) in.read(mask);
                
                byte[] data = new byte[length];
                in.read(data);
                
                if (masked) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] ^= mask[i % 4];
                    }
                }
                
                String payload = new String(data);
                // Payload format: "COMMAND|arg1|arg2|..."
                String[] parts = payload.split("\\|");
                if (parts.length > 0 && vault != null) {
                    try {
                        String cmd = parts[0].toUpperCase();
                        switch (cmd) {
                            case "SET_POS":
                                if (parts.length >= 3) {
                                    vault.writeInt(DarkStateLayout.PLAYER_X, Integer.parseInt(parts[1]));
                                    vault.writeInt(DarkStateLayout.PLAYER_Y, Integer.parseInt(parts[2]));
                                }
                                break;
                            case "SPAWN_LIGHT":
                                if (parts.length >= 8) {
                                    sv.dark.scene.DarkLightSystem.addPointLight(
                                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]), // x,y,z
                                        Float.parseFloat(parts[4]), // radius
                                        Float.parseFloat(parts[5]), Float.parseFloat(parts[6]), Float.parseFloat(parts[7]), // r,g,b
                                        1.0f // intensity
                                    );
                                }
                                break;
                            case "HOT_RELOAD":
                                DarkLogger.info("WEB EDITOR", "Hot Reload Requested via WebSocket");
                                break;
                            default:
                                DarkLogger.warn("WEB EDITOR", "Unknown Command: " + cmd);
                                break;
                        }
                    } catch (Exception ignore) {
                        DarkLogger.error("WEB EDITOR", "Failed to parse command: " + payload);
                    }
                }
            }
        }
        client.close();
    }

    public void stopServer() {
        running = false;
        try { server.close(); } catch (Exception ignore) {}
    }
}
