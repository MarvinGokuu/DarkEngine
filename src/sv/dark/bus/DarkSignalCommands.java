// Reading Order: 00000010
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * Centralized system command catalog.
 *
 * <p>Defines unique 32-bit IDs for all events, mapped into domains.
 * Format: [16 bits: Type Base (0xX000)] [16 bits: Specific Command (0x0XXX)].
 * Designed as a zero-allocation constant pool for the event bus.
 *
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(
    date         = "2026-01-04",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment    = 64,
    lockFree     = true,
    offHeap      = false,
    notes        = "Constant pool for event bus — zero allocation"
)
public final class DarkSignalCommands {

    /** Utility class — no instances. */
    private DarkSignalCommands() {
        throw new AssertionError("DarkSignalCommands is a static utility class");
    }

    // -------------------------------------------------------------------------
    // Admin Commands (Human Interface / Core Commands)
    // -------------------------------------------------------------------------

    /** Power On command (MarvinDevOn). */
    public static final int ADMIN_CMD_ON = 0x9001;

    /** Power Off command (MarvinDevoff). */
    public static final int ADMIN_CMD_OFF = 0x9002;

    /** Installer trigger (MarvinDevinstaller). */
    public static final int ADMIN_CMD_INSTALLER = 0x9003;

    /** Recovery mode trigger (MarvinDevsv). */
    public static final int ADMIN_CMD_RECOVERY = 0x9004;

    // -------------------------------------------------------------------------
    // Input Commands (0x1000 - 0x1FFF)
    // -------------------------------------------------------------------------

    /** Master Key ON (ASCII '1'). */
    public static final int INPUT_KEY_MASTER_ON = 0x31; 

    /** Master Key OFF (ASCII '0'). */
    public static final int INPUT_KEY_MASTER_OFF = 0x30;

    /** Key pressed event (payload: keyCode). */
    public static final int INPUT_KEY_DOWN = 0x1001;

    /** Key released event (payload: keyCode). */
    public static final int INPUT_KEY_UP = 0x1002;

    /** Mouse movement event (payload: packed X,Y). */
    public static final int INPUT_MOUSE_MOVE = 0x1003;

    /** Mouse click event (payload: button ID). */
    public static final int INPUT_MOUSE_CLICK = 0x1004;

    /** Gamepad button pressed (payload: button ID). */
    public static final int INPUT_GAMEPAD_BUTTON = 0x1005;

    // -------------------------------------------------------------------------
    // Network Commands (0x2000 - 0x2FFF)
    // -------------------------------------------------------------------------

    /** Synchronize entity state (payload: entity ID). */
    public static final int NET_SYNC_ENTITY = 0x2001;

    /** Packet received event (payload: packet ID). */
    public static final int NET_PACKET_RECEIVED = 0x2002;

    /** Connection established event (payload: client ID). */
    public static final int NET_CONNECTION_ESTABLISHED = 0x2003;

    /** Connection lost event (payload: client ID). */
    public static final int NET_CONNECTION_LOST = 0x2004;

    // -------------------------------------------------------------------------
    // System Commands (0x3000 - 0x3FFF)
    // -------------------------------------------------------------------------

    /** Spawn entity request (payload: entity type). */
    public static final int SYS_ENTITY_SPAWN = 0x3001;

    /** Destroy entity request (payload: entity ID). */
    public static final int SYS_ENTITY_DESTROY = 0x3002;

    /** Move entity request (payload: entity ID). */
    public static final int SYS_ENTITY_MOVE = 0x3003;

    /** Pause engine execution (payload: 0). */
    public static final int SYS_ENGINE_PAUSE = 0x3100;

    /** Resume engine execution (payload: 0). */
    public static final int SYS_ENGINE_RESUME = 0x3101;

    /** Engine shutdown request (payload: exit code). */
    public static final int SYS_ENGINE_SHUTDOWN = 0x3102;

    /** Rollback engine state (payload: 0). */
    public static final int SYS_ENGINE_ROLLBACK = 0x3103;

    /** Terminate logging asynchronously (Poison Pill). */
    public static final int SYS_TERMINATE_LOG_SIGNAL = 0x3104;

    // -------------------------------------------------------------------------
    // Audio Commands (0x4000 - 0x4FFF)
    // -------------------------------------------------------------------------

    /** Play sound request (payload: sound ID). */
    public static final int AUDIO_PLAY_SOUND = 0x4001;

    /** Stop sound request (payload: sound ID). */
    public static final int AUDIO_STOP_SOUND = 0x4002;

    /** Adjust global volume (payload: volume 0-100). */
    public static final int AUDIO_SET_VOLUME = 0x4003;

    // -------------------------------------------------------------------------
    // Physics Commands (0x5000 - 0x5FFF)
    // -------------------------------------------------------------------------

    /** Apply physical force (payload: entity ID). */
    public static final int PHYSICS_APPLY_FORCE = 0x5001;

    /** Collision detected event (payload: entity ID). */
    public static final int PHYSICS_COLLISION = 0x5002;

    /** Set global gravity scale (payload: gravity value). */
    public static final int PHYSICS_SET_GRAVITY = 0x5003;

    // -------------------------------------------------------------------------
    // Render Commands (0x6000 - 0x6FFF)
    // -------------------------------------------------------------------------

    /** Change active shader (payload: shader ID). */
    public static final int RENDER_SET_SHADER = 0x6001;

    /** Update texture mapping (payload: texture ID). */
    public static final int RENDER_UPDATE_TEXTURE = 0x6002;

    /** Change active camera (payload: camera ID). */
    public static final int RENDER_SET_CAMERA = 0x6003;

    // -------------------------------------------------------------------------
    // Spatial Commands (0x7000 - 0x7FFF)
    // -------------------------------------------------------------------------

    /** Orbital update received (payload: orbit ID). */
    public static final int SPATIAL_ORBITAL_UPDATE = 0x7001;

    /** Telemetry received from satellite (payload: telemetry ID). */
    public static final int SPATIAL_TELEMETRY_RECEIVED = 0x7002;

    /** Synchronize with satellite clock/state (payload: satellite ID). */
    public static final int SPATIAL_SATELLITE_SYNC = 0x7003;

    /** Compute orbital differential (payload: orbit pair ID). */
    public static final int SPATIAL_COMPUTE_DIFFERENTIAL = 0x7010;

    /** Scale data flow throughput (payload: percentage). */
    public static final int SPATIAL_SCALE_FLOW = 0x7011;

    /** Align data to hardware page boundary (payload: alignment type). */
    public static final int SPATIAL_ALIGN_PAGE = 0x7012;

    /** Data injection from edge computing nodes (payload: buffer ID). */
    public static final int SPATIAL_EDGE_INJECT = 0x7020;

    /** Toggle zero-copy memory mode (payload: 0=off, 1=on). */
    public static final int SPATIAL_ZERO_COPY_MODE = 0x7021;

    // -------------------------------------------------------------------------
    // Memory Commands (0x8000 - 0x8FFF)
    // -------------------------------------------------------------------------

    /** Allocate off-heap memory region (payload: size in bytes). */
    public static final int MEMORY_ALLOC_OFFHEAP = 0x8001;

    /** Free allocated off-heap memory (payload: pointer ID). */
    public static final int MEMORY_FREE_OFFHEAP = 0x8002;

    /** Map a specific memory segment (payload: segment ID). */
    public static final int MEMORY_MAP_SEGMENT = 0x8003;

    /** Align memory to 4KB page boundary (payload: pointer ID). */
    public static final int MEMORY_ALIGN_PAGE_4KB = 0x8010;

    /** Align memory to 2MB huge-page boundary (payload: pointer ID). */
    public static final int MEMORY_ALIGN_PAGE_2MB = 0x8011;

    /** Enable hardware prefetching for buffer (payload: buffer ID). */
    public static final int MEMORY_PREFETCH_ENABLE = 0x8020;

    /** Disable hardware prefetching for buffer (payload: buffer ID). */
    public static final int MEMORY_PREFETCH_DISABLE = 0x8021;

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Translates a command ID into a human-readable string for debugging.
     * 
     * @param commandId The 32-bit command identifier.
     * @return The string representation of the command, or "UNKNOWN_COMMAND" if not found.
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
            case SYS_ENGINE_ROLLBACK:
                return "SYS_ENGINE_ROLLBACK";

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
