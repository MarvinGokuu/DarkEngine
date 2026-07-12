// Reading Order: 00100001
//  33
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: Procesamiento de señales sin boxing. (Signal processing without boxing).
 * WHY: java.util.function.LongConsumer involves type inheritance which can prevent aggressive JIT optimizations. We need a domain-specific functional interface for the hot-path.
 * TECHNIQUE: Functional Interface for 64-bit signal processing. No object creation, no I/O, no blocking.
 * GUARANTEES: Zero-allocation, hot-path optimized. Expected latency <50ns per signal.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@FunctionalInterface
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
public interface SignalProcessor {

    /**
     * Procesa una senal de 64 bits.
     * 
     * ADVERTENCIA: Este metodo se llama en hot-path.
     * - NO crear objetos
     * - NO hacer I/O
     * - NO bloquear threads
     * - NO lanzar excepciones (usar codigos de error)
     * 
     * LATENCIA ESPERADA: <50ns por senal
     * 
     * @param signal Senal empaquetada (64 bits)
     */
    void process(long signal);
}
