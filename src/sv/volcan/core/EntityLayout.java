package sv.volcan.core; // Sincronizado con la ruta src/sv/volcan/core/

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Mapa de offsets (Layout) para entidades en memoria Off-Heap.
 * DEPENDENCIAS: Ninguna (Definición Estática)
 * MÉTRICAS: Alineación 64-byte (Cache Line), Access O(1)
 * 
 * Define la estructura física de una Entidad en memoria nativa.
 * Garantiza alineación perfecta para prevenir False Sharing y permitir
 * acceso vectorizado (SIMD).
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class EntityLayout {

    // Estructura de 64 bytes por entidad (Alineación perfecta con L1 Cache Line)
    public static final long STRIDE = 64L;

    // Posición (8 bytes cada uno - double/long)
    public static final long X_OFFSET = 0L;
    public static final long Y_OFFSET = 8L;

    // Velocidad (Requerido por MovementSystem)
    public static final long VX_OFFSET = 16L;
    public static final long VY_OFFSET = 24L;

    // Metadatos y Visualización
    public static final long ID_OFFSET = 32L;
    public static final long STATE_FLAGS = 40L;
    public static final long GLOW_ALPHA = 48L; // Requerido por SpriteSystem

    // Spatial Management (Bytes 56-64)
    public static final long SECTOR_ID_OFFSET = 56L; // Requerido por VolcanSectorManager

    private EntityLayout() {
    } // Sellado: Solo constantes de direccionamiento.
}
// actualizado3/1/26