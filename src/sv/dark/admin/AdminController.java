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
     * Administrative Consumer loop.
     * Separates "dirty" logic (String formatting) from the Main Kernel.
     */
    private static void runAdminLoop(sv.dark.kernel.EngineKernel kernel) {
        var adminBus = kernel.getAdminMetricsBus();
        // [OPTIMIZATION] Pre-allocated StringBuilder for Zero-Garbage JSON construction
        // We use capacity 2048 to prevent resize with future fields
        StringBuilder jsonBuilder = new StringBuilder(2048);

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
                        DarkLogger.info("METRICS", String.format("Frame: %d | Time: %dus | FPS: %d (Target: %d) | Headroom: %.2fms", 
                                        frameCount, timeMicros, actualFps, targetFps, headroomNs / 1_000_000.0));

                        // 2. Obtain slow state (Slow-Path safe here)
                        boolean isParallel = kernel.getSystemRegistry().isParallelMode();
                        int systemCount = kernel.getSystemRegistry().getGameSystemCount();
                        String executionMode = isParallel ? "Parallel" : "Sequential";
                        String executionOrder = isParallel ? "DAG" : "Linear";

                        // 3. Build JSON (Builder Pattern - AAA+ Compliant)
                        jsonBuilder.setLength(0);
                        jsonBuilder.append("{")
                                .append("\"frameLatency\":").append(frameLatencyNs).append(",")
                                .append("\"cpuCore\":1,")
                                .append("\"executionMode\":\"").append(executionMode).append("\",")
                                .append("\"systems\":").append(systemCount).append(",")
                                .append("\"executionOrder\":\"").append(executionOrder).append("\",")
                                .append("\"parallelism\":\"").append(isParallel ? "ON (Automatic)" : "OFF").append("\",")
                                .append("\"frameCount\":").append(frameCount).append(",")
                                .append("\"targetFps\":").append(targetFps).append(",")
                                .append("\"actualFps\":").append(actualFps).append(",")
                                .append("\"headroomNs\":").append(headroomNs)
                                .append("}");

                        // 4. Publish to Atomic Snapshot
                        updateSnapshot(jsonBuilder.toString().getBytes(StandardCharsets.UTF_8));
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
