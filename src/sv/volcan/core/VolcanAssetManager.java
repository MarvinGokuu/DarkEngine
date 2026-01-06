package sv.volcan.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Localización y validación de activos industriales (Texturas,
 * ABI, Config).
 * GARANTÍAS: Resolución multiplataforma, fallback automático, validación de
 * integridad.
 * PROHIBICIONES: Prohibido usar File (I/O antiguo), prohibido llamar a
 * resolve() en el hot-path.
 * DOMINIO CRÍTICO: Assets / Persistencia
 */
public final class VolcanAssetManager {

    // Ruta de la Bóveda definida por variable de entorno o fallback seguro
    private static final String VAULT_ENV = System.getenv("VOLCAN_VAULT");
    private static final Path DEFAULT_VAULT = Paths.get(System.getProperty("user.home"), "Volcan_Vault");
    private static final Path LOCAL_ASSETS = Paths.get("assets");

    private VolcanAssetManager() {
    } // Sellado: Solo utilidad estática

    /**
     * Resuelve la ubicación absoluta de un activo priorizando la Bóveda Soberana.
     * [NOTA TÉCNICA]: No llamar durante la simulación. El costo de syscall es
     * inaceptable.
     */
    public static Path resolve(String assetName) {
        Path vaultBase = (VAULT_ENV != null) ? Paths.get(VAULT_ENV) : DEFAULT_VAULT;
        Path target = vaultBase.resolve(assetName);

        // 1. Prioridad: Bóveda Soberana (Ubicación externa al motor)
        if (Files.exists(target)) {
            return target.toAbsolutePath();
        }

        // 2. Fallback: Local Assets (Ubicación relativa al binario)
        Path localTarget = LOCAL_ASSETS.resolve(assetName);
        if (Files.exists(localTarget)) {
            return localTarget.toAbsolutePath();
        }

        // 3. Fallo Crítico de Activo
        return null;
    }

    /**
     * Diagnóstico de integridad de la Bóveda.
     * [VIOLACIÓN PROTOCOLO V2.0]: Uso de System.out/err detectado.
     * Mantener solo para debug inicial de infraestructura.
     */
    public static void probeVault() {
        Path vaultBase = (VAULT_ENV != null) ? Paths.get(VAULT_ENV) : DEFAULT_VAULT;

        if (Files.isDirectory(vaultBase)) {
            // [LOG]: Bóveda activa detectada.
            System.out.println("[VOLCAN-ASSETS] Bóveda activa: " + vaultBase);
        } else {
            // [LOG]: Operando en modo Local-Only.
            System.err.println("[VOLCAN-ASSETS] Bóveda no detectada. Operando en modo Local-Only.");
        }
    }
    // actualizado3/1/26
}