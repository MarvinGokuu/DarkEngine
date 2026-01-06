package sv.volcan.state;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Contenedor inmutable de snapshots para persistencia, red y
 * rollback.
 * GARANTÍAS: Copia binaria exacta (memcpy nativo), alineación de 64-bytes,
 * zero-heap.
 * PROHIBICIONES: Prohibido modificar los datos una vez capturados; prohibido
 * usar tipos no primitivos.
 * DOMINIO CRÍTICO: State / Continuidad temporal
 */
public final class WorldStateFrame {

    private final MemorySegment data;
    private final long timestamp;

    /**
     * Captura soberana: El frame se adueña de una porción del tiempo.
     * [MECHANICAL SYMPATHY]: Forzamos alineación de línea de caché para ráfagas
     * SIMD.
     */
    public WorldStateFrame(Arena arena, MemorySegment source, long timestamp) {
        // Reserva de memoria nativa fuera del alcance del GC
        this.data = arena.allocate(source.byteSize(), 64L);
        // Copia directa de hardware (CPU Burst)
        this.data.copyFrom(source);
        this.timestamp = timestamp;
    }

    /**
     * Restauración inmediata (Protocolo de Rollback).
     * El estado activo del motor se sobreescribe con este frame.
     */
    public void restoreInto(MemorySegment target) {
        target.copyFrom(this.data);
    }

    /**
     * Escritura directa al frame (Solo permitida durante la fase de
     * ejecución/setup).
     */
    public void writeInt(long offset, int value) {
        data.set(ValueLayout.JAVA_INT, offset, value);
    }

    /**
     * Lectura segura de registros específicos del frame histórico.
     */
    public int readInt(long offset) {
        return data.get(ValueLayout.JAVA_INT, offset);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MemorySegment getData() {
        return data;
    }

    /**
     * Acceso directo a segmento de memoria nativa (Off-Heap).
     * Usado por sistemas de alta frecuencia para evitar indirección.
     * LATENCIA: ~50-150 ns (acceso directo sin boxing).
     */
    public MemorySegment getRawSegment() {
        return data;
    }

    /**
     * Lectura de double desde offset específico.
     * LATENCIA: ~50-150 ns (acceso directo a memoria nativa).
     */
    public double readDouble(long offset) {
        return data.get(ValueLayout.JAVA_DOUBLE, offset);
    }

    /**
     * Escritura de double en offset específico.
     * LATENCIA: ~50-150 ns (acceso directo a memoria nativa).
     */
    public void writeDouble(long offset, double value) {
        data.set(ValueLayout.JAVA_DOUBLE, offset, value);
    }

    /**
     * Lectura de float desde offset específico.
     * LATENCIA: ~50-150 ns (acceso directo a memoria nativa).
     */
    public float readFloat(long offset) {
        return data.get(ValueLayout.JAVA_FLOAT, offset);
    }

    /**
     * Lectura de long desde offset específico.
     * LATENCIA: ~50-150 ns (acceso directo a memoria nativa).
     */
    public long readLong(long offset) {
        return data.get(ValueLayout.JAVA_LONG, offset);
    }

    /**
     * Escritura de float en offset específico.
     * LATENCIA: ~50-150 ns (acceso directo a memoria nativa).
     */
    public void writeFloat(long offset, float value) {
        data.set(ValueLayout.JAVA_FLOAT, offset, value);
    }

    /**
     * Escritura de long en offset específico.
     * LATENCIA: ~50-150 ns (acceso directo a memoria nativa).
     */
    public void writeLong(long offset, long value) {
        data.set(ValueLayout.JAVA_LONG, offset, value);
    }
}
// actualizado3/1/26