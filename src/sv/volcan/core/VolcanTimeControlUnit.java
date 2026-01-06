package sv.volcan.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Gestión de persistencia temporal (Snapshots) sin alocación
 * de objetos.
 * GARANTÍAS: Zero-GC en captura/rollback, límite de memoria físico,
 * determinismo de bits.
 * PROHIBICIONES: Prohibido usar colecciones de Java (List/Deque), prohibido
 * crear objetos en el loop.
 * DOMINIO CRÍTICO: State / Tiempo
 */
public final class VolcanTimeControlUnit {

    private final MemorySegment timeSlab; // Un solo bloque contiguo de memoria nativa (Off-Heap)
    private final long frameSize;
    private final int maxFrames;
    private int writeIndex = 0;

    /**
     * @param arena     Arena confinada para la sesión de ejecución.
     * @param frameSize Tamaño exacto en bytes de un WorldStateFrame.
     * @param maxFrames Capacidad del búfer circular para el historial.
     */
    public VolcanTimeControlUnit(Arena arena, long frameSize, int maxFrames) {
        this.frameSize = frameSize;
        this.maxFrames = maxFrames;

        // [INGENIERÍA DURA]: Reservamos el slab completo al inicio para evitar
        // fragmentación.
        // Alineación a 64 bytes para maximizar el ancho de banda del bus de memoria.
        this.timeSlab = arena.allocate(frameSize * maxFrames, 64L);
    }

    /**
     * Captura el estado presente sobreescribiendo el frame más antiguo.
     * Operación Wait-free mediante copia directa de memoria nativa.
     */
    public void capture(MemorySegment activeState) {
        // Cálculo de offset O(1)
        long offset = (long) writeIndex * frameSize;

        // Creamos una vista (slice) sin alocación en el Heap
        MemorySegment targetFrame = timeSlab.asSlice(offset, frameSize);

        // [MECHANICAL SYMPATHY]: Copia de bajo nivel directa al silicio.
        targetFrame.copyFrom(activeState);

        // Avance del puntero circular
        writeIndex = (writeIndex + 1) % maxFrames;
    }

    /**
     * Restaura el estado anterior del motor.
     * Revierte la soberanía de datos al instante previo capturado.
     */
    public void rollback(MemorySegment activeState) {
        // Retroceso del índice en el búfer circular
        writeIndex = (writeIndex - 1 + maxFrames) % maxFrames;
        long offset = (long) writeIndex * frameSize;

        MemorySegment historicFrame = timeSlab.asSlice(offset, frameSize);

        // Restauración atómica de bits
        activeState.copyFrom(historicFrame);
    }
}
// actualizado3/1/26
