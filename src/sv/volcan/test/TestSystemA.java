package sv.volcan.test;

import sv.volcan.core.systems.SovereignSystem;
import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;

/**
 * Sistema de prueba A - Sin dependencias
 * 
 * Este sistema se ejecuta primero (Layer 0) porque no tiene dependencias.
 * Simula procesamiento de input básico.
 */
public class TestSystemA implements SovereignSystem {

    @SuppressWarnings("unused") // implementar contador de ejecuciones
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        executionCount++;

        // Simular trabajo: escribir un valor en el estado
        int currentValue = state.readInt(VolcanStateLayout.PLAYER_X);
        state.writeInt(VolcanStateLayout.PLAYER_X, currentValue + 1);

        // Log deshabilitado para evitar spam en terminal
        // if (executionCount % 60 == 0) {
        // System.out.println("[TestSystemA] Executed " + executionCount + " times,
        // value: " + (currentValue + 1));
        // }
    }

    @Override
    public String getName() {
        return "TestSystemA";
    }

    @Override
    public String[] getDependencies() {
        return new String[0]; // Sin dependencias - se ejecuta primero
    }
}
