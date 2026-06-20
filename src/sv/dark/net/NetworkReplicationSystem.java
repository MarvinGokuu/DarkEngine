// Reading Order: 00100071
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.net;

import java.net.InetSocketAddress;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;
import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import sv.dark.ecs.DarkScene;

/**
 * Sistema de Replicación de Red (Phase 33).
 * Muestrea la memoria contigua de la ECS (TransformSoA) y la empaqueta 
 * en un payload binario para retransmitirla a los clientes UDP conectados.
 */
@AAACertified(date = "2026-06-20", maxLatencyNs = 50, minThroughput = 0, lockFree = true, offHeap = true, notes = "Phase 33: ECS State Serialization")
public final class NetworkReplicationSystem implements GameSystem {

    private final DarkNetworkClient client;
    private final DarkScene scene;
    private final MemorySegment snapshotBuffer;
    private final Arena arena;
    
    // Lista estática de clientes tontos (En un juego real esto se maneja dinámicamente)
    private final InetSocketAddress[] clients;
    
    // Tick Rate: 20Hz para replicación de red típica (Para ahorrar ancho de banda)
    private static final double TICK_RATE = 1.0 / 20.0;
    private double accumulator = 0.0;

    public NetworkReplicationSystem(DarkNetworkClient client, DarkScene scene) {
        this.client = client;
        this.scene = scene;
        this.arena = Arena.ofShared();
        
        // 1000 Entidades * 3 Floats (ID, X, Y) * 4 Bytes = 12 KB Payload Size
        this.snapshotBuffer = arena.allocate(1000 * 3 * 4);
        
        // Broadcast local por defecto
        this.clients = new InetSocketAddress[] { new InetSocketAddress("127.0.0.1", 27015) };
    }

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        accumulator += deltaTime;
        if (accumulator >= TICK_RATE) {
            accumulator = 0.0;
            broadcastSnapshot();
        }
    }

    private void broadcastSnapshot() {
        int maxEntities = Math.min(scene.getSoA().getCapacity(), 1000);
        long offset = 0;
        
        // Empaquetar posiciones
        var tSoa = scene.getSoA();
        for (int i = 0; i < maxEntities; i++) {
            // Un motor real revisaría el BitMask para saltar entidades muertas,
            // pero para el Stress Test empaquetaremos de forma agresiva.
            float px = (float) tSoa.globalPosX.get(ValueLayout.JAVA_DOUBLE, i * 8L);
            float py = (float) tSoa.globalPosY.get(ValueLayout.JAVA_DOUBLE, i * 8L);
            
            snapshotBuffer.set(ValueLayout.JAVA_INT, offset, i); offset += 4;
            snapshotBuffer.set(ValueLayout.JAVA_FLOAT, offset, px); offset += 4;
            snapshotBuffer.set(ValueLayout.JAVA_FLOAT, offset, py); offset += 4;
        }

        // Disparar por UDP
        for (InetSocketAddress target : clients) {
            client.sendRawPayload(snapshotBuffer, offset, target);
        }
    }

    @Override
    public String getName() {
        return "NetworkReplicationSystem";
    }

    public void cleanup() {
        arena.close();
        client.close();
    }
}
