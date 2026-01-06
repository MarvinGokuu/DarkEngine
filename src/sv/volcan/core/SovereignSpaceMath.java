package sv.volcan.core;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Utilidad matemática pura para el cálculo de sectores y
 * empaquetado espacial.
 * GARANTÍAS: Operaciones bitwise de ciclo único, sin asignación de objetos,
 * determinismo 64-bit.
 * DOMINIO CRÍTICO: Matemáticas / Espacio
 * 
 * USO: Centraliza la lógica de 'Sector Size' para evitar números mágicos.
 */
public final class SovereignSpaceMath {

    // [CONFIGURACIÓN MAESTRA DE REJILLA]
    // Shift de 7 bits = División por 128 (2^7)
    public static final int SECTOR_SHIFT = 7;
    public static final int SECTOR_SIZE = 1 << SECTOR_SHIFT; // 128

    // Máscaras para empaquetado 3D (21-21-22)
    private static final long MASK_21_BITS = 0x1FFFFFL; // 2,097,152 valores
    private static final long MASK_22_BITS = 0x3FFFFFL; // 4,194,304 valores

    private SovereignSpaceMath() {
    } // Sellado

    /**
     * Convierte una coordenada de mundo (float) a índice de sector (int).
     * [OPTIMIZACIÓN]: Usa bit-shifting en lugar de división.
     */
    public static int getSectorIndex(float worldCoord) {
        // Casting a int trunca (floor positivo), luego shift.
        // Equivalente a: floor(worldCoord / 128)
        return (int) worldCoord >> SECTOR_SHIFT;
    }

    /**
     * Empaqueta coordenadas 2D en una llave espacial de 64 bits.
     * Compatible con la arquitectura actual (High-32: X | Low-32: Y).
     */
    public static long packKey2D(int sx, int sy) {
        // High-32: X | Low-32: Y (masked to avoid sign extension issues)
        return ((long) sx << 32) | (sy & 0xFFFFFFFFL);
    }

    /**
     * [FUTURO AAA] Empaquetado 3D de alta densidad.
     * Distribución: X(21) | Z(21) | Y(22)
     */
    public static long packKey3D(int sx, int sy, int sz) {
        long px = (sx & MASK_21_BITS);
        long pz = (sz & MASK_21_BITS);
        long py = (sy & MASK_22_BITS);

        // Layout: [X: 21 bits] [Z: 21 bits] [Y: 22 bits]
        return (px << 43) | (pz << 22) | py;
    }

    /**
     * Recupera el tamaño del sector para validaciones o debug.
     */
    public static int getSectorSize() {
        return SECTOR_SIZE;
    }
}
