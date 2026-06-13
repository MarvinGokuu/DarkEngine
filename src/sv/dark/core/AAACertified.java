// Reading Order: 00000100
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.core;

// JDK — Annotations
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AAA+ hardware-level performance certification marker.
 *
 * <p>Documents and statically validates strict latency, throughput, and memory 
 * alignment guarantees for critical engine components. 
 *
 * <p><b>Zero-Overhead Contract:</b> Must be discarded by the compiler 
 * ({@code RetentionPolicy.SOURCE}). It is strictly forbidden to use runtime 
 * retention, as reflective scanning would introduce unacceptable latency spikes 
 * on the hot path.
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@Retention(RetentionPolicy.SOURCE) // CRITICAL: Discarded in bytecode (0ns overhead)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface AAACertified {

    /**
     * Component license identifier.
     * Defaults to LGPL-3.0-or-later for the DarkEngine project.
     */
    String license() default "LGPL-3.0-or-later";

    /**
     * Author or owner of the certified component.
     */
    String author() default "Marvin Alexander Flores Canales";

    /**
     * Certification date (Format: YYYY-MM-DD).
     * Required for audit traceability.
     */
    String date();

    /**
     * Maximum permitted latency in nanoseconds.
     * Default: 150ns (AAA+ standard).
     */
    long maxLatencyNs() default 150;

    /**
     * Minimum required throughput in operations per second.
     * Default: 10,000,000 (10M ops/s).
     */
    long minThroughput() default 10_000_000;

    /**
     * Memory alignment requirement in bytes.
     * Default: 64 (L1 Cache Line).
     */
    int alignment() default 64;

    /**
     * Indicates whether the component guarantees lock-free execution.
     * Default: true.
     */
    boolean lockFree() default true;

    /**
     * Indicates whether the component utilizes off-heap memory mapping.
     * Default: false.
     */
    boolean offHeap() default false;

    /**
     * Additional notes regarding the certification guarantees or limitations.
     */
    String notes() default "";
}
