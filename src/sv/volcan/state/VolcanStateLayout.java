// Reading Order: 00000001
package sv.volcan.state;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Memory Layout Constants - Offset Map
 * DEPENDENCIAS: None (Pure Constants)
 * MÉTRICAS: Zero-allocation, Compile-time resolution
 * 
 * Defines memory offsets for WorldStateFrame.
 * Guarantees L1 cache alignment and optimized offsets.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanStateLayout {

    private VolcanStateLayout() {
    } // Sealed: Pure addressing constants only.

    // --- [BLOQUE 0-99]: ESTADO DE ACTORES (HOT DATA - Alta Frecuencia) ---
    public static final int PLAYER_X = 0;
    public static final int PLAYER_Y = 4;
    public static final int PLAYER_DIR = 8;
    public static final int PLAYER_SCORE = 12;

    // --- [BLOQUE 100-199]: CONTROL DE KERNEL (CRITICAL) ---
    public static final int SYS_TICK = 400; // Slot 100
    public static final int SYS_ENGINE_FLAGS = 404; // Slot 101: 0=Running, 1=Alert, 2=Healing
    public static final int SYS_TARGET_FPS = 408; // Slot 102
    public static final int SYS_DELTA_TIME = 412; // Slot 103: dt escalado a int
    public static final int ENTITY_COUNT = 416; // Slot 104: Entidades activas

    // --- [BLOQUE 200-299]: TELEMETRÍA DE HARDWARE ---
    public static final int METRIC_CPU_LOAD = 800; // Slot 200: 0-10000
    public static final int METRIC_RAM_FREE = 804; // Slot 201: MB
    public static final int METRIC_RAM_TOTAL = 808; // Slot 202: MB

    // Alias para compatibilidad con sensores previos
    public static final int SYS_CPU_LOAD = METRIC_CPU_LOAD;
    public static final int SYS_MEM_FREE = METRIC_RAM_FREE;

    // --- [BLOQUE 300-399]: INPUT PIPELINE ---
    public static final int INPUT_MOUSE_X = 1200; // Slot 300
    public static final int INPUT_MOUSE_Y = 1204; // Slot 301
    public static final int INPUT_LAST_SIGNAL = 1208; // Slot 302

    // --- LÍMITES FÍSICOS ---
    /**
     * 1024 slots de 4 bytes = 4096 bytes (4KB).
     * Tamaño óptimo para una página de memoria o línea de caché L1.
     */
    public static final int MAX_SLOTS = 1024;

    // --- [BLOQUE EXTENDIDO]: ENTITY LAYOUT (BYTE OFFSETS) ---
}
