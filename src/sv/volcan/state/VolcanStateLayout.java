package sv.volcan.state;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Definición física del direccionamiento (ABI) del StateVault.
 * GARANTÍAS: Alineación para caché L1, offsets de 4-bytes (Integer slots),
 * direccionamiento O(1).
 * PROHIBICIONES: Prohibido duplicar offsets; prohibido usar índices negativos.
 * DOMINIO CRÍTICO: State / Memoria
 */
public final class VolcanStateLayout {

    private VolcanStateLayout() {
    } // Sellado: Solo constantes de direccionamiento puro.

    // --- [BLOQUE 0-99]: ESTADO DE ACTORES (HOT DATA - Alta Frecuencia) ---
    public static final int PLAYER_X = 0;
    public static final int PLAYER_Y = 1;
    public static final int PLAYER_DIR = 2;
    public static final int PLAYER_SCORE = 3;

    // --- [BLOQUE 100-199]: CONTROL DE KERNEL (CRITICAL) ---
    public static final int SYS_TICK = 100;
    public static final int SYS_ENGINE_FLAGS = 101; // Bits: 0=Running, 1=Alert, 2=Healing
    public static final int SYS_TARGET_FPS = 102;
    public static final int SYS_DELTA_TIME = 103; // dt escalado a int para evitar float-drift
    public static final int ENTITY_COUNT = 104; // Número de entidades activas en el mundo

    // --- [BLOQUE 200-299]: TELEMETRÍA DE HARDWARE ---
    public static final int METRIC_CPU_LOAD = 200; // 0-10000 (0.00% - 100.00%)
    public static final int METRIC_RAM_FREE = 201; // en MB
    public static final int METRIC_RAM_TOTAL = 202; // en MB

    // Alias para compatibilidad con sensores previos
    public static final int SYS_CPU_LOAD = 200;
    public static final int SYS_MEM_FREE = 201;

    // --- [BLOQUE 300-399]: INPUT PIPELINE ---
    public static final int INPUT_MOUSE_X = 300;
    public static final int INPUT_MOUSE_Y = 301;
    public static final int INPUT_LAST_SIGNAL = 302;

    // --- LÍMITES FÍSICOS ---
    /**
     * 1024 slots de 4 bytes = 4096 bytes (4KB).
     * Tamaño óptimo para una página de memoria o línea de caché L1.
     */
    public static final int MAX_SLOTS = 1024;

    // --- [BLOQUE EXTENDIDO]: ENTITY LAYOUT (BYTE OFFSETS) ---
    // Requerido por VolcanSectorManager
    public static final int SECTOR_ID_OFFSET = 56;
}
