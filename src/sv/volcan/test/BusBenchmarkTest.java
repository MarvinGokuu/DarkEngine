package sv.volcan.test;

import sv.volcan.bus.VolcanAtomicBus;

/**
 * AAA+ Certification Benchmark
 * Target: > 10,000,000 ops/sec
 * Latency: < 150 ns/op
 * Allocation: 0 bytes/op (Hot-Path)
 */
public class BusBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 5_000_000;
    private static final int MEASURE_ITERATIONS = 20_000_000;
    private static final int BATCH_SIZE = 1000;

    // 8192 (2^13) capacity
    private static final VolcanAtomicBus bus = new VolcanAtomicBus(13);

    // Pre-allocated buffers for Zero-Allocation in Hot-Path
    private static final long[] batchInput = new long[BATCH_SIZE];
    private static final long[] batchOutput = new long[BATCH_SIZE];

    public static void main(String[] args) {
        System.out.println("=== AAA+ VolcanEngine Benchmark ===");

        // Initialize batch data
        for (int i = 0; i < BATCH_SIZE; i++)
            batchInput[i] = 0xCAFEBABE;

        warmup();
        double latency = runLatencyTest();
        long throughput = runThroughputTest();

        // Validar certificación AAA+ (intención original del benchmark)
        boolean certified = (latency < 150.0 && throughput > 10_000_000);

        System.out.println("=== Certification Complete ===");
        System.out.println("AAA+ Status: " + (certified ? "CERTIFIED ✓" : "NOT CERTIFIED ✗"));

        // Exit code para CI/CD
        System.exit(certified ? 0 : 1);
    }

    private static void warmup() {
        System.out.print("JIT Warmup (" + WARMUP_ITERATIONS + " ops)... ");
        long start = System.nanoTime();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            bus.offer(0xCAFEBABE);
            bus.poll();
        }
        long end = System.nanoTime();
        System.out.println("Done in " + ((end - start) / 1_000_000) + " ms");
    }

    private static double runLatencyTest() {
        System.out.println("\n[Phase 1] Atomic Latency Test (offer/poll)");
        long totalTime = 0;

        // Ensure bus is empty
        bus.clear();

        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long t0 = System.nanoTime();
            bus.offer(0x1);
            long t1 = System.nanoTime();

            // Measure only offer latency as per spec (<150ns atomic write)
            totalTime += (t1 - t0);

            bus.poll(); // Drain to keep buffer usable
        }

        double avgLatency = (double) totalTime / MEASURE_ITERATIONS;
        System.out.printf("  > Average Latency: %.2f ns/op\n", avgLatency);

        if (avgLatency < 150.0) {
            System.out.println("  > Status: PASSED (AAA+ Certified)");
        } else {
            System.out.println("  > Status: FAILED (Optimization Required)");
        }

        return avgLatency;
    }

    private static long runThroughputTest() {
        System.out.println("\n[Phase 2] High-Velocity Throughput Test (Batch)");

        // Ensure bus is empty
        bus.clear();

        long start = System.nanoTime();
        int totalProcessed = 0;

        // Process in batches
        int loops = MEASURE_ITERATIONS / BATCH_SIZE;
        for (int i = 0; i < loops; i++) {
            // Write batch
            int written = bus.batchOffer(batchInput, 0, BATCH_SIZE);

            // Validate transaction integrity (AAA+ Determinism)
            if (written != BATCH_SIZE) {
                throw new RuntimeException("Bus Saturation: Expected " + BATCH_SIZE + " but wrote " + written);
            }

            // Read batch
            int read = bus.batchPoll(batchOutput, BATCH_SIZE);

            totalProcessed += read;
        }

        long end = System.nanoTime();
        long durationNs = end - start;
        double durationSec = durationNs / 1_000_000_000.0;
        long opsPerSec = (long) (totalProcessed / durationSec);

        System.out.printf("  > Throughput: %,d ops/sec\n", opsPerSec);

        if (opsPerSec > 10_000_000) {
            System.out.println("  > Status: PASSED (AAA+ Certified)");
        } else {
            System.out.println("  > Status: FAILED");
        }

        return opsPerSec;
    }
}
