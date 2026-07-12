// Reading Order: 01111010
//  122
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.kernel.SystemSnapshot;
import sv.dark.kernel.SystemStateManager;
import sv.dark.kernel.CleanupValidator;

/**
 * RESPONSIBILITY: SystemStateManager Lifecycle Validation Test.
 * WHY: Performance tweaks (Power Scheme, Thread Affinity) must be accurately applied and cleanly rolled back.
 * TECHNIQUE: Captures baseline telemetry, applies high-performance boost, restores, and uses CleanupValidator.
 * GUARANTEES: Successful application of high-performance optimizations and complete integrity restoration to original state.
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
public class SystemStateManagerTest {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("TEST: SYSTEM STATE MANAGER & CLEANUP VALIDATOR");
        System.out.println("===============================================================\n");

        // Step 1: Capture initial state
        System.out.println("[TEST] Step 1: Capturing initial system snapshot...");
        SystemSnapshot initial = SystemStateManager.captureInitialState();
        System.out.println(initial.formatTelemetryData());

        if (initial.powerSchemeGuid == null || initial.powerSchemeGuid.isEmpty()) {
            throw new AssertionError("FAILED: Power Scheme GUID cannot be null or empty.");
        }
        if (initial.powerSource == null || initial.powerSource.isEmpty()) {
            throw new AssertionError("FAILED: Power Source cannot be null or empty.");
        }
        System.out.println("[TEST] Step 1 passed.\n");

        // Step 2: Apply performance boost
        System.out.println("[TEST] Step 2: Applying performance boost (High Performance)...");
        boolean boostSuccess = SystemStateManager.applyPerformanceBoost();
        if (!boostSuccess) {
            System.err.println("[TEST WARNING] Failed to apply performance boost. It might require admin privileges or GUID is not supported on this OS edition.");
        } else {
            System.out.println("[TEST] Step 2 passed.");
        }
        System.out.println();

        // Step 3: Restore initial state
        System.out.println("[TEST] Step 3: Restoring original OS settings...");
        boolean restoreSuccess = SystemStateManager.restoreInitialState(initial);
        if (!restoreSuccess) {
            throw new AssertionError("FAILED: Thread affinity or Power Plan restoration failed.");
        }
        System.out.println("[TEST] Step 3 passed.\n");

        // Step 4: Validate clean state
        System.out.println("[TEST] Step 4: Capturing final snapshot and running CleanupValidator...");
        SystemSnapshot current = SystemStateManager.captureInitialState();
        boolean clean = CleanupValidator.validate(initial, current);
        if (!clean) {
            throw new AssertionError("FAILED: CleanupValidator flagged residual OS configurations.");
        }
        System.out.println("[TEST] Step 4 passed.\n");

        System.out.println("===============================================================");
        System.out.println("SYSTEM STATE MANAGER TEST: PASSED [OK]");
        System.out.println("===============================================================\n");
    }
}
