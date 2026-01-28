// Reading Order: 00000010
package sv.volcan.bus;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Catálogo centralizado de comandos del sistema.
 * GARANTÍAS: IDs únicos, organización por dominio, extensibilidad.
 * PROHIBICIONES: Prohibido usar IDs duplicados o fuera del rango del tipo.
 * DOMINIO CRÍTICO: Arquitectura / Comunicación
 * 
 * PATRÓN: Registry Pattern + Constant Pool
 * CONCEPTO: Command Catalog
 * ROL: Event Type Definition
 * 
 * FORMATO DE COMMAND ID (32 bits):
 * [16 bits: Type Base (0xX000)] [16 bits: Specific Command (0x0XXX)]
 * 
 * @author MarvinDev
 * @version 2.0
 * @since 2026-01-04
 */
public final class VolcanSignalCommands {

    private VolcanSignalCommands() {
        // Sellado: Solo constantes estáticas
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADMIN COMMANDS (HUMAN INTERFACE) - CORE COMMANDS
    // ═══════════════════════════════════════════════════════════════════════

    /** ENCENDIDO (MarvinDevOn) */
    public static final int ADMIN_CMD_ON = 0x9001;

    /** APAGADO (MarvinDevoff) */
    public static final int ADMIN_CMD_OFF = 0x9002;

    /** INSTALADOR (MarvinDevinstaller) */
    public static final int ADMIN_CMD_INSTALLER = 0x9003;

    /** RECUPERACIÓN (MarvinDevsv) */
    public static final int ADMIN_CMD_RECOVERY = 0x9004;

    // ═══════════════════════════════════════════════════════════════════════
    // INPUT COMMANDS (0x1000 - 0x1FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Tecla Maestra ENCENDIDO (1) */
    public static final int INPUT_KEY_MASTER_ON = 0x31; // ASCII '1'

    /** Tecla Maestra APAGADO (0) */
    public static final int INPUT_KEY_MASTER_OFF = 0x30; // ASCII '0'

    /** Tecla presionada (payload: keyCode) */
    public static final int INPUT_KEY_DOWN = 0x1001;

    /** Tecla liberada (payload: keyCode) */
    public static final int INPUT_KEY_UP = 0x1002;

    /** Mouse movido (payload: packed X,Y) */
    public static final int INPUT_MOUSE_MOVE = 0x1003;

    /** Click del mouse (payload: button ID) */
    public static final int INPUT_MOUSE_CLICK = 0x1004;

    /** Gamepad botón presionado (payload: button ID) */
    public static final int INPUT_GAMEPAD_BUTTON = 0x1005;

    // ═══════════════════════════════════════════════════════════════════════
    // NETWORK COMMANDS (0x2000 - 0x2FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Sincronizar estado de entidad (payload: entity ID) */
    public static final int NET_SYNC_ENTITY = 0x2001;

    /** Paquete recibido (payload: packet ID) */
    public static final int NET_PACKET_RECEIVED = 0x2002;

    /** Conexión establecida (payload: client ID) */
    public static final int NET_CONNECTION_ESTABLISHED = 0x2003;

    /** Conexión perdida (payload: client ID) */
    public static final int NET_CONNECTION_LOST = 0x2004;

    // ═══════════════════════════════════════════════════════════════════════
    // SYSTEM COMMANDS (0x3000 - 0x3FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Spawn de entidad (payload: entity type) */
    public static final int SYS_ENTITY_SPAWN = 0x3001;

    /** Destruir entidad (payload: entity ID) */
    public static final int SYS_ENTITY_DESTROY = 0x3002;

    /** Mover entidad (payload: entity ID) */
    public static final int SYS_ENTITY_MOVE = 0x3003;

    /** Pausar motor (payload: 0) */
    public static final int SYS_ENGINE_PAUSE = 0x3100;

    /** Reanudar motor (payload: 0) */
    public static final int SYS_ENGINE_RESUME = 0x3101;

    /** Shutdown del motor (payload: exit code) */
    public static final int SYS_ENGINE_SHUTDOWN = 0x3102;

    // ═══════════════════════════════════════════════════════════════════════
    // AUDIO COMMANDS (0x4000 - 0x4FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Reproducir sonido (payload: sound ID) */
    public static final int AUDIO_PLAY_SOUND = 0x4001;

    /** Detener sonido (payload: sound ID) */
    public static final int AUDIO_STOP_SOUND = 0x4002;

    /** Cambiar volumen (payload: volume 0-100) */
    public static final int AUDIO_SET_VOLUME = 0x4003;

    // ═══════════════════════════════════════════════════════════════════════
    // PHYSICS COMMANDS (0x5000 - 0x5FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Aplicar fuerza (payload: entity ID) */
    public static final int PHYSICS_APPLY_FORCE = 0x5001;

    /** Colisión detectada (payload: entity ID) */
    public static final int PHYSICS_COLLISION = 0x5002;

    /** Cambiar gravedad (payload: gravity value) */
    public static final int PHYSICS_SET_GRAVITY = 0x5003;

    // ═══════════════════════════════════════════════════════════════════════
    // RENDER COMMANDS (0x6000 - 0x6FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Cambiar shader (payload: shader ID) */
    public static final int RENDER_SET_SHADER = 0x6001;

    /** Actualizar textura (payload: texture ID) */
    public static final int RENDER_UPDATE_TEXTURE = 0x6002;

    /** Cambiar cámara (payload: camera ID) */
    public static final int RENDER_SET_CAMERA = 0x6003;

    // ═══════════════════════════════════════════════════════════════════════
    // SPATIAL COMMANDS (0x7000 - 0x7FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Actualización orbital recibida (payload: orbit ID) */
    public static final int SPATIAL_ORBITAL_UPDATE = 0x7001;

    /** Telemetría recibida desde satélite (payload: telemetry ID) */
    public static final int SPATIAL_TELEMETRY_RECEIVED = 0x7002;

    /** Sincronización con satélite (payload: satellite ID) */
    public static final int SPATIAL_SATELLITE_SYNC = 0x7003;

    /** Calcular diferencial orbital (payload: orbit pair ID) */
    public static final int SPATIAL_COMPUTE_DIFFERENTIAL = 0x7010;

    /** Escalar flujo de datos (payload: percentage) */
    public static final int SPATIAL_SCALE_FLOW = 0x7011;

    /** Alinear datos a página (payload: alignment type) */
    public static final int SPATIAL_ALIGN_PAGE = 0x7012;

    /** Inyección desde edge computing (payload: buffer ID) */
    public static final int SPATIAL_EDGE_INJECT = 0x7020;

    /** Activar modo zero-copy (payload: 0=off, 1=on) */
    public static final int SPATIAL_ZERO_COPY_MODE = 0x7021;

    // ═══════════════════════════════════════════════════════════════════════
    // MEMORY COMMANDS (0x8000 - 0x8FFF)
    // ═══════════════════════════════════════════════════════════════════════

    /** Asignar memoria off-heap (payload: size in bytes) */
    public static final int MEMORY_ALLOC_OFFHEAP = 0x8001;

    /** Liberar memoria off-heap (payload: pointer ID) */
    public static final int MEMORY_FREE_OFFHEAP = 0x8002;

    /** Mapear segmento de memoria (payload: segment ID) */
    public static final int MEMORY_MAP_SEGMENT = 0x8003;

    /** Alinear a página de 4KB (payload: pointer ID) */
    public static final int MEMORY_ALIGN_PAGE_4KB = 0x8010;

    /** Alinear a página de 2MB (payload: pointer ID) */
    public static final int MEMORY_ALIGN_PAGE_2MB = 0x8011;

    /** Habilitar prefetch (payload: buffer ID) */
    public static final int MEMORY_PREFETCH_ENABLE = 0x8020;

    /** Deshabilitar prefetch (payload: buffer ID) */
    public static final int MEMORY_PREFETCH_DISABLE = 0x8021;

    // ═══════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Retorna el nombre legible de un comando.
     * 
     * @param commandId ID del comando
     * @return Nombre del comando o "UNKNOWN"
     */
    public static String getCommandName(int commandId) {
        switch (commandId) {
            // Input
            case INPUT_KEY_DOWN:
                return "INPUT_KEY_DOWN";
            case INPUT_KEY_UP:
                return "INPUT_KEY_UP";
            case INPUT_MOUSE_MOVE:
                return "INPUT_MOUSE_MOVE";
            case INPUT_MOUSE_CLICK:
                return "INPUT_MOUSE_CLICK";
            case INPUT_GAMEPAD_BUTTON:
                return "INPUT_GAMEPAD_BUTTON";

            // Network
            case NET_SYNC_ENTITY:
                return "NET_SYNC_ENTITY";
            case NET_PACKET_RECEIVED:
                return "NET_PACKET_RECEIVED";
            case NET_CONNECTION_ESTABLISHED:
                return "NET_CONNECTION_ESTABLISHED";
            case NET_CONNECTION_LOST:
                return "NET_CONNECTION_LOST";

            // System
            case SYS_ENTITY_SPAWN:
                return "SYS_ENTITY_SPAWN";
            case SYS_ENTITY_DESTROY:
                return "SYS_ENTITY_DESTROY";
            case SYS_ENTITY_MOVE:
                return "SYS_ENTITY_MOVE";
            case SYS_ENGINE_PAUSE:
                return "SYS_ENGINE_PAUSE";
            case SYS_ENGINE_RESUME:
                return "SYS_ENGINE_RESUME";
            case SYS_ENGINE_SHUTDOWN:
                return "SYS_ENGINE_SHUTDOWN";

            // Audio
            case AUDIO_PLAY_SOUND:
                return "AUDIO_PLAY_SOUND";
            case AUDIO_STOP_SOUND:
                return "AUDIO_STOP_SOUND";
            case AUDIO_SET_VOLUME:
                return "AUDIO_SET_VOLUME";

            // Physics
            case PHYSICS_APPLY_FORCE:
                return "PHYSICS_APPLY_FORCE";
            case PHYSICS_COLLISION:
                return "PHYSICS_COLLISION";
            case PHYSICS_SET_GRAVITY:
                return "PHYSICS_SET_GRAVITY";

            // Render
            case RENDER_SET_SHADER:
                return "RENDER_SET_SHADER";
            case RENDER_UPDATE_TEXTURE:
                return "RENDER_UPDATE_TEXTURE";
            case RENDER_SET_CAMERA:
                return "RENDER_SET_CAMERA";

            // Spatial
            case SPATIAL_ORBITAL_UPDATE:
                return "SPATIAL_ORBITAL_UPDATE";
            case SPATIAL_TELEMETRY_RECEIVED:
                return "SPATIAL_TELEMETRY_RECEIVED";
            case SPATIAL_SATELLITE_SYNC:
                return "SPATIAL_SATELLITE_SYNC";
            case SPATIAL_COMPUTE_DIFFERENTIAL:
                return "SPATIAL_COMPUTE_DIFFERENTIAL";
            case SPATIAL_SCALE_FLOW:
                return "SPATIAL_SCALE_FLOW";
            case SPATIAL_ALIGN_PAGE:
                return "SPATIAL_ALIGN_PAGE";
            case SPATIAL_EDGE_INJECT:
                return "SPATIAL_EDGE_INJECT";
            case SPATIAL_ZERO_COPY_MODE:
                return "SPATIAL_ZERO_COPY_MODE";

            // Memory
            case MEMORY_ALLOC_OFFHEAP:
                return "MEMORY_ALLOC_OFFHEAP";
            case MEMORY_FREE_OFFHEAP:
                return "MEMORY_FREE_OFFHEAP";
            case MEMORY_MAP_SEGMENT:
                return "MEMORY_MAP_SEGMENT";
            case MEMORY_ALIGN_PAGE_4KB:
                return "MEMORY_ALIGN_PAGE_4KB";
            case MEMORY_ALIGN_PAGE_2MB:
                return "MEMORY_ALIGN_PAGE_2MB";
            case MEMORY_PREFETCH_ENABLE:
                return "MEMORY_PREFETCH_ENABLE";
            case MEMORY_PREFETCH_DISABLE:
                return "MEMORY_PREFETCH_DISABLE";

            // Admin Commands
            case ADMIN_CMD_ON:
                return "MarvinDevOn";
            case ADMIN_CMD_OFF:
                return "MarvinDevoff";
            case ADMIN_CMD_INSTALLER:
                return "MarvinDevinstaller";
            case ADMIN_CMD_RECOVERY:
                return "MarvinDevsv";

            default:
                return "UNKNOWN_COMMAND";
        }
    }
}
