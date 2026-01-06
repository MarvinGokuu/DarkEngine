package sv.volcan.core.systems; // Sincronizado con la ubicación física en src/sv/volcan/bus/

import sv.volcan.state.VolcanStateVault;
import sv.volcan.state.VolcanStateLayout;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Ejecución de lógica de entidades mediante comandos binarios
 * (ABI del Motor).
 * GARANTÍAS: Determinismo puro, Zero-Allocation, O(1) Dispatching.
 * PROHIBICIONES: Prohibido instanciar objetos o usar tipos String en el
 * despacho de comandos.
 * DOMINIO CRÍTICO: Lógica de Ejecución y Tiempo Real.
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

                // Escritura directa en memoria nativa (Soberanía del State)
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