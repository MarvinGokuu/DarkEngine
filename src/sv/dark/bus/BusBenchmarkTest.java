// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.bus;

import sv.dark.core.AAACertified;

import java.util.Arrays;

/**
 * @author Marvin Alexander Flores Canales
 * RESPONSABILIDAD: Certificacion de Rendimiento AAA+ (Benchmark Suite).
 * DEPENDENCIAS: DarkAtomicBus, DarkRingBus
 * METRICAS: Latency < 150ns, Throughput > 10M ops/sec
 * 
 * Suite de pruebas de estres para validar el cumplimiento de los estandares
 * AAA+.
 * Mide latencia de operaciones atomicas y throughput de procesamiento en batch.
 * 
 * @author Marvin Alexander Flores Canales
 * @version 1.0
 * @since 2026-01-05
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(
    date = "2026-06-11",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Automatically AAA Certified during Core Audit"
)
public class BusBenchmarkTest {

    private static final int ITERATIONS = 10_000_000;
    private static final int WARMUP = 100_000;
    private static final int BATCH_SIZE = 1024;

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  DARK ENGINE - AAA+ BUS BENCHMARK & THROUGHPUT TEST");
        System.out.println("  Target Latency: < 150ns  |  Target Throughput: > 10M/s");
        System.out.println("==========================================================");

        runLatencyTest();
        runThroughputTest();
    }

    private static void runLatencyTest() {
        System.out.println("\n[PHASE 1] LATENCY TEST (Atomic Operations)");
        // 4096 elements -> 2^12
        DarkAtomicBus bus = new DarkAtomicBus(12);

        // Warmup
        System.out.print("Warmup... ");
        long[] samples = new long[ITERATIONS];
        for (int i= 0; i< WARMUP; i++) {
            bus.offer(100 + i);
            bus.poll();
        }
        System.out.println("Done.");

        // Measurement
        System.out.print("Measuring " + ITERATIONS + " ops... ");
        long totalNs = 0;

        for (int i= 0; i< ITERATIONS; i++) {
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

        if (p99 <= 150) {
            System.out.println("    [OK] AAA+ Certified (<150ns)");
        } else {
            System.out.println("    [WARNING] Latency optimization required");
        }
    }

    private static void runThroughputTest() {
        System.out.println("\n[PHASE 2] THROUGHPUT TEST (Batch Processing)");
        // 65536 elements -> 2^16
        DarkAtomicBus bus = new DarkAtomicBus(16); // Large buffer for batch

        long startTime = System.nanoTime();
        int ops = 0;

        // Simulating producer burst
        for (int i= 0; i< ITERATIONS / BATCH_SIZE; i++) {
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
        if (throughput >= 10_000_000) {
            System.out.println("    [OK] AAA+ Certified (>10M/s)");
        } else {
            System.out.println("    [WARNING] Throughput optimization required");
        }
    }
}
