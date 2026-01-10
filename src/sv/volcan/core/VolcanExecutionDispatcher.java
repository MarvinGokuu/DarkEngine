package sv.volcan.core; // Sincronizado con la ruta src/sv/volcan/core/

import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Ejecución de Instrucciones de Bajo Nivel sobre el Estado.
 * DEPENDENCIAS: WorldStateFrame
 * MÉTRICAS: O(1) Execution, Zero-Branching Critical Path
 * 
 * Despachador de ejecución directa. Inyecta valores primitivos directamente
 * en los offsets de memoria del WorldStateFrame sin intermediarios.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanExecutionDispatcher {

    private final WorldStateFrame state;

    public VolcanExecutionDispatcher(WorldStateFrame state) {
        this.state = state;
    }

    /**
     * Inyección Directa Atómica.
     * El autor asume la responsabilidad del offset (Soberanía de Datos).
     * [MECHANICAL SYMPATHY]: Operación inyectada directamente en el pipeline de
     * ejecución.
     */
    public void dispatch(long offset, int value) {
        // En V3.0, el offset ya viene validado por el SovereignInputBuffer.
        // Se elimina cualquier check de límites para evitar stall en el pipeline.
        state.writeInt(offset, value);
    }

    /**
     * Señal de Control de Pulso.
     * Modifica los registros de señalización del Kernel (Flags de control).
     */
    public void triggerSignal(long signalOffset, int signalCode) {
        state.writeInt(signalOffset, signalCode);
        // La alerta no se procesa aquí: el TelemetryMonitor la observa de forma no
        // intrusiva.
    }

    /**
     * Sincronización de Sector por ID (Aritmética de Punteros).
     * [ESTRATEGIA]: Acceso directo al offset del SectorVault omitiendo estructuras
     * de datos pesadas.
     */
    public void syncSector(int entityIndex) {
        // Implementación futura: El índice se multiplica por el Stride directamente
        // para hallar el offset físico.
    }
}
// actualizado3/1/26
