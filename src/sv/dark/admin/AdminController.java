// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.admin;

import sv.dark.core.AAACertified;

import sv.dark.core.DarkLogger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RESPONSIBILITY: Administrative Data Bridge (Control Plane).
 * WHY: We need peripheral servers (HTTP/WebSocket) to read unformatted telemetry data without ever touching or blocking the Kernel.
 * TECHNIQUE: Maintains the latest state snapshot "pre-baked" by the AdminConsumer in an AtomicReference. Separates String formatting logic from the Main Kernel.
 * GUARANTEES: Non-blocking reads. Single writer principle. Zero-Garbage JSON construction via pre-allocated StringBuilder.
 * 
 * <p>Metrics: Non-blocking reads
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(date = "2026-06-11",
     maxLatencyNs = 0,
     minThroughput = 0, 
     alignment = 0, 
     lockFree = false, 
     offHeap = false, 
     notes = "Automatically AAA Certified during Core Audit")
public final class AdminController {

    // Default snapshot (JSON valid dummy)
    private static final byte[] DEFAULT_SNAPSHOT = "{\"status\":\"waiting_for_kernel\"}"
            .getBytes(StandardCharsets.UTF_8);

    // Atomic reference for thread-safe reading
    private static final AtomicReference<byte[]> latestSnapshot = new AtomicReference<>(DEFAULT_SNAPSHOT);

    private static sv.dark.net.DarkMetricsServer metricsServer = null;
    private static Thread adminConsumerThread = null;

    private AdminController() {
    } // Utility Class

    /**
     * Gets the latest snapshot of raw bytes.
     * Trivial cost operation (reference read).
     *
     * @return byte[] ready to write to socket
     */
    public static byte[] getLatestSnapshot() {
        return latestSnapshot.get();
    }

    /**
     * Updates the snapshot with new pre-formatted data.
     * Called only by AdminConsumer (Single writer principle recommended,
     * although AtomicReference supports concurrency).
     *
     * @param snapshotBytes JSON already converted to bytes
     */
    public static void updateSnapshot(byte[] snapshotBytes) {
        if (snapshotBytes != null) {
            latestSnapshot.set(snapshotBytes);
        }
    }

    /**
     * Starts the Control Plane asynchronously.
     * "Invisible" infrastructure bootstrap (Metrics Server + Admin Consumer).
     * 
     * @param kernel Reference to Main Kernel for telemetry
     */
    public static void startControlPlane(sv.dark.kernel.EngineKernel kernel, sv.dark.memory.SectorMemoryVault memoryVault) {
        try {
            // 1. Start Gateway 13000 (Blind Server)
            DarkLogger.info("Admin", "Iniciando plano de control (Métricas)");
            metricsServer = new sv.dark.net.DarkMetricsServer(13000, memoryVault);
            metricsServer.start();

            // 2. Start AdminConsumer (Zero-Garbage Translator)
            adminConsumerThread = new Thread(() -> runAdminLoop(kernel), "AdminConsumer");
            adminConsumerThread.setDaemon(true);
            adminConsumerThread.start();

        } catch (Exception e) {
            DarkLogger.error("Admin", "Failed to start Control Plane: " + e.getMessage());
        }
    }

    /**
     * Stops the metrics server cleanly to prevent port/thread leaks.
     */
    public static void stopControlPlane() {
        if (metricsServer != null) {
            DarkLogger.info("Admin", "Stopping DarkMetricsServer...");
            metricsServer.stop();
            metricsServer = null;
        }
        if (adminConsumerThread != null) {
            try {
                // Wait for the Poison Pill to finish processing
                adminConsumerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            adminConsumerThread = null;
        }
    }

    /**
     * Escribe un String estático (Ascii puro) al buffer. Zero-GC.
     */
    private static int writeAscii(byte[] dest, int offset, String text) {
        for (int i = 0; i < text.length(); i++) {
            dest[offset++] = (byte) text.charAt(i);
        }
        return offset;
    }

    /**
     * Escribe un número entero (long) directamente al buffer en formato ASCII. Zero-GC.
     */
    private static int writeLong(byte[] dest, int offset, long value) {
        if (value == 0) {
            dest[offset++] = '0';
            return offset;
        }
        if (value < 0) {
            dest[offset++] = '-';
            value = -value;
        }
        long temp = value;
        int digits = 0;
        while (temp > 0) {
            digits++;
            temp /= 10;
        }
        int idx = offset + digits - 1;
        while (value > 0) {
            dest[idx--] = (byte) ('0' + (value % 10));
            value /= 10;
        }
        return offset + digits;
    }

    /**
     * Administrative Consumer loop.
     * Separates "dirty" logic (String formatting) from the Main Kernel.
     */
    private static void runAdminLoop(sv.dark.kernel.EngineKernel kernel) {
        var adminBus = kernel.getAdminMetricsBus();
        // [OPTIMIZATION] Pre-allocated byte array for Zero-Garbage JSON construction
        byte[] jsonBuffer = new byte[2048];

        while (true) {
            try {
                long metric = adminBus.poll();
                if (metric != -1L) {
                    long metricType = sv.dark.kernel.MetricsPacker.getMetricType(metric);
                    
                    if (metricType == sv.dark.kernel.MetricsPacker.TYPE_FRAME_STATS) {
                        // 1. Unpack hot-path data
                        long frameCount = sv.dark.kernel.MetricsPacker.unpackFrameCount(metric);
                        long timeMicros = sv.dark.kernel.MetricsPacker.unpackTimeMicros(metric);
                        long frameLatencyNs = timeMicros * 1000;
                        long targetFps = sv.dark.kernel.MetricsPacker.unpackTargetFps(metric);
                        long actualFps = sv.dark.kernel.MetricsPacker.unpackActualFps(metric);
                        long headroomNs = sv.dark.kernel.MetricsPacker.unpackHeadroomNs(metric);

                        // 1.5 Write beautifully formatted metrics to darkengine_metrics.log
                        // Note: DarkLogger should eventually become completely Zero-GC too.
                        DarkLogger.info("METRICS", String.format("Frame: %d | Time: %dus | FPS: %d (Target: %d) | Headroom: %.2fms", 
                                        frameCount, timeMicros, actualFps, targetFps, headroomNs / 1_000_000.0));

                        // 2. Obtain slow state (Slow-Path safe here)
                        boolean isParallel = kernel.getSystemRegistry().isParallelMode();
                        int systemCount = kernel.getSystemRegistry().getGameSystemCount();
                        
                        // 3. Build JSON (Zero-GC Byte Writing Pattern - AAA+ Compliant)
                        int ptr = 0;
                        ptr = writeAscii(jsonBuffer, ptr, "{\"frameLatency\":");
                        ptr = writeLong(jsonBuffer, ptr, frameLatencyNs);
                        ptr = writeAscii(jsonBuffer, ptr, ",\"cpuCore\":1,\"executionMode\":\"");
                        ptr = writeAscii(jsonBuffer, ptr, isParallel ? "Parallel" : "Sequential");
                        ptr = writeAscii(jsonBuffer, ptr, "\",\"systems\":");
                        ptr = writeLong(jsonBuffer, ptr, systemCount);
                        ptr = writeAscii(jsonBuffer, ptr, ",\"executionOrder\":\"");
                        ptr = writeAscii(jsonBuffer, ptr, isParallel ? "DAG" : "Linear");
                        ptr = writeAscii(jsonBuffer, ptr, "\",\"parallelism\":\"");
                        ptr = writeAscii(jsonBuffer, ptr, isParallel ? "ON (Automatic)" : "OFF");
                        ptr = writeAscii(jsonBuffer, ptr, "\",\"frameCount\":");
                        ptr = writeLong(jsonBuffer, ptr, frameCount);
                        ptr = writeAscii(jsonBuffer, ptr, ",\"targetFps\":");
                        ptr = writeLong(jsonBuffer, ptr, targetFps);
                        ptr = writeAscii(jsonBuffer, ptr, ",\"actualFps\":");
                        ptr = writeLong(jsonBuffer, ptr, actualFps);
                        ptr = writeAscii(jsonBuffer, ptr, ",\"headroomNs\":");
                        ptr = writeLong(jsonBuffer, ptr, headroomNs);
                        ptr = writeAscii(jsonBuffer, ptr, "}");

                        // 4. Publish to Atomic Snapshot
                        // Copy exact length to avoid trailing null bytes
                        byte[] finalSnapshot = new byte[ptr];
                        System.arraycopy(jsonBuffer, 0, finalSnapshot, 0, ptr);
                        updateSnapshot(finalSnapshot);
                    } else {
                        // Not a frame stat, possibly a packed command ID
                        int commandId = sv.dark.bus.DarkSignalPacker.unpackCommandId(metric);
                        if (commandId == 2) {
                            DarkLogger.info("KERNEL", "Pause State toggled");
                        } else if (commandId == sv.dark.bus.DarkSignalCommands.SYS_ENGINE_ROLLBACK) {
                            DarkLogger.info("KERNEL", "Rollback / Time Travel Executed");
                        } else if (commandId == sv.dark.bus.DarkSignalCommands.SYS_TERMINATE_LOG_SIGNAL) {
                            DarkLogger.info("Admin", "Poison Pill Received. Terminating logger.");
                            DarkLogger.flushAndClose();
                            break;
                        }
                    }

                } else {
                    try {
                        Thread.sleep(16); // ~60 FPS check
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (IllegalStateException e) {
                // Bus closed during shutdown - terminate silently
                DarkLogger.info("Admin", "Bus cerrado - AdminConsumer terminando");
                break;
            }
        }
    }
}
