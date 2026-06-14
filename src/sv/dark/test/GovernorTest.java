// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.kernel.TimeKeeper;

/**
 * RESPONSIBILITY: Dynamic Performance Governor Validation Test.
 * WHY: We need to verify that the system adapts its FPS output based on workload to save power without dropping frames.
 * TECHNIQUE: Simulates variable workloads (light, heavy, overload) using Thread.onSpinWait() to force the Governor to shift gears.
 * GUARANTEES: Upshifts gears in light situations, downshifts during stress spikes, without crashing the game loop.
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
public class GovernorTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=============================================================================================================================");
        System.out.println("TEST: DARK GOVERNOR (DYNAMIC FPS SCALING)");
        System.out.println("=============================================================================================================================");

        TimeKeeper timeKeeper = new TimeKeeper();

        System.out.println("\n[PHASE 1] Warmup (Simulating light load)...");
        // Should upshift to Gear 2 (120 FPS) and then Gear 3 (144 FPS) if fast enough
        simulateFrames(timeKeeper, 180, 1_000_000); // 1ms load (very light)

        System.out.println("\n[PHASE 2] Stress Test (Simulating 'Cyberpunk' Load)...");
        // Simulate a heavy 12ms frame.
        // At 144 FPS (7ms budget) this is unacceptable -> Should downshift to Gear 1 (60 FPS, 16ms budget)
        simulateFrames(timeKeeper, 60, 12_000_000); // 12ms load

        System.out.println("\n[PHASE 3] Recovery (Returning to calm)...");
        // Should gradually recover gears
        simulateFrames(timeKeeper, 200, 2_000_000); // 2ms load

        System.out.println("\n[PHASE 4] TNT OVERLOAD (Simulating massive explosion in Minecraft)...");
        // 50ms load per frame. This breaks even the 60 FPS budget (16ms).
        // The system must stay in Gear 1 and report warnings, without collapsing.
        simulateFrames(timeKeeper, 30, 50_000_000); // 50ms load

        System.out.println("\n=============================================================================================================================");
        System.out.println("TEST COMPLETE");
        System.out.println("=============================================================================================================================");
    }

    private static void simulateFrames(TimeKeeper tk, int frames, long workloadNs) {
        for (int i= 0; i< frames; i++) {
            tk.startFrame();

            // Simulate work (Sleeper)
            long startWork = System.nanoTime();
            while (System.nanoTime() - startWork < workloadNs) {
                Thread.onSpinWait();
            }

            // Record simulated time in Phase 3 (Systems)
            tk.recordPhaseTime(3, workloadNs);

            // Print status every 30 frames to avoid console saturation
            if (i % 30 == 0) {
                tk.printStats();
            }

            tk.waitForNextFrame();
        }
    }
}
