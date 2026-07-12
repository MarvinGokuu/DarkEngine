// Reading Order: 10101011
//  171
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.kernel.BaselineValidator;
import sv.dark.kernel.BaselineValidator.MemorySnapshot;
import sv.dark.kernel.EngineKernel;
import sv.dark.bus.DarkEventDispatcher;
import sv.dark.memory.SectorMemoryVault;

/**
 * RESPONSIBILITY: Graceful Shutdown and Baseline Validation Protocol Test.
 * WHY: Native memory leaks (Project Panama) or thread leaks could degrade performance over multiple server restarts.
 * TECHNIQUE: A/B/C Protocol Test using BaselineValidator to capture state before (A), during (B), and after (C) engine execution.
 * GUARANTEES: Validates that State A == State C, guaranteeing absolutely no memory leaks or thread leaks.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
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
public class GracefulShutdownTest {

    public static void main(String[] args) {
        System.setProperty("sv.dark.test.nohalt", "true");
        System.out.print("[TEST] Preloading subsystems to stabilize baseline... ");
        try {
            Class.forName("javax.imageio.ImageIO");
            Class.forName("sv.dark.editor.DarkAssetCompiler");
            // Preload Virtual Thread machinery (ForkJoinPool, Unblocker) to avoid false positive thread leaks
            Thread.startVirtualThread(() -> {}).join();
        } catch (Exception e) {}
        
        System.out.print("\n[TEST] Running Graceful Shutdown & Baseline Protocol... ");

        MemorySnapshot stateA = BaselineValidator.captureStateA();

        Thread engineThread = new Thread(() -> {
            try {
                DarkEventDispatcher dispatcher = DarkEventDispatcher.createDefault(14);
                SectorMemoryVault vault = new SectorMemoryVault(1024);
                EngineKernel kernel = new EngineKernel(dispatcher, vault);
                kernel.start();
            } catch (Exception e) {
                System.err.println("Engine error: " + e.getMessage());
            }
        }, "EngineThread");

        engineThread.start();

        try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        MemorySnapshot stateB = BaselineValidator.captureStateB();

        engineThread.interrupt();

        try {
            engineThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        MemorySnapshot stateC = BaselineValidator.captureStateC();

        // No debug thread dump in production AAA+ tests

        boolean passed = BaselineValidator.validateCleanShutdown(stateA, stateC);

        System.out.println("DONE.");
        System.out.println("\n======================================================================");
        System.out.println("               GRACEFUL SHUTDOWN MEMORY PROTOCOL SUMMARY              ");
        System.out.println("======================================================================");
        System.out.printf(" %-20s | %-12s | %-12s | %-10s%n", "STATE", "HEAP (MB)", "NON-HEAP (MB)", "THREADS");
        System.out.println("----------------------------------------------------------------------");
        System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "A (Pre-Boot)", stateA.heapUsedBytes / 1048576.0, stateA.nonHeapUsedBytes / 1048576.0, stateA.threadCount);
        System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "B (Execution)", stateB.heapUsedBytes / 1048576.0, stateB.nonHeapUsedBytes / 1048576.0, stateB.threadCount);
        System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "C (Post-Shutdown)", stateC.heapUsedBytes / 1048576.0, stateC.nonHeapUsedBytes / 1048576.0, stateC.threadCount);
        System.out.println("----------------------------------------------------------------------");
        
        long heapImpact = stateB.heapUsedBytes - stateA.heapUsedBytes;
        long nonHeapImpact = stateB.nonHeapUsedBytes - stateA.nonHeapUsedBytes;
        System.out.printf(" ENGINE HEAP IMPACT   : %,d bytes (%.2f MB)%n", heapImpact, heapImpact / 1048576.0);
        System.out.printf(" ENGINE NON-HEAP PULL : %,d bytes (%.2f MB)%n", nonHeapImpact, nonHeapImpact / 1048576.0);
        System.out.printf(" ENGINE THREAD COUNT  : %d threads%n", stateB.threadCount - stateA.threadCount);
        System.out.println("----------------------------------------------------------------------");
        System.out.printf(" LEAK STATUS: %s%n", passed ? "[OK] NO LEAKS DETECTED (AAA+)" : "[WARN] POTENTIAL LEAK DETECTED");
        System.out.println("======================================================================\n");

        if (passed) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    private static String formatSnapshot(MemorySnapshot snapshot) {
        return String.format("Heap=%.2fMB, NonHeap=%.2fMB, Threads=%d",
                snapshot.heapUsedBytes / 1_048_576.0,
                snapshot.nonHeapUsedBytes / 1_048_576.0,
                snapshot.threadCount);
    }
}
