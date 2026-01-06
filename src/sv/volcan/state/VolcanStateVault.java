package sv.volcan.state;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * AUTORIDAD: Volcan (Fase 3.2)
 * ESTADO: Purificado - Zero Strings / Zero GC.
 * RESPONSABILIDAD: Almacenamiento Off-Heap alineado para el Kernel.
 * GARANTÍAS: Acceso O(1), alineación de línea de caché (64-byte), inmunidad al
 * GC.
 * DOMINIO CRÍTICO: Persistencia de Estado / Memoria Nativa
 */
public final class VolcanStateVault {

    private final MemorySegment storage;
    private static final long ALIGNMENT = 64L; // Alineación para evitar False Sharing

    /**
     * @param arena    El ciclo de vida de la memoria (Shared o Confined).
     * @param maxSlots Cantidad de registros definidos en VolcanStateLayout.
     */
    public VolcanStateVault(Arena arena, int maxSlots) {
        // Reserva memoria nativa alineada.
        // [INGENIERÍA]: El layout de memoria es contiguo y predecible para el hardware.
        this.storage = arena.allocate(
                ValueLayout.JAVA_INT.byteSize() * maxSlots,
                ALIGNMENT);
    }

    /**
     * Acceso O(1) puro. Sin parsing, sin hashing.
     * 
     * @param slotIndex Índice obtenido de VolcanStateLayout.
     * @param value     Valor escalar a inyectar.
     */
    public void write(int slotIndex, int value) {
        storage.setAtIndex(ValueLayout.JAVA_INT, (long) slotIndex, value);
    }

    public int read(int slotIndex) {
        return storage.getAtIndex(ValueLayout.JAVA_INT, (long) slotIndex);
    }

    /**
     * Soporte para Long (8 bytes) para telemetría de alta precisión.
     */
    public long readLong(int slotIndex) {
        return storage.getAtIndex(ValueLayout.JAVA_LONG, (long) slotIndex / 2);
    }

    /**
     * Snapshot instantáneo para el sistema de Rollback o Telemetría Externa.
     * [MECHANICAL SYMPATHY]: Copia por bloque a nivel de hardware.
     */
    public void snapshotTo(MemorySegment destination) {
        destination.copyFrom(storage);
    }

    public MemorySegment getRawSegment() {
        return this.storage;
    }
}
// actualizado3/1/26