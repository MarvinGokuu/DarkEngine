package sv.volcan.admin;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Puente de Datos Administrativos (Control Plane)
 * DEPENDENCIAS: Ninguna (DTO Holder)
 * MÉTRICAS: Non-blocking reads
 *
 * Mantiene el último snapshot de estado "pre-horneado" (Pre-baked) por el
 * AdminConsumer.
 * Permite que los servidores periféricos (HTTP/WebSocket) lean datos sin
 * formatear
 * y sin tocar el Kernel.
 *
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-08
 */
public final class AdminController {

    // Snapshot por defecto (JSON valid dummy)
    private static final byte[] DEFAULT_SNAPSHOT = "{\"status\":\"waiting_for_kernel\"}"
            .getBytes(StandardCharsets.UTF_8);

    // Referencia atómica para lectura segura en multithread
    private static final AtomicReference<byte[]> latestSnapshot = new AtomicReference<>(DEFAULT_SNAPSHOT);

    private AdminController() {
    } // Utility Class

    /**
     * Obtiene el último snapshot de bytes crudos.
     * Operación de costo trivial (lectura de referencia).
     *
     * @return byte[] listo para escribir en el socket
     */
    public static byte[] getLatestSnapshot() {
        return latestSnapshot.get();
    }

    /**
     * Actualiza el snapshot con nuevos datos pre-formateados.
     * Llamado solo por el AdminConsumer (Single writer principle recomendado,
     * aunque AtomicReference soporta concurrencia).
     *
     * @param snapshotBytes JSON ya convertido a bytes
     */
    public static void updateSnapshot(byte[] snapshotBytes) {
        if (snapshotBytes != null) {
            latestSnapshot.set(snapshotBytes);
        }
    }

    /**
     * Inicia el Plano de Control (Control Plane) de manera asíncrona.
     * Bootstrap de infraestructura "Invisible" (Metrics Server + Admin Consumer).
     * 
     * @param kernel Referencia al Main Kernel para telemetría
     */
    public static void startControlPlane(sv.volcan.kernel.EngineKernel kernel) {
        try {
            // 1. Iniciar Gateway 8080 (Blind Server)
            System.out.println("[ADMIN] Starting VolcanMetricsServer (Blind Gateway)...");
            sv.volcan.net.VolcanMetricsServer metricsServer = new sv.volcan.net.VolcanMetricsServer(8080);
            metricsServer.start();

            // 2. Iniciar AdminConsumer (Traductor Zero-Garbage)
            Thread adminConsumer = new Thread(() -> runAdminLoop(kernel), "AdminConsumer");
            adminConsumer.setDaemon(true);
            adminConsumer.start();

        } catch (java.io.IOException e) {
            System.err.println("[ADMIN PANIC] Failed to start Control Plane: " + e.getMessage());
        }
    }

    /**
     * Loop del Consumidor Administrativo.
     * Separa la lógica "sucia" (String formatting) del Main Kernel.
     */
    private static void runAdminLoop(sv.volcan.kernel.EngineKernel kernel) {
        var adminBus = kernel.getAdminMetricsBus();
        // [OPTIMIZATION] Pre-allocated StringBuilder for Zero-Garbage JSON construction
        // Usamos capacidad 2048 para evitar resize con futuros campos
        StringBuilder jsonBuilder = new StringBuilder(2048);

        while (true) {
            try {
                long metric = adminBus.poll();
                if (metric != 0) {
                    // 1. Desempaquetar datos del Hot-Path
                    long frameCount = sv.volcan.kernel.MetricsPacker.unpackFrameCount(metric);
                    long timeMicros = sv.volcan.kernel.MetricsPacker.unpackTimeMicros(metric);
                    long frameLatencyNs = timeMicros * 1000;

                    // 2. Obtener estado lento (Slow-Path safe here)
                    boolean isParallel = kernel.getSystemRegistry().isParallelMode();
                    int systemCount = kernel.getSystemRegistry().getGameSystemCount();
                    String executionMode = isParallel ? "Parallel" : "Sequential";
                    String executionOrder = isParallel ? "DAG" : "Linear";

                    // 3. Construir JSON (Builder Pattern - AAA+ Compliant)
                    jsonBuilder.setLength(0);
                    jsonBuilder.append("{")
                            .append("\"frameLatency\":").append(frameLatencyNs).append(",")
                            .append("\"cpuCore\":1,")
                            .append("\"executionMode\":\"").append(executionMode).append("\",")
                            .append("\"systems\":").append(systemCount).append(",")
                            .append("\"executionOrder\":\"").append(executionOrder).append("\",")
                            .append("\"parallelism\":\"").append(isParallel ? "ON (Automatic)" : "OFF").append("\",")
                            .append("\"frameCount\":").append(frameCount)
                            .append("}");

                    // 4. Publicar al Snapshot Atómico
                    updateSnapshot(jsonBuilder.toString().getBytes(StandardCharsets.UTF_8));

                } else {
                    try {
                        Thread.sleep(16); // ~60 FPS check
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (IllegalStateException e) {
                // Bus cerrado durante shutdown - terminar silenciosamente
                System.out.println("[ADMIN] Bus cerrado - AdminConsumer terminando");
                break;
            }
        }
    }
}
