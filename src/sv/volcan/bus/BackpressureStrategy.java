package sv.volcan.bus;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Definir estrategias de manejo de saturación del bus.
 * GARANTÍAS: Comportamiento determinista ante backpressure.
 * DOMINIO CRÍTICO: Concurrencia / Resiliencia
 * 
 * PATRÓN: Strategy Pattern
 * CONCEPTO: Backpressure Handling
 * ROL: Policy Definition
 * 
 * @author MarvinDev
 * @version 2.0
 * @since 2026-01-04
 */
public enum BackpressureStrategy {

    /**
     * Descarta el evento nuevo si el bus está lleno.
     * Uso: Eventos no críticos (ej: partículas, efectos visuales).
     */
    DROP,

    /**
     * Bloquea el thread hasta que haya espacio disponible.
     * Uso: Eventos críticos que no pueden perderse (ej: input del jugador).
     * ADVERTENCIA: Puede causar deadlock si no se consume el bus.
     */
    BLOCK,

    /**
     * Sobrescribe el evento más antiguo con el nuevo.
     * Uso: Eventos donde solo importa el estado más reciente (ej: posición del
     * mouse).
     */
    OVERWRITE
}
