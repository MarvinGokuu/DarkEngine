package sv.volcan.bus;

import java.util.Arrays;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Certificación de Rendimiento AAA+ (Benchmark Suite).
 * DEPENDENCIAS: VolcanAtomicBus, VolcanRingBus
 * MÉTRICAS: Latency < 150ns, Throughput > 10M ops/sec
 * 
 * Suite de pruebas de estrés para validar el cumplimiento de los estándares
 * AAA+.
 * Mide latencia de operaciones atómicas y throughput de procesamiento en batch.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public class BusBenchmarkTest {

    private static final int ITERATIONS = 10_000_000;
    private static final int WARMUP = 100_000;
    private static final int BATCH_SIZE = 1024;

    public static void main(String[] args) {
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("   VOLCAN ENGINE AAA+ BENCHMARK SUITE");
        System.out.println("   Target: Latency < 150ns | Throughput > 10M/s");
        System.out.println("════════════════════════════════════════════════════════");

        runLatencyTest();
        runThroughputTest();
    }

    private static void runLatencyTest() {
        System.out.println("\n[PHASE 1] LATENCY TEST (Atomic Operations)");
        // 4096 elements -> 2^12
        VolcanAtomicBus bus = new VolcanAtomicBus(12);

        // Warmup
        System.out.print("Warmup... ");
        long[] samples = new long[ITERATIONS];
        for (int i = 0; i < WARMUP; i++) {
            bus.offer(100 + i);
            bus.poll();
        }
        System.out.println("Done.");

        // Measurement
        System.out.print("Measuring " + ITERATIONS + " ops... ");
        long totalNs = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            long t0 = System.nanoTime();
            bus.offer(i);
            long t1 = System.nanoTime();
            bus.poll(); // Clear for next op

            long latency = t1 - t0;
            samples[i] = latency;
            totalNs += latency;
        }
        System.out.println("Done.");

        // Analysis
        Arrays.sort(samples);
        long p50 = samples[ITERATIONS / 2];
        long p95 = samples[(int) (ITERATIONS * 0.95)];
        long p99 = samples[(int) (ITERATIONS * 0.99)];
        double avg = (double) totalNs / ITERATIONS;

        System.out.println("    -> Average Latency: " + String.format("%.2f", avg) + " ns");
        System.out.println("    -> P50: " + p50 + " ns");
        System.out.println("    -> P95: " + p95 + " ns");
        System.out.println("    -> P99: " + p99 + " ns");

        if (p99 < 150) {
            System.out.println("    [✅ PASS] AAA+ Certified (<150ns)");
        } else {
            System.out.println("    [⚠️ WARNING] Latency optimization required");
        }
    }

    private static void runThroughputTest() {
        System.out.println("\n[PHASE 2] THROUGHPUT TEST (Batch Processing)");
        // 65536 elements -> 2^16
        VolcanAtomicBus bus = new VolcanAtomicBus(16); // Large buffer for batch

        long startTime = System.nanoTime();
        int ops = 0;

        // Simulating producer burst
        for (int i = 0; i < ITERATIONS / BATCH_SIZE; i++) {
            for (int b = 0; b < BATCH_SIZE; b++) {
                if (!bus.offer(b))
                    break;
            }

            // Simulating consumer drain
            while (bus.poll() != -1) {
                ops++;
            }
        }

        long endTime = System.nanoTime();
        double seconds = (endTime - startTime) / 1_000_000_000.0;
        double throughput = (ops) / seconds;

        System.out.println("    -> Total Time: " + String.format("%.4f", seconds) + " s");
        System.out.println("    -> Total Ops: " + ops);
        System.out.println("    -> Throughput: " + String.format("%,.0f", throughput) + " ops/sec");

        long target = 10_000_000;
        if (throughput > target) {
            System.out.println("    [✅ PASS] AAA+ Certified (>10M/s)");
        } else {
            System.out.println("    [⚠️ WARNING] Throughput optimization required");
        }
    }
}
