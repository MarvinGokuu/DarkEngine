/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Anotación de certificación AAA+ (Compile-Time Only)
 * DEPENDENCIAS: Ninguna
 * MÉTRICAS: Overhead = 0ns (eliminada en bytecode)
 * 
 * Marca un componente como certificado bajo el estándar AAA+ de baja latencia.
 * Esta anotación es SOLO para documentación y validación estática.
 * NO tiene overhead en runtime (RetentionPolicy.SOURCE).
 * 
 * GARANTÍAS:
 * - Overhead de compilación: <1ms
 * - Overhead de runtime: 0ns (eliminada después de javac)
 * - Memoria consumida: 0 bytes
 * 
 * PROHIBIDO:
 * - NO usar RetentionPolicy.RUNTIME (añadiría overhead)
 * - NO validar en hot-path (añadiría latencia)
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-06
 */
// Reading Order: 00000100
package sv.volcan.core;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE) // ← CRÍTICO: Eliminada en bytecode (0ns overhead)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface AAACertified {

    /**
     * Licencia del componente.
     * Default: "AAA+ Certification (Propietaria)"
     */
    String license() default "AAA+ Certification (Propietaria)";

    /**
     * Autor del componente certificado.
     * Default: "Marvin-Dev"
     */
    String author() default "Marvin-Dev";

    /**
     * Fecha de certificación (formato: YYYY-MM-DD).
     * Requerido para trazabilidad.
     */
    String date();

    /**
     * Latencia máxima permitida en nanosegundos.
     * Default: 150ns (estándar AAA+)
     */
    long maxLatencyNs() default 150;

    /**
     * Throughput mínimo en operaciones por segundo.
     * Default: 10,000,000 (10M ops/s)
     */
    long minThroughput() default 10_000_000;

    /**
     * Alineación de memoria requerida en bytes.
     * Default: 64 (L1 Cache Line)
     */
    int alignment() default 64;

    /**
     * Indica si el componente es lock-free.
     * Default: true (estándar AAA+)
     */
    boolean lockFree() default true;

    /**
     * Indica si el componente usa off-heap memory.
     * Default: false (no todos los componentes lo requieren)
     */
    boolean offHeap() default false;

    /**
     * Notas adicionales sobre la certificación.
     * Default: ""
     */
    String notes() default "";
}
