package sv.volcan.core; // Sincronizado con la ruta src/sv/volcan/core/

/**
 * AUTORIDAD: Sovereign
 * RESPONSABILIDAD: Mapa de offsets para el acceso atómico y lineal a datos de
 * entidad.
 * GARANTÍAS: Alineación de 64-bytes (Cache Line Alignment), acceso O(1),
 * compatibilidad SIMD.
 * PROHIBICIONES: Prohibido modificar offsets en runtime; prohibido usar tipos
 * no primitivos.
 * DOMINIO CRÍTICO: Memoria
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

    private EntityLayout() {
    } // Sellado: Solo constantes de direccionamiento.
}
// actualizado3/1/26