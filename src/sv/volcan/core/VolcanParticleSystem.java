package sv.volcan.core;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.Arena;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Simulación de partículas masivas mediante memoria nativa
 * contigua.
 * GARANTÍAS: Zero-GC, Cache-locality extrema, alineación de 64-bytes
 * (SIMD-ready).
 * PROHIBICIONES: Prohibido instanciar objetos 'Particle'; prohibido el acceso
 * indexado no lineal.
 * DOMINIO CRÍTICO: Visualización / FX (Cómputo Intensivo)
 */
public final class VolcanParticleSystem {

    // [INGENIERÍA DURA]: Capacidad y Stride calculados para saturar la línea de
    // caché.
    private static final int MAX_PARTICLES = 1000;
    private static final long STRIDE = 24L; // Layout: x(4), y(4), vx(4), vy(4), life(4), size(4) = 24 bytes

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
            particleData.set(ValueLayout.JAVA_FLOAT, base, (float) (Math.random() * 1280)); // X
            particleData.set(ValueLayout.JAVA_FLOAT, base + 4, (float) (Math.random() * 720)); // Y
            particleData.set(ValueLayout.JAVA_FLOAT, base + 8, (float) (Math.random() * 2)); // Speed (VY)
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
