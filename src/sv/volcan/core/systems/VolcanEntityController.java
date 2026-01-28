package sv.volcan.core.systems; // Sincronizado con la ubicación física en src/sv/volcan/bus/

import sv.volcan.state.VolcanStateVault;
import sv.volcan.state.VolcanStateLayout;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Despacho de Comandos Binarios (ABI).
 * DEPENDENCIAS: VolcanStateVault
 * MÉTRICAS: O(1) Command Dispatch
 * 
 * Controlador de entidades basado en ABI. Recibe identificadores de comandos
 * numéricos y ejecuta la lógica asociada directamente sobre el Vault.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanEntityController {

    // IDs de Comandos (ABI del motor)
    public static final int CMD_MOVE_SPRITE = 0x01;
    public static final int CMD_SCALE_SPRITE = 0x02;
    public static final int CMD_RESET_STATE = 0xFF;

    /**
     * Procesa la acción usando la tabla de registros (Vault) y el ID de comando.
     * [NOTA TÉCNICA]: El determinismo depende de que el vault sea la única fuente
     * de entrada.
     */
    public static void dispatch(int commandId, VolcanStateVault vault) {

        switch (commandId) {
            case CMD_MOVE_SPRITE -> {
                // Usamos el TICK actual como semilla para determinismo básico
                int tick = vault.read(VolcanStateLayout.SYS_TICK);

                // Generación de posición pseudo-aleatoria determinista
                // [OBSERVACIÓN]: Operaciones de módulo (%) detectadas.
                // Pendiente de optimización a bitwise AND si se detecta latencia en simulación
                // masiva.
                int nx = (tick * 13) % 800;
                int ny = (tick * 7) % 600;

                // Escritura directa en memoria nativa (State Control)
                vault.write(VolcanStateLayout.PLAYER_X, nx);
                vault.write(VolcanStateLayout.PLAYER_Y, ny);
            }

            case CMD_RESET_STATE -> {
                vault.write(VolcanStateLayout.PLAYER_X, 0);
                vault.write(VolcanStateLayout.PLAYER_Y, 0);
            }

            default -> {
                // Reportar comando desconocido en el registro de flags
                // [VIOLACIÓN PROTOCOLO V2.0]: El código de error debería estar en un Layout de
                // errores.
                vault.write(VolcanStateLayout.SYS_ENGINE_FLAGS, 0x02); // Flag de alerta
            }
        }
    }
    // actualizado3/1/26
}