package sv.volcan.test;

import sv.volcan.core.systems.SovereignSystem;
import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;

/**
 * Sistema de prueba C - Depende de TestSystemA
 * 
 * Este sistema se ejecuta en Layer 1 después de TestSystemA.
 * Puede ejecutarse en paralelo con TestSystemB.
 * 
 * VALIDACIÓN DE PARALELISMO:
 * - TestSystemB y TestSystemC no tienen dependencias entre sí
 * - Ambos dependen solo de TestSystemA
 * - Por lo tanto, pueden ejecutarse en paralelo en Layer 1
 */
public class TestSystemC implements SovereignSystem {
    @SuppressWarnings("unused") // implementar contador de ejecuciones
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        executionCount++;

        // Leer el valor que escribió TestSystemA
        int valueFromA = state.readInt(VolcanStateLayout.PLAYER_X);

        // Simular trabajo: escribir en otro slot diferente a B
        state.writeInt(VolcanStateLayout.PLAYER_DIR, valueFromA + 100);

        // Log deshabilitado para evitar spam en terminal
        // if (executionCount % 60 == 0) {
        // System.out.println("[TestSystemC] Executed " + executionCount + " times,
        // computed: " + (valueFromA + 100));
        // }
    }

    @Override
    public String getName() {
        return "TestSystemC";
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "TestSystemA" }; // Depende de A (igual que B)
    }
}
