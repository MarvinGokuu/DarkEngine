// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ui;

import sv.dark.core.AAACertified;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * RESPONSIBILITY: Lock-free state channel between kernel and visual layer.
 * WHY: The visual UI thread must be able to read the current state of the engine without causing lock contention or blocking the kernel thread.
 * TECHNIQUE: AtomicInteger — zero locks, zero contention, zero allocations. The kernel writes the state via AsyncLogWriter, the visual layer only reads.
 * GUARANTEES: Visibility between threads without locks or explicit memory barriers.
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
@AAACertified(date = "2026-06-11", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = false, offHeap = false, notes = "Automatically AAA Certified during Core Audit")
public final class EngineStateChannel {

    public static final int STATE_BOOT    = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_TIER3   = 2;

    /**
     * Current state of the engine. Written by AsyncLogWriter (when parsing stdout),
     * read by DarkEngineWindow. AtomicInteger guarantees visibility between threads
     * without locks or explicit memory barriers.
     */
    public static final AtomicInteger STATE = new AtomicInteger(STATE_BOOT);

    /** Exposes the off-heap state vault to the UI thread in a lock-free/volatile manner. */
    public static volatile sv.dark.state.DarkStateVault vault = null;

    private EngineStateChannel() {}
}
