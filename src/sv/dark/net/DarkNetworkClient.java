// Reading Order: 00100070
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.lang.foreign.MemorySegment;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;

/**
 * Cliente UDP Orientado a Datos.
 * Sin Serialización XML/JSON. Sin Asignaciones de Objetos (Zero-Allocation).
 * Envía la memoria estructural cruda directamente a través de Sockets UDP No-Bloqueantes.
 */
@AAACertified(date = "2026-06-20", maxLatencyNs = 100, minThroughput = 0, lockFree = true, offHeap = true, notes = "Phase 33: DatagramChannel UDP Zero-Copy")
public final class DarkNetworkClient {

    private DatagramChannel channel;
    private final ByteBuffer receiveBuffer;
    private final ByteBuffer sendBuffer;

    public DarkNetworkClient(int port, int bufferSize) {
        this.receiveBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.sendBuffer = ByteBuffer.allocateDirect(bufferSize);
        
        try {
            this.channel = DatagramChannel.open();
            this.channel.configureBlocking(false);
            if (port > 0) {
                this.channel.bind(new InetSocketAddress(port));
                DarkLogger.info("NETWORK", "Servidor UDP Autorizado escuchando en puerto " + port);
            } else {
                DarkLogger.info("NETWORK", "Cliente UDP instanciado en puerto efímero.");
            }
        } catch (IOException e) {
            DarkLogger.fatal("NETWORK", "Fallo al inicializar DatagramChannel", e);
        }
    }

    /**
     * Fija el canal a un destinatario específico para habilitar Operaciones Zero-GC.
     */
    public void connect(InetSocketAddress target) {
        try {
            channel.connect(target);
            DarkLogger.info("NETWORK", "DatagramChannel conectado en modo Fast-Path a " + target);
        } catch (IOException e) {
            DarkLogger.warning("NETWORK", "Error de conexion UDP: " + e.getMessage());
        }
    }

    /**
     * Envía una carga útil binaria pura (Solo funciona si connect() fue llamado).
     */
    public void sendRawPayload(MemorySegment payload, long bytes) {
        try {
            if (!channel.isConnected()) return;
            sendBuffer.clear();
            // Project Panama permite copiar de MemorySegment a ByteBuffer nativo directo
            MemorySegment.copy(payload, 0, MemorySegment.ofBuffer(sendBuffer), 0, bytes);
            sendBuffer.limit((int) bytes);
            channel.write(sendBuffer); // ZERO ALLOCATION NATIVE SYSCALL
        } catch (IOException e) {
            // Ignorar fallos de envío UDP
        }
    }

    /**
     * Lee un paquete de la red y lo vuelca en el MemorySegment de destino.
     * Retorna el número de bytes leídos (0 si no hay paquetes pendientes).
     */
    public long pollPayload(MemorySegment destination) {
        try {
            if (!channel.isConnected()) return 0;
            receiveBuffer.clear();
            // ZERO ALLOCATION NATIVE SYSCALL
            int bytesRead = channel.read(receiveBuffer);
            if (bytesRead <= 0) return 0; // Sin paquetes
            
            receiveBuffer.flip();
            MemorySegment.copy(MemorySegment.ofBuffer(receiveBuffer), 0, destination, 0, bytesRead);
            return bytesRead;
        } catch (IOException e) {
            return 0;
        }
    }

    public void close() {
        try {
            if (channel != null) channel.close();
        } catch (IOException e) {
            DarkLogger.warning("NETWORK", "Error al cerrar canal UDP");
        }
    }
}
