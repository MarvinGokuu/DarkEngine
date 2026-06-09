package sv.dark.test;

import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import sv.dark.state.DarkStateLayout;

/**
 * Sistema de prueba B - Depende de SystemExecutionTest
 * 
 * Este sistema se ejecuta en Layer 1 después de SystemExecutionTest.
 * Puede ejecutarse en paralelo con SystemParallelismTest.
 */
public class SystemDependencyTest implements GameSystem {

    @SuppressWarnings("unused") // implementar contador de ejecuciones
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        executionCount++;

        // Leer el valor que escribió SystemExecutionTest
        int valueFromA = state.readInt(DarkStateLayout.PLAYER_X);

        // Simular trabajo: escribir en otro slot
        state.writeInt(DarkStateLayout.PLAYER_Y, valueFromA * 2);

        // Log deshabilitado para evitar spam en terminal
        // if (executionCount % 60 == 0) {
        // System.out.println("[SystemDependencyTest] Executed " + executionCount + "
        // times,
        // computed: " + (valueFromA * 2));
        // }
    }

    @Override
    public String getName() {
        return "SystemDependencyTest";
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "SystemExecutionTest" }; // Depende de SystemExecutionTest
    }
}
