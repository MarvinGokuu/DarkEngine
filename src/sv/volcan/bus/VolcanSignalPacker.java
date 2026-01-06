package sv.volcan.bus; // Sincronizado con la ruta src/sv/volcan/bus/

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Empaquetado de señales atómicas de 64 bits (Zero-Heap).
 * GARANTÍAS: Operación O(1), determinismo binario, transparencia de endianness.
 * PROHIBICIONES: Prohibido el uso de Boxing (Long vs long) o cualquier llamada
 * al Heap.
 * DOMINIO CRÍTICO: Concurrencia y Serialización Binaria.
 * DISEÑO: [32 bits: Command ID] | [32 bits: Payload/Value]
 */
public final class VolcanSignalPacker {

    private VolcanSignalPacker() {
    } // Sellado: Solo métodos estáticos de utilidad de bits.

    /**
     * Empaqueta un comando y un valor entero en una señal soberana de 64 bits.
     * Cero copias de memoria. Cero objetos. Solo aritmética de bits.
     */
    public static long pack(int commandId, int value) {
        // [INGENIERÍA DURA]: Desplazamiento de bits para construcción de palabra de 64
        // bits.
        // Desplazamos el ID 32 bits a la izquierda y unimos el valor con una máscara OR
        return ((long) commandId << 32) | (value & 0xFFFFFFFFL);
    }

    /**
     * Utilidad para empaquetar comandos de sistema sin valor adicional.
     */
    public static long packCmd(int commandId) {
        // [OPTIMIZACIÓN]: No requiere máscara OR al no haber payload.
        return (long) commandId << 32;
    }

    /**
     * Desempaqueta el ID del comando desde una señal de 64 bits.
     * Extrae los 32 bits superiores que contienen el identificador del comando.
     * 
     * @param signal Señal empaquetada de 64 bits
     * @return ID del comando (32 bits superiores)
     */
    public static int unpackCommandId(long signal) {
        // [INGENIERÍA DURA]: Desplazamiento aritmético de 32 bits a la derecha
        // para extraer los bits superiores que contienen el Command ID.
        return (int) (signal >>> 32);
    }

    /**
     * Desempaqueta el valor/payload desde una señal de 64 bits.
     * Extrae los 32 bits inferiores que contienen el valor asociado.
     * 
     * @param signal Señal empaquetada de 64 bits
     * @return Valor/payload (32 bits inferiores)
     */
    public static int unpackValue(long signal) {
        // [INGENIERÍA DURA]: Máscara AND con 0xFFFFFFFF para extraer
        // solo los 32 bits inferiores, descartando los superiores.
        return (int) (signal & 0xFFFFFFFFL);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // FORMATOS ESPECIALIZADOS AAA+ (Vectores, Coordenadas, GUIDs)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Empaqueta dos floats (32-bit) en un long (64-bit).
     * 
     * FORMATO: [float X: 32 bits][float Y: 32 bits]
     * 
     * PROPÓSITO:
     * - Coordenadas 2D (posición, velocidad)
     * - Datos de física (fuerza, aceleración)
     * - Uniformidad de registro del CPU
     * 
     * MECÁNICA:
     * - Convierte floats a representación binaria (IEEE 754)
     * - Empaqueta X en bits superiores, Y en bits inferiores
     * - Sin pérdida de precisión
     * 
     * LATENCIA: ~5ns (operaciones de bits puras)
     * 
     * @param x Coordenada X (32-bit float)
     * @param y Coordenada Y (32-bit float)
     * @return Vector empaquetado (64-bit long)
     */
    public static long packFloats(float x, float y) {
        int xBits = Float.floatToRawIntBits(x);
        int yBits = Float.floatToRawIntBits(y);
        return ((long) xBits << 32) | (yBits & 0xFFFFFFFFL);
    }

    /**
     * Desempaqueta la coordenada X desde un vector empaquetado.
     * 
     * @param packed Vector empaquetado (64-bit long)
     * @return Coordenada X (32-bit float)
     */
    public static float unpackX(long packed) {
        return Float.intBitsToFloat((int) (packed >>> 32));
    }

    /**
     * Desempaqueta la coordenada Y desde un vector empaquetado.
     * 
     * @param packed Vector empaquetado (64-bit long)
     * @return Coordenada Y (32-bit float)
     */
    public static float unpackY(long packed) {
        return Float.intBitsToFloat((int) (packed & 0xFFFFFFFFL));
    }

    /**
     * Empaqueta coordenadas 3D con precisión mixta.
     * 
     * FORMATO: [X: 16 bits][Y: 16 bits][Z: 32 bits]
     * 
     * PROPÓSITO:
     * - Telemetría espacial (X,Y en rango corto, Z en rango largo)
     * - Optimización de ancho de banda
     * - Datos de órbitas comprimidas
     * 
     * RANGOS:
     * - X: -32768 a 32767 (short)
     * - Y: -32768 a 32767 (short)
     * - Z: -2147483648 a 2147483647 (int)
     * 
     * @param x Coordenada X (short, -32768 a 32767)
     * @param y Coordenada Y (short, -32768 a 32767)
     * @param z Coordenada Z (int, rango completo)
     * @return Coordenadas empaquetadas (64-bit long)
     */
    public static long packCoordinates3D(short x, short y, int z) {
        return ((long) x << 48) | ((long) y << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Desempaqueta coordenada X desde coordenadas 3D.
     * 
     * @param packed Coordenadas empaquetadas
     * @return Coordenada X (short)
     */
    public static short unpack3DX(long packed) {
        return (short) (packed >>> 48);
    }

    /**
     * Desempaqueta coordenada Y desde coordenadas 3D.
     * 
     * @param packed Coordenadas empaquetadas
     * @return Coordenada Y (short)
     */
    public static short unpack3DY(long packed) {
        return (short) (packed >>> 32);
    }

    /**
     * Desempaqueta coordenada Z desde coordenadas 3D.
     * 
     * @param packed Coordenadas empaquetadas
     * @return Coordenada Z (int)
     */
    public static int unpack3DZ(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    /**
     * Empaqueta un GUID de 64 bits.
     * 
     * PROPÓSITO:
     * - Identificadores únicos de entidades
     * - Tracking de paquetes de red
     * - Referencias a objetos espaciales
     * 
     * NOTA: El GUID ya es de 64 bits, este método es para consistencia de API.
     * 
     * @param guid Identificador único (64-bit)
     * @return GUID (sin modificación)
     */
    public static long packGUID(long guid) {
        return guid; // Pass-through para consistencia de API
    }

    /**
     * Empaqueta un puntero de memoria off-heap.
     * 
     * PROPÓSITO:
     * - Referencias a MemorySegment (Project Panama)
     * - Punteros a datos masivos (mapas estelares)
     * - Zero-copy desde fuentes externas
     * 
     * ADVERTENCIA: Solo válido en la misma sesión de JVM.
     * No serializar ni persistir estos punteros.
     * 
     * @param memoryAddress Dirección de memoria (64-bit)
     * @return Puntero empaquetado
     */
    public static long packOffHeapPointer(long memoryAddress) {
        return memoryAddress; // Pass-through, validación en uso
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SEÑALES ATÓMICAS (Operaciones de Bits)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Empaqueta múltiples señales booleanas en un long.
     * 
     * FORMATO: [63 bits de flags][1 bit reservado]
     * 
     * PROPÓSITO:
     * - Estado de comunicación satelital (conectado/desconectado)
     * - Flags de sincronización
     * - Máscaras de eventos
     * 
     * EJEMPLO:
     * - Bit 0: Satélite conectado
     * - Bit 1: Datos válidos
     * - Bit 2: Checksum correcto
     * - Bits 3-62: Flags personalizados
     * 
     * @param flags Máscara de bits (cada bit es una señal)
     * @return Señales empaquetadas
     */
    public static long packAtomicSignals(long flags) {
        return flags;
    }

    /**
     * Obtiene el valor de un bit específico.
     * 
     * @param packed   Señales empaquetadas
     * @param bitIndex Índice del bit (0-62)
     * @return true si el bit está en 1, false si está en 0
     */
    public static boolean getSignalBit(long packed, int bitIndex) {
        return ((packed >>> bitIndex) & 1L) == 1L;
    }

    /**
     * Establece el valor de un bit específico.
     * 
     * @param packed   Señales empaquetadas
     * @param bitIndex Índice del bit (0-62)
     * @param value    Valor a establecer (true = 1, false = 0)
     * @return Señales actualizadas
     */
    public static long setSignalBit(long packed, int bitIndex, boolean value) {
        if (value) {
            return packed | (1L << bitIndex); // Set bit to 1
        } else {
            return packed & ~(1L << bitIndex); // Set bit to 0
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // OPERACIONES ARITMÉTICAS EN HOT-PATH
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Calcula el diferencial entre dos órbitas.
     * 
     * MECÁNICA:
     * - Operación aritmética directa (resta)
     * - Sin copias de memoria
     * - Resultado en registro del CPU
     * 
     * PROPÓSITO:
     * - Telemetría espacial en tiempo real
     * - Detección de desviaciones orbitales
     * - Procesamiento de flujos masivos
     * 
     * @param orbit1 Primera órbita (coordenada empaquetada)
     * @param orbit2 Segunda órbita (coordenada empaquetada)
     * @return Diferencial (orbit1 - orbit2)
     */
    public static long computeOrbitalDifferential(long orbit1, long orbit2) {
        float x1 = unpackX(orbit1);
        float y1 = unpackY(orbit1);
        float x2 = unpackX(orbit2);
        float y2 = unpackY(orbit2);

        float dx = x1 - x2;
        float dy = y1 - y2;

        return packFloats(dx, dy);
    }

    /**
     * Escala un flujo de datos por un porcentaje.
     * 
     * MECÁNICA:
     * - Multiplicación en punto flotante
     * - Operación en registro del CPU
     * - Sin allocations
     * 
     * PROPÓSITO:
     * - Ajuste de telemetría por calibración
     * - Normalización de datos espaciales
     * - Escalado de flujos masivos
     * 
     * @param flowData   Datos de flujo (vector empaquetado)
     * @param percentage Porcentaje (0-100)
     * @return Datos escalados
     */
    public static long scaleFlowPercentage(long flowData, int percentage) {
        float x = unpackX(flowData);
        float y = unpackY(flowData);

        float scale = percentage / 100.0f;
        float scaledX = x * scale;
        float scaledY = y * scale;

        return packFloats(scaledX, scaledY);
    }

    /**
     * Valida y ajusta datos para alineación de página de 4KB.
     * 
     * MECÁNICA:
     * - Verifica que el offset sea múltiplo de 4096
     * - Ajusta automáticamente si es necesario
     * - Reduce TLB misses
     * 
     * PROPÓSITO:
     * - Lectura de datos masivos del espacio
     * - Prevención de fallos de memoria
     * - Optimización de acceso a disco/red
     * 
     * @param dataPointer Puntero a datos off-heap
     * @return Puntero alineado a 4KB
     */
    public static long alignToPage4KB(long dataPointer) {
        long pageSize = 4096L;
        long remainder = dataPointer % pageSize;

        if (remainder == 0) {
            return dataPointer; // Ya alineado
        }

        return dataPointer + (pageSize - remainder);
    }
}