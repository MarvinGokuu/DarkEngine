package sv.volcan.test;

import sv.volcan.core.systems.SovereignSystem;
import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;

/**
 * Sistema de prueba B - Depende de TestSystemA
 * 
 * Este sistema se ejecuta en Layer 1 después de TestSystemA.
 * Puede ejecutarse en paralelo con TestSystemC.
 */
public class TestSystemB implements SovereignSystem {

    @SuppressWarnings("unused") // implementar contador de ejecuciones
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        executionCount++;

        // Leer el valor que escribió TestSystemA
        int valueFromA = state.readInt(VolcanStateLayout.PLAYER_X);

        // Simular trabajo: escribir en otro slot
        state.writeInt(VolcanStateLayout.PLAYER_Y, valueFromA * 2);

        // Log deshabilitado para evitar spam en terminal
        // if (executionCount % 60 == 0) {
        // System.out.println("[TestSystemB] Executed " + executionCount + " times,
        // computed: " + (valueFromA * 2));
        // }
    }

    @Override
    public String getName() {
        return "TestSystemB";
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "TestSystemA" }; // Depende de A
    }
}
