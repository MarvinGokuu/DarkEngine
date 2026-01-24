package sv.volcan.core;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.Arena;
import java.util.Random;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Simulación Masiva de Partículas Off-Heap.
 * DEPENDENCIAS: MemorySegment, Arena
 * MÉTRICAS: Zero-GC, SIMD-Friendly Memory Layout
 * 
 * Sistema de partículas de alto rendimiento. Utiliza un bloque contiguo
 * de memoria nativa para maximizar la localidad de caché y permitir
 * actualizaciones vectorizadas.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanParticleSystem {

    // [INGENIERÍA DURA]: Capacidad y Stride calculados para saturar la línea de
    // caché.
    private static final int MAX_PARTICLES = 1000;
    private static final long STRIDE = 24L; // Layout: x(4), y(4), vx(4), vy(4), life(4), size(4) = 24 bytes

    // [FIX AUDIT]: Seeded Random for deterministic particle initialization
    // PORQUÉ: Math.random() no es determinista, rompe garantía de reproducibilidad
    // TÉCNICA: Random con seed fijo garantiza misma secuencia siempre
    // GARANTÍA: Mismo seed = mismas posiciones de partículas
    private static final Random RNG = new Random(0xCAFEBABE); // Fixed seed for determinism

    private final MemorySegment particleData;

    public VolcanParticleSystem(Arena arena) {
        // Reservamos un solo bloque de memoria nativa. Alineación a 64 para prefetcher
        // optimizado.
        this.particleData = arena.allocate(MAX_PARTICLES * STRIDE, 64L);
        initializeParticles();
    }

    private void initializeParticles() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            long base = i * STRIDE;
            // Inicialización determinista del flujo binario
            particleData.set(ValueLayout.JAVA_FLOAT, base, RNG.nextFloat() * 1280); // X
            particleData.set(ValueLayout.JAVA_FLOAT, base + 4, RNG.nextFloat() * 720); // Y
            particleData.set(ValueLayout.JAVA_FLOAT, base + 8, RNG.nextFloat() * 2); // Speed (VY)
        }
    }

    /**
     * Procesamiento de cinemática de partículas.
     * [MECHANICAL SYMPATHY]: Recorrido lineal de memoria para maximizar el L1-Cache
     * Hit Rate.
     */
    public void update(double dt) {
        float deltaTime = (float) dt;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            long base = i * STRIDE;

            // Acceso atómico a memoria nativa
            float y = particleData.get(ValueLayout.JAVA_FLOAT, base + 4);
            float speed = particleData.get(ValueLayout.JAVA_FLOAT, base + 8);

            y += speed * deltaTime * 60.0f;

            // Reciclaje de partículas sin de-asignación (Zero-GC)
            if (y > 720)
                y = -10;

            particleData.set(ValueLayout.JAVA_FLOAT, base + 4, y);
        }
    }
}
// actualizado3/1/26
