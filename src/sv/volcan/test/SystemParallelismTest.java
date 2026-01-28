package sv.volcan.test;

import sv.volcan.core.systems.GameSystem;
import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;

/**
 * Sistema de prueba C - Depende de SystemExecutionTest
 * 
 * Este sistema se ejecuta en Layer 1 después de SystemExecutionTest.
 * Puede ejecutarse en paralelo con SystemDependencyTest.
 * 
 * VALIDACIÓN DE PARALELISMO:
 * - SystemDependencyTest y SystemParallelismTest no tienen dependencias entre
 * sí
 * - Ambos dependen solo de SystemExecutionTest
 * - Por lo tanto, pueden ejecutarse en paralelo en Layer 1
 */
public class SystemParallelismTest implements GameSystem {
    @SuppressWarnings("unused") // implementar contador de ejecuciones
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        executionCount++;

        // Leer el valor que escribió SystemExecutionTest
        int valueFromA = state.readInt(VolcanStateLayout.PLAYER_X);

        // Simular trabajo: escribir en otro slot diferente a B
        state.writeInt(VolcanStateLayout.PLAYER_DIR, valueFromA + 100);

        // Log deshabilitado para evitar spam en terminal
        // if (executionCount % 60 == 0) {
        // System.out.println("[SystemParallelismTest] Executed " + executionCount + "
        // times,
        // computed: " + (valueFromA + 100));
        // }
    }

    @Override
    public String getName() {
        return "SystemParallelismTest";
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "SystemExecutionTest" }; // Depende de SystemExecutionTest (igual que
                                                       // SystemDependencyTest)
    }
}
