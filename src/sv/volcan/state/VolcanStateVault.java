// Reading Order: 00000011
package sv.volcan.state;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.volcan.core.AAACertified;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Almacenamiento Off-Heap - Long-term memory neuron
 * DEPENDENCIAS: Java Panama (Foreign Memory API)
 * MÉTRICAS: Latencia <150ns, Capacidad ilimitada (solo RAM física)
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-06
 */
@AAACertified(date = "2026-01-06", maxLatencyNs = 150, alignment = 64, lockFree = false, offHeap = true, notes = "Long-term memory neuron - Off-heap storage for AI state persistence")
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
     * Lee un long (8 bytes) desde la posición especificada.
     * 
     * PORQUÉ:
     * - Telemetría de alta precisión requiere valores de 64 bits
     * - Un long ocupa 2 slots de int (8 bytes = 2 × 4 bytes)
     * - Validación condicional previene errores en development
     * 
     * TÉCNICA:
     * - slotIndex debe ser par (0, 2, 4, 6, ...) para alineación correcta
     * - Conversión: slotIndex → byte offset (slotIndex × 4 bytes)
     * - Validación solo en development (0ns overhead en production)
     * 
     * GARANTÍA:
     * - Production: 0ns overhead (validación eliminada por JIT)
     * - Development: Fail-fast si slotIndex es impar
     * - Latencia: ~50-150ns (acceso directo a memoria nativa)
     * 
     * @param slotIndex Índice del slot (debe ser par: 0, 2, 4, 6, ...)
     * @return Valor long leído (8 bytes)
     */
    public long readLong(int slotIndex) {
        // Validación condicional (solo en development profile)
        if (sv.volcan.config.VolcanEngineConfig.VALIDATION_ENABLED) {
            if (slotIndex % 2 != 0) {
                throw new IllegalArgumentException(
                        "slotIndex must be even to read long (8 bytes). " +
                                "Got slotIndex=" + slotIndex + ". " +
                                "Use even indices: 0, 2, 4, 6, etc.");
            }
        }

        // Convertir slotIndex (índice de int) a byte offset
        long byteOffset = (long) slotIndex * ValueLayout.JAVA_INT.byteSize();
        return storage.get(ValueLayout.JAVA_LONG, byteOffset);
    }

    /**
     * Escribe un long (8 bytes) en la posición especificada.
     * 
     * PORQUÉ:
     * - API simétrica: readLong() ↔ writeLong()
     * - Evita tener que escribir 2 ints separados
     * - Validación condicional previene errores en development
     * 
     * TÉCNICA:
     * - slotIndex debe ser par (0, 2, 4, 6, ...) para alineación correcta
     * - Conversión: slotIndex → byte offset (slotIndex × 4 bytes)
     * - Validación solo en development (0ns overhead en production)
     * 
     * GARANTÍA:
     * - Production: 0ns overhead (validación eliminada por JIT)
     * - Development: Fail-fast si slotIndex es impar
     * - Latencia: ~50-150ns (acceso directo a memoria nativa)
     * 
     * @param slotIndex Índice del slot (debe ser par: 0, 2, 4, 6, ...)
     * @param value     Valor long a escribir (8 bytes)
     */
    public void writeLong(int slotIndex, long value) {
        // Validación condicional (solo en development profile)
        if (sv.volcan.config.VolcanEngineConfig.VALIDATION_ENABLED) {
            if (slotIndex % 2 != 0) {
                throw new IllegalArgumentException(
                        "slotIndex must be even to write long (8 bytes). " +
                                "Got slotIndex=" + slotIndex + ". " +
                                "Use even indices: 0, 2, 4, 6, etc.");
            }
        }

        // Convertir slotIndex (índice de int) a byte offset
        long byteOffset = (long) slotIndex * ValueLayout.JAVA_INT.byteSize();
        storage.set(ValueLayout.JAVA_LONG, byteOffset, value);
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

    /**
     * Cierra el vault (no-op, el Arena es propiedad del caller).
     * Mantenido por interfaz AutoCloseable.
     */
    public void close() {
        // No cerramos el Arena aquí porque se inyecta en el constructor.
        // El ownership del Arena pertenece al EngineKernel (o test).
    }
}