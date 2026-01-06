package sv.volcan.core; // Sincronizado con la ruta física src/sv/volcan/core/systems/

import java.lang.foreign.MemorySegment;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Representación espacial de un segmento del WorldStateFrame
 * (Sliver).
 * GARANTÍAS: Acceso por punteros nativos, cero copias de arrays (Zero-Copy),
 * alineación de datos contigua.
 * PROHIBICIONES: Prohibido usar Lists, ArrayLists o cualquier objeto de la Java
 * Collections API.
 * DOMINIO CRÍTICO: Espacial / Memoria
 */
public final class VolcanSector {

    private final long sectorHash;
    private final MemorySegment sectorMemory;
    private final int maxEntities;
    private int activeCount;

    /**
     * El sector recibe una porción (Sliver) de la memoria nativa total del Vault.
     * [MECHANICAL SYMPATHY]: No se reserva nueva memoria, se mapea una referencia
     * existente.
     */
    public VolcanSector(long sectorHash, MemorySegment rawVaultSegment, int maxEntities) {
        this.sectorHash = sectorHash;
        this.maxEntities = maxEntities;
        this.sectorMemory = rawVaultSegment; // Referencia al bloque de memoria dedicado (Zero-Copy)
        this.activeCount = 0;
    }

    /**
     * En el modelo Soberano, "registrar" es simplemente incrementar el puntero
     * de entidades activas dentro del segmento de memoria pre-asignado.
     */
    public void registerEntity() {
        if (activeCount < maxEntities) {
            activeCount++;
        }
    }

    /**
     * Decrementa el conteo de entidades activas al salir del sector.
     */
    public void unregisterEntity() {
        if (activeCount > 0) {
            activeCount--;
        }
    }

    public MemorySegment getSectorMemory() {
        return sectorMemory;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public long getSectorHash() {
        return sectorHash;
    }
}
// actualizado3/1/26