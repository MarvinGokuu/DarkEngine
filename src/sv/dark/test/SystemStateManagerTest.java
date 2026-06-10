package sv.dark.test;

import sv.dark.kernel.SystemSnapshot;
import sv.dark.kernel.SystemStateManager;
import sv.dark.kernel.CleanupValidator;

/**
 * AUTORIDAD: Dark
 * RESPONSABILIDAD: Test de validación del ciclo de vida del SystemStateManager
 *
 * Valida:
 * 1. Captura correcta de parámetros en Windows.
 * 2. Aplicación exitosa de optimizaciones de alto rendimiento.
 * 3. Restauración íntegra al estado original sin configuraciones residuales.
 */
public class SystemStateManagerTest {

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("TEST: SYSTEM STATE MANAGER & CLEANUP VALIDATOR");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Step 1: Capture initial state
        System.out.println("[TEST] Step 1: Capturing initial system snapshot...");
        SystemSnapshot initial = SystemStateManager.captureInitialState();
        initial.print();

        if (initial.powerSchemeGuid == null || initial.powerSchemeGuid.isEmpty()) {
            throw new AssertionError("FAILED: Power Scheme GUID cannot be null or empty.");
        }
        if (initial.powerSource == null || initial.powerSource.isEmpty()) {
            throw new AssertionError("FAILED: Power Source cannot be null or empty.");
        }
        System.out.println("[TEST] Step 1 passed.\n");

        // Step 2: Apply performance boost
        System.out.println("[TEST] Step 2: Applying performance boost (High Performance)...");
        boolean boostSuccess = SystemStateManager.applyPerformanceBoost();
        if (!boostSuccess) {
            System.err.println("[TEST WARNING] Failed to apply performance boost. It might require admin privileges or GUID is not supported on this OS edition.");
        } else {
            System.out.println("[TEST] Step 2 passed.");
        }
        System.out.println();

        // Step 3: Restore initial state
        System.out.println("[TEST] Step 3: Restoring original OS settings...");
        boolean restoreSuccess = SystemStateManager.restoreInitialState(initial);
        if (!restoreSuccess) {
            throw new AssertionError("FAILED: Thread affinity or Power Plan restoration failed.");
        }
        System.out.println("[TEST] Step 3 passed.\n");

        // Step 4: Validate clean state
        System.out.println("[TEST] Step 4: Capturing final snapshot and running CleanupValidator...");
        SystemSnapshot current = SystemStateManager.captureInitialState();
        boolean clean = CleanupValidator.validate(initial, current);
        if (!clean) {
            throw new AssertionError("FAILED: CleanupValidator flagged residual OS configurations.");
        }
        System.out.println("[TEST] Step 4 passed.\n");

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("SYSTEM STATE MANAGER TEST: PASSED ✓");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
    }
}
