package sv.volcan.test;

import sv.volcan.bus.VolcanAtomicBus;
import sv.volcan.core.AAACertified;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Benchmark AAA+ para validar throughput >10M ops/s
 * DEPENDENCIAS: VolcanAtomicBus
 * MÉTRICAS: Throughput >10M ops/s, Latencia <150ns
 * 
 * Benchmark para certificación AAA+ del bus de eventos.
 * Valida que el throughput supere 10M operaciones/segundo.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-08
 */
@AAACertified(date = "2026-01-08", maxLatencyNs = 150, minThroughput = 10_000_000, alignment = 64, lockFree = true, offHeap = false, notes = "Benchmark AAA+ - Validación throughput >10M ops/s")
public class Test_BusBenchmark {

    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int BENCHMARK_ITERATIONS = 10_000_000;
    private static final int CAPACITY = 16384; // 2^14

    /**
     * Benchmark de throughput - Operaciones offer().
     * 
     * OBJETIVO: >10M ops/s
     * MÉTODO: Warm-up + medición precisa con TSC
     */
    public static void benchmarkOffer() {
        VolcanAtomicBus bus = new VolcanAtomicBus(CAPACITY);

        // ═══════════════════════════════════════════════════════════
        // WARM-UP: Calentar JIT compiler
        // ═══════════════════════════════════════════════════════════
        System.out.println("[BENCHMARK] Warming up JIT compiler...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            long signal = packSignal(i, 100, 200);
            bus.offer(signal);
        }

        // Limpiar bus
        while (bus.poll() != -1) {
            // Vaciar
        }

        // ═══════════════════════════════════════════════════════════
        // BENCHMARK: Medición real
        // ═══════════════════════════════════════════════════════════
        System.out.println("[BENCHMARK] Starting throughput test...");
        System.out.println("[BENCHMARK] Iterations: " + BENCHMARK_ITERATIONS);

        long startNs = System.nanoTime();

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long signal = packSignal(i, 100, 200);
            bus.offer(signal);
        }

        long endNs = System.nanoTime();
        long totalNs = endNs - startNs;

        // ═══════════════════════════════════════════════════════════
        // RESULTADOS
        // ═══════════════════════════════════════════════════════════
        double totalSeconds = totalNs / 1_000_000_000.0;
        double opsPerSecond = BENCHMARK_ITERATIONS / totalSeconds;
        double avgLatencyNs = (double) totalNs / BENCHMARK_ITERATIONS;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK RESULTS - offer()");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Total time:        " + String.format("%.3f", totalSeconds) + " seconds");
        System.out.println("Throughput:        " + String.format("%.2f", opsPerSecond / 1_000_000) + " M ops/s");
        System.out.println("Avg latency:       " + String.format("%.2f", avgLatencyNs) + " ns");
        System.out.println("Target throughput: 10.00 M ops/s");
        System.out.println("Target latency:    150.00 ns");

        // Validación AAA+
        boolean throughputPass = opsPerSecond >= 10_000_000;
        boolean latencyPass = avgLatencyNs <= 150;

        System.out.println("\n--- AAA+ CERTIFICATION ---");
        System.out.println("Throughput: " + (throughputPass ? "✅ PASS" : "❌ FAIL"));
        System.out.println("Latency:    " + (latencyPass ? "✅ PASS" : "❌ FAIL"));
        System.out.println("═══════════════════════════════════════════════════════\n");
    }

    /**
     * Benchmark de throughput - Operaciones poll().
     * 
     * OBJETIVO: >10M ops/s
     */
    public static void benchmarkPoll() {
        VolcanAtomicBus bus = new VolcanAtomicBus(CAPACITY);

        // Llenar bus
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long signal = packSignal(i, 100, 200);
            bus.offer(signal);
        }

        // ═══════════════════════════════════════════════════════════
        // BENCHMARK: poll()
        // ═══════════════════════════════════════════════════════════
        System.out.println("[BENCHMARK] Starting poll() test...");

        long startNs = System.nanoTime();

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long signal = bus.poll();
            if (signal == -1)
                break; // Bus vacío
        }

        long endNs = System.nanoTime();
        long totalNs = endNs - startNs;

        // ═══════════════════════════════════════════════════════════
        // RESULTADOS
        // ═══════════════════════════════════════════════════════════
        double totalSeconds = totalNs / 1_000_000_000.0;
        double opsPerSecond = BENCHMARK_ITERATIONS / totalSeconds;
        double avgLatencyNs = (double) totalNs / BENCHMARK_ITERATIONS;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK RESULTS - poll()");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Total time:        " + String.format("%.3f", totalSeconds) + " seconds");
        System.out.println("Throughput:        " + String.format("%.2f", opsPerSecond / 1_000_000) + " M ops/s");
        System.out.println("Avg latency:       " + String.format("%.2f", avgLatencyNs) + " ns");

        boolean throughputPass = opsPerSecond >= 10_000_000;
        boolean latencyPass = avgLatencyNs <= 150;

        System.out.println("\n--- AAA+ CERTIFICATION ---");
        System.out.println("Throughput: " + (throughputPass ? "✅ PASS" : "❌ FAIL"));
        System.out.println("Latency:    " + (latencyPass ? "✅ PASS" : "❌ FAIL"));
        System.out.println("═══════════════════════════════════════════════════════\n");
    }

    /**
     * Benchmark combinado - offer() + poll().
     * 
     * OBJETIVO: >10M ops/s en ciclo completo
     */
    public static void benchmarkRoundTrip() {
        VolcanAtomicBus bus = new VolcanAtomicBus(CAPACITY);

        System.out.println("[BENCHMARK] Starting round-trip test...");

        long startNs = System.nanoTime();

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long signal = packSignal(i, 100, 200);
            bus.offer(signal);
            long retrieved = bus.poll();

            // Validación básica
            if (retrieved == -1) {
                System.err.println("[ERROR] Bus returned -1 at iteration " + i);
                break;
            }
        }

        long endNs = System.nanoTime();
        long totalNs = endNs - startNs;

        // ═══════════════════════════════════════════════════════════
        // RESULTADOS
        // ═══════════════════════════════════════════════════════════
        double totalSeconds = totalNs / 1_000_000_000.0;
        double opsPerSecond = (BENCHMARK_ITERATIONS * 2) / totalSeconds; // offer + poll
        double avgLatencyNs = (double) totalNs / (BENCHMARK_ITERATIONS * 2);

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK RESULTS - Round-Trip (offer + poll)");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Total time:        " + String.format("%.3f", totalSeconds) + " seconds");
        System.out.println("Throughput:        " + String.format("%.2f", opsPerSecond / 1_000_000) + " M ops/s");
        System.out.println("Avg latency:       " + String.format("%.2f", avgLatencyNs) + " ns");

        boolean throughputPass = opsPerSecond >= 10_000_000;
        boolean latencyPass = avgLatencyNs <= 150;

        System.out.println("\n--- AAA+ CERTIFICATION ---");
        System.out.println("Throughput: " + (throughputPass ? "✅ PASS" : "❌ FAIL"));
        System.out.println("Latency:    " + (latencyPass ? "✅ PASS" : "❌ FAIL"));
        System.out.println("═══════════════════════════════════════════════════════\n");
    }

    /**
     * Empaqueta un signal para testing.
     * 
     * @param id ID del evento
     * @param x  Coordenada X
     * @param y  Coordenada Y
     * @return Signal empaquetado
     */
    private static long packSignal(int id, int x, int y) {
        // Formato simple: [id:32][x:16][y:16]
        return ((long) id << 32) | ((long) x << 16) | (long) y;
    }

    /**
     * Main - Ejecuta todos los benchmarks.
     */
    public static void main(String[] args) {
        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║  VOLCAN ENGINE - AAA+ BENCHMARK SUITE                ║");
        System.out.println("║  Target: >10M ops/s, <150ns latency                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");

        // Benchmark 1: offer()
        benchmarkOffer();

        // Pausa entre benchmarks
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Benchmark 2: poll()
        benchmarkPoll();

        // Pausa entre benchmarks
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Benchmark 3: Round-trip
        benchmarkRoundTrip();

        System.out.println("\n[BENCHMARK] All tests completed.");
        System.out.println("[BENCHMARK] Review results above for AAA+ certification.\n");
    }
}
