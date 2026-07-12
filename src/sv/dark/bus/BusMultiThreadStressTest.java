// Reading Order: 01010010
//  82
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.bus;

import sv.dark.core.AAACertified;
import sv.dark.kernel.ThreadPinning;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RESPONSIBILITY: High-Performance Multi-threaded Cache-Stress Benchmark (Fase 9).
 * WHY: Multi-threaded contention must be validated physically by forcing inter-core cache line bouncing.
 * TECHNIQUE: Spawns independent threads for producers and consumers, pinning them to different physical cores
 * (e.g. Core 1, Core 2, Core 3) via Win32 FFI Thread Affinity to trigger L3 and inter-socket/NUMA cache invalidations.
 * GUARANTEES: Lock-free execution under actual hardware contention, measuring latency under multi-core stress.
 * 
 * @author System Architect / JVM Core Engineer
 * @version 1.0
 * @since 2026-06-12
 */
@AAACertified(
    date = "2026-06-12",
    maxLatencyNs = 150,
    minThroughput = 15_000_000,
    alignment = 64,
    lockFree = true,
    offHeap = false,
    notes = "Multi-threaded NUMA/L3 cache stress test using Thread Pinning"
)
public class BusMultiThreadStressTest {

    private static final int ITERATIONS = 10_000_000;
    private static final int WARMUP = 1_000_000;

    private static double spscSeconds;
    private static double spscThroughput;
    private static double spscLatency;
    private static boolean spscOk;

    private static double mpscSeconds;
    private static double mpscThroughput;
    private static double mpscLatency;
    private static boolean mpscOk;

    public static void main(String[] args) {
        System.out.print("[TEST] Running Multi-threaded Cache Contention Benchmark (Fase 9)... ");

        try {
            runSpscBench();
            runMpscBench();
            printSummary();
        } catch (InterruptedException e) {
            System.err.println("FAILED: " + e.getMessage());
        }
    }

    private static void printSummary() {
        System.out.println("DONE.");
        System.out.println("\n======================================================================");
        System.out.println("                  MULTI-THREADED BUS BENCHMARK SUMMARY                 ");
        System.out.println("======================================================================");
        
        System.out.printf(" %-25s | %-12s | %-15s | %-10s%n", "TEST TOPOLOGY", "TIME (s)", "THROUGHPUT", "LATENCY");
        System.out.println("----------------------------------------------------------------------");
        
        System.out.printf(" %-25s | %-12.4f | %,11.0f ops | %5.2f ns%n", 
                "SPSC (Core 2 -> Core 3)", spscSeconds, spscThroughput, spscLatency);
        System.out.printf(" %-25s | %-12.4f | %,11.0f ops | %5.2f ns%n", 
                "MPSC (Core 2,3 -> Core 1)", mpscSeconds, mpscThroughput, mpscLatency);
        
        System.out.println("----------------------------------------------------------------------");
        System.out.printf(" SPSC LATENCY STATUS: %s%n", spscOk ? "[OK] AAA+ Standard" : "[WARN] Exceeded limits");
        System.out.printf(" MPSC LATENCY STATUS: %s%n", mpscOk ? "[OK] AAA+ Standard" : "[WARN] Exceeded limits");
        System.out.println("======================================================================\n");
    }

    private static void runSpscBench() throws InterruptedException {
        DarkRingBus bus = new DarkRingBus(18); // 262,144 elements
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        Thread consumer = new Thread(() -> {
            ThreadPinning.pinToCore(3);
            endLatch.countDown();
            try { startLatch.await(); } catch (InterruptedException ignored) {}

            int received = 0;
            while (received < ITERATIONS) {
                long val = bus.poll();
                if (val != -1L) received++;
                else Thread.onSpinWait();
            }
            endLatch.countDown();
        }, "SPSC-Consumer");

        Thread producer = new Thread(() -> {
            ThreadPinning.pinToCore(2);
            endLatch.countDown();
            try { startLatch.await(); } catch (InterruptedException ignored) {}

            for (int i = 0; i < ITERATIONS; i++) {
                while (!bus.offer(i)) Thread.onSpinWait();
            }
        }, "SPSC-Producer");

        consumer.start();
        producer.start();

        Thread.sleep(100);
        long startTime = System.nanoTime();
        startLatch.countDown();
        
        consumer.join();
        long endTime = System.nanoTime();

        spscSeconds = (endTime - startTime) / 1_000_000_000.0;
        spscThroughput = ITERATIONS / spscSeconds;
        spscLatency = (spscSeconds * 1_000_000_000.0) / ITERATIONS;
        spscOk = spscLatency <= 150;
    }

    private static void runMpscBench() throws InterruptedException {
        DarkAtomicBus bus = new DarkAtomicBus(18);
        int halfIterations = ITERATIONS / 2;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(3);

        Thread consumer = new Thread(() -> {
            ThreadPinning.pinToCore(1);
            endLatch.countDown();
            try { startLatch.await(); } catch (InterruptedException ignored) {}

            int received = 0;
            while (received < ITERATIONS) {
                long val = bus.poll();
                if (val != -1L) received++;
                else Thread.onSpinWait();
            }
            endLatch.countDown();
        }, "MPSC-Consumer");

        Thread producer1 = new Thread(() -> {
            ThreadPinning.pinToCore(2);
            endLatch.countDown();
            try { startLatch.await(); } catch (InterruptedException ignored) {}

            for (int i = 0; i < halfIterations; i++) {
                while (!bus.offer(i)) Thread.onSpinWait();
            }
        }, "MPSC-Producer-1");

        Thread producer2 = new Thread(() -> {
            ThreadPinning.pinToCore(3);
            endLatch.countDown();
            try { startLatch.await(); } catch (InterruptedException ignored) {}

            for (int i = 0; i < halfIterations; i++) {
                while (!bus.offer(i)) Thread.onSpinWait();
            }
        }, "MPSC-Producer-2");

        consumer.start();
        producer1.start();
        producer2.start();

        Thread.sleep(100);
        long startTime = System.nanoTime();
        startLatch.countDown();
        
        consumer.join();
        long endTime = System.nanoTime();

        mpscSeconds = (endTime - startTime) / 1_000_000_000.0;
        mpscThroughput = ITERATIONS / mpscSeconds;
        mpscLatency = (mpscSeconds * 1_000_000_000.0) / ITERATIONS;
        mpscOk = mpscLatency <= 150;
    }
}
