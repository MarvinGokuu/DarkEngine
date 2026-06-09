package sv.dark.test;

import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import sv.dark.state.DarkStateLayout;

/**
 * Sistema de prueba A - Sin dependencias
 * 
 * Este sistema se ejecuta primero (Layer 0) porque no tiene dependencias.
 * Simula procesamiento de input básico.
 */
public class SystemExecutionTest implements GameSystem {

    @SuppressWarnings("unused") // implementar contador de ejecuciones
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        executionCount++;

        // Simular trabajo: escribir un valor en el estado
        int currentValue = state.readInt(DarkStateLayout.PLAYER_X);
        state.writeInt(DarkStateLayout.PLAYER_X, currentValue + 1);

        // Log deshabilitado para evitar spam en terminal
        // if (executionCount % 60 == 0) {
        // System.out.println("[SystemExecutionTest] Executed " + executionCount + "
        // times,
        // value: " + (currentValue + 1));
        // }
    }

    @Override
    public String getName() {
        return "SystemExecutionTest";
    }

    @Override
    public String[] getDependencies() {
        return new String[0]; // Sin dependencias - se ejecuta primero
    }
}
