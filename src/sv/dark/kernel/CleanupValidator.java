package sv.dark.kernel;

/**
 * TECHNICAL SPECIFICATION
 *
 * CONTEXT:
 * - Auditor system validating that no residual hardware or OS modifications remain after shutdown.
 *
 * MEMORY SEMANTICS:
 * - Thread-confined execution.
 */
public final class CleanupValidator {

    /**
     * Compara el snapshot inicial del sistema con el snapshot posterior al apagado.
     * Genera reportes de auditoría y advierte sobre cualquier configuración residual.
     * 
     * @param initial El snapshot del sistema capturado antes de arrancar.
     * @param current El snapshot del sistema capturado después de apagar el motor.
     * @return true si el sistema se restauró por completo (sin residuos), false en caso contrario.
     */
    public static boolean validate(SystemSnapshot initial, SystemSnapshot current) {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("OS CLEANUP AUDIT: INITIAL vs POST-SHUTDOWN");
        System.out.println("═══════════════════════════════════════════════════════════════");

        boolean passed = true;

        if (initial == null || current == null) {
            System.err.println("  ❌ AUDIT CRITICAL: One or both snapshots are null.");
            System.out.println("═══════════════════════════════════════════════════════════════\n");
            return false;
        }

        // 1. Validar restauración de afinidad de hilos
        if (initial.threadAffinityMask == current.threadAffinityMask) {
            System.out.println("  ✅ Thread Affinity: RESTORED OK");
        } else {
            System.err.printf("  ❌ THREAD AFFINITY RESIDUAL DETECTED: Initial: 0x%X | Post-Shutdown: 0x%X%n",
                    initial.threadAffinityMask, current.threadAffinityMask);
            passed = false;
        }

        // 2. Validar restauración del plan de energía
        if (initial.powerSchemeGuid.equalsIgnoreCase(current.powerSchemeGuid)) {
            System.out.println("  ✅ Power Scheme: RESTORED OK (" + initial.powerSchemeName + ")");
        } else {
            System.err.printf("  ❌ POWER SCHEME RESIDUAL DETECTED: Initial: %s (%s) | Post-Shutdown: %s (%s)%n",
                    initial.powerSchemeName, initial.powerSchemeGuid, current.powerSchemeName, current.powerSchemeGuid);
            passed = false;
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        if (passed) {
            System.out.println("✅ SYSTEM RESTORE VALIDATION PASSED: 100% CLEAN");
        } else {
            System.out.println("❌ SYSTEM RESTORE VALIDATION FAILED: OS is in a dirty/modified state");
        }
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        return passed;
    }
}
