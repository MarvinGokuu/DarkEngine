package sv.volcan.core.systems;

import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;
import sv.volcan.core.EntityLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * AUTORIDAD: Sector
 * RESPONSABILIDAD: Procesamiento lineal de cinemática de alta frecuencia.
 * GARANTÍAS: Acceso secuencial a memoria (Cache-friendly), zero-allocation,
 * pre-calculado para optimización SIMD.
 * DOMINIO CRÍTICO: Ejecución
 * 
 * PATRÓN: Strategy Pattern (implementa SovereignSystem)
 * PRINCIPIO SOLID: Single Responsibility - Solo calcula movimiento
 * CONCEPTO: Cache Locality - Acceso secuencial para maximizar L1 hits
 * ROL: Performance Engineer aplicando Mechanical Sympathy
 */
public final class MovementSystem implements SovereignSystem {

    private static final long STRIDE = EntityLayout.STRIDE;
    private static final long X_OFF = EntityLayout.X_OFFSET;
    private static final long VX_OFF = EntityLayout.VX_OFFSET;

    /**
     * Update de alta velocidad.
     * Procesa el sector de memoria como un flujo continuo de bytes.
     * 
     * IMPLEMENTACIÓN: SovereignSystem.update()
     * GARANTÍA: Determinista - mismo state + deltaTime = mismo resultado
     * OPTIMIZACIÓN: Acceso secuencial (stride-based) para prefetching de CPU
     */
    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        // SSOT: Leemos entityCount desde el estado (no como parámetro)
        int entityCount = state.readInt(VolcanStateLayout.ENTITY_COUNT);

        // Obtenemos el segmento de memoria nativa una sola vez
        MemorySegment segment = state.getRawSegment();
        long currentBase = 0;

        for (int i = 0; i < entityCount; i++) {
            // Lectura y Escritura mediante offsets constantes (Mechanical Sympathy)
            double x = segment.get(ValueLayout.JAVA_DOUBLE, currentBase + X_OFF);
            double vx = segment.get(ValueLayout.JAVA_DOUBLE, currentBase + VX_OFF);

            // Inyección física directa: x = x + (vx * deltaTime)
            segment.set(ValueLayout.JAVA_DOUBLE, currentBase + X_OFF, x + (vx * deltaTime));

            // Avance del puntero de memoria (Evita la multiplicación en el loop)
            currentBase += STRIDE;
        }
    }
}