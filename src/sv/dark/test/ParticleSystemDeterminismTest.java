package sv.dark.test;

import sv.dark.core.DarkParticleSystem;
import java.lang.foreign.Arena;
import java.lang.reflect.Field;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Random;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Verificar la reproducibilidad del sistema de partículas
 * mediante un generador de números aleatorios con seed fijo.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-06-03
 */
public class ParticleSystemDeterminismTest {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  AAA+ CERTIFICATION: PARTICLE SYSTEM DETERMINISM");
        System.out.println("=======================================================");

        try (Arena arena = Arena.ofConfined()) {
            // Generar los valores esperados usando la misma seed y secuencia
            Random expectedRNG = new Random(0xCAFEBABE);
            float expectedX = expectedRNG.nextFloat() * 1280;
            float expectedY = expectedRNG.nextFloat() * 720;
            float expectedSpeed = expectedRNG.nextFloat() * 2;

            // Instanciar DarkParticleSystem
            DarkParticleSystem system = new DarkParticleSystem(arena);

            // Acceder al campo privado particleData mediante reflexión
            Field dataField = DarkParticleSystem.class.getDeclaredField("particleData");
            dataField.setAccessible(true);
            MemorySegment particleData = (MemorySegment) dataField.get(system);

            // Leer los valores de la primera partícula (X en offset 0, Y en offset 4, speed en offset 8)
            float actualX = particleData.get(ValueLayout.JAVA_FLOAT, 0L);
            float actualY = particleData.get(ValueLayout.JAVA_FLOAT, 4L);
            float actualSpeed = particleData.get(ValueLayout.JAVA_FLOAT, 8L);

            System.out.printf("[TEST] Expected Particle 0: X=%.6f, Y=%.6f, Speed=%.6f%n", expectedX, expectedY, expectedSpeed);
            System.out.printf("[TEST] Actual Particle 0:   X=%.6f, Y=%.6f, Speed=%.6f%n", actualX, actualY, actualSpeed);

            if (Math.abs(actualX - expectedX) < 1e-5 &&
                Math.abs(actualY - expectedY) < 1e-5 &&
                Math.abs(actualSpeed - expectedSpeed) < 1e-5) {
                System.out.println("\n[PASSED] PARTICLE SYSTEM INITIALIZATION IS DETERMINISTIC");
                System.exit(0);
            } else {
                System.err.println("\n[FAILED] PARTICLE SYSTEM INITIALIZATION IS NOT DETERMINISTIC");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
