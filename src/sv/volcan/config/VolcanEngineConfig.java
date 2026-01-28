package sv.volcan.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Zero-Cost Configuration Management
 * DEPENDENCIAS: None (pure Java)
 * MÉTRICAS: Load time <50ms, Runtime overhead 0ns
 * 
 * Configuration loader with static final constants for zero runtime overhead.
 * All configuration is loaded ONCE at class initialization and inlined by JIT.
 * 
 * TÉCNICA:
 * - Static final = JIT inlines as constants (0ns overhead)
 * - Load-once pattern = No file I/O in runtime
 * - Profile-based = Production vs Development configs
 * 
 * GARANTÍA:
 * - Runtime overhead: 0ns (constants are inlined)
 * - Boot overhead: <50ms (one-time file load)
 * - Thread-safe: Immutable after initialization
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-21
 */
public final class VolcanEngineConfig {

    // ═══════════════════════════════════════════════════════════════
    // PROFILE SELECTION
    // ═══════════════════════════════════════════════════════════════

    private static final String PROFILE = System.getProperty(
            "volcan.profile",
            "production" // Default to production
    );

    private static final String CONFIG_FILE = "config/volcan-" + PROFILE + ".properties";

    // ═══════════════════════════════════════════════════════════════
    // LOGGING CONFIGURATION (Zero-cost when disabled)
    // ═══════════════════════════════════════════════════════════════

    public static final boolean LOGGING_ENABLED;
    public static final String LOGGING_LEVEL;
    public static final boolean LOGGING_ASYNC;
    public static final int LOGGING_BUFFER_SIZE;

    // ═══════════════════════════════════════════════════════════════
    // METRICS CONFIGURATION (Minimal overhead with sampling)
    // ═══════════════════════════════════════════════════════════════

    public static final boolean METRICS_ENABLED;
    public static final double METRICS_SAMPLING;
    public static final boolean METRICS_SERVER_ENABLED;
    public static final int METRICS_SERVER_PORT;

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION CONFIGURATION (Zero-cost when disabled)
    // ═══════════════════════════════════════════════════════════════

    public static final boolean VALIDATION_ENABLED;
    public static final boolean VALIDATION_INPUT;
    public static final boolean VALIDATION_MEMORY;

    // ═══════════════════════════════════════════════════════════════
    // BUS CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    public static final int BUS_CAPACITY;
    public static final String BUS_BACKPRESSURE;
    public static final boolean BUS_PADDING_ENABLED;

    // ═══════════════════════════════════════════════════════════════
    // KERNEL CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    public static final int KERNEL_TICK_RATE;
    public static final boolean KERNEL_THREAD_PINNING;
    public static final int KERNEL_THREAD_CORE;

    // ═══════════════════════════════════════════════════════════════
    // MEMORY CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    public static final long MEMORY_VAULT_SIZE;
    public static final int MEMORY_ALIGNMENT;

    // ═══════════════════════════════════════════════════════════════
    // PERFORMANCE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    public static final boolean PERFORMANCE_JIT_WARMUP;
    public static final boolean PERFORMANCE_GC_ZGC;

    // ═══════════════════════════════════════════════════════════════
    // STATIC INITIALIZATION (Executed ONCE at class load)
    // ═══════════════════════════════════════════════════════════════

    static {
        Properties props = new Properties();

        try {
            // Try to load from file
            try (InputStream input = new FileInputStream(CONFIG_FILE)) {
                props.load(input);
                System.out.println("[CONFIG] Loaded profile: " + PROFILE + " from " + CONFIG_FILE);
            }
        } catch (IOException e) {
            System.err.println("[CONFIG] Failed to load " + CONFIG_FILE + ", using defaults");
            // Fallback to production defaults
            loadProductionDefaults(props);
        }

        // ───────────────────────────────────────────────────────────
        // LOGGING
        // ───────────────────────────────────────────────────────────
        LOGGING_ENABLED = Boolean.parseBoolean(props.getProperty("volcan.logging.enabled", "false"));
        LOGGING_LEVEL = props.getProperty("volcan.logging.level", "ERROR");
        LOGGING_ASYNC = Boolean.parseBoolean(props.getProperty("volcan.logging.async", "true"));
        LOGGING_BUFFER_SIZE = Integer.parseInt(props.getProperty("volcan.logging.buffer.size", "1024"));

        // ───────────────────────────────────────────────────────────
        // METRICS
        // ───────────────────────────────────────────────────────────
        METRICS_ENABLED = Boolean.parseBoolean(props.getProperty("volcan.metrics.enabled", "true"));
        METRICS_SAMPLING = Double.parseDouble(props.getProperty("volcan.metrics.sampling", "0.001"));
        METRICS_SERVER_ENABLED = Boolean.parseBoolean(props.getProperty("volcan.metrics.server.enabled", "true"));
        METRICS_SERVER_PORT = Integer.parseInt(props.getProperty("volcan.metrics.server.port", "8080"));

        // ───────────────────────────────────────────────────────────
        // VALIDATION
        // ───────────────────────────────────────────────────────────
        VALIDATION_ENABLED = Boolean.parseBoolean(props.getProperty("volcan.validation.enabled", "false"));
        VALIDATION_INPUT = Boolean.parseBoolean(props.getProperty("volcan.validation.input", "false"));
        VALIDATION_MEMORY = Boolean.parseBoolean(props.getProperty("volcan.validation.memory", "false"));

        // ───────────────────────────────────────────────────────────
        // BUS
        // ───────────────────────────────────────────────────────────
        BUS_CAPACITY = Integer.parseInt(props.getProperty("volcan.bus.capacity", "1024"));
        BUS_BACKPRESSURE = props.getProperty("volcan.bus.backpressure", "DROP");
        BUS_PADDING_ENABLED = Boolean.parseBoolean(props.getProperty("volcan.bus.padding.enabled", "true"));

        // ───────────────────────────────────────────────────────────
        // KERNEL
        // ───────────────────────────────────────────────────────────
        KERNEL_TICK_RATE = Integer.parseInt(props.getProperty("volcan.kernel.tick.rate", "60"));
        KERNEL_THREAD_PINNING = Boolean.parseBoolean(props.getProperty("volcan.kernel.thread.pinning", "true"));
        KERNEL_THREAD_CORE = Integer.parseInt(props.getProperty("volcan.kernel.thread.core", "1"));

        // ───────────────────────────────────────────────────────────
        // MEMORY
        // ───────────────────────────────────────────────────────────
        MEMORY_VAULT_SIZE = Long.parseLong(props.getProperty("volcan.memory.vault.size", "1048576"));
        MEMORY_ALIGNMENT = Integer.parseInt(props.getProperty("volcan.memory.alignment", "64"));

        // ───────────────────────────────────────────────────────────
        // PERFORMANCE
        // ───────────────────────────────────────────────────────────
        PERFORMANCE_JIT_WARMUP = Boolean.parseBoolean(props.getProperty("volcan.performance.jit.warmup", "true"));
        PERFORMANCE_GC_ZGC = Boolean.parseBoolean(props.getProperty("volcan.performance.gc.zgc", "true"));

        // Print configuration summary
        printConfigSummary();
    }

    /**
     * Load production defaults if config file is not found.
     */
    private static void loadProductionDefaults(Properties props) {
        props.setProperty("volcan.logging.enabled", "false");
        props.setProperty("volcan.metrics.sampling", "0.001");
        props.setProperty("volcan.validation.enabled", "false");
    }

    /**
     * Print configuration summary at startup.
     */
    private static void printConfigSummary() {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  VOLCAN ENGINE CONFIGURATION");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  Profile:           " + PROFILE);
        System.out.println("  Logging:           " + (LOGGING_ENABLED ? "ENABLED" : "DISABLED"));
        System.out.println("  Metrics Sampling:  " + (METRICS_SAMPLING * 100) + "%");
        System.out.println("  Validation:        " + (VALIDATION_ENABLED ? "ENABLED" : "DISABLED"));
        System.out.println("  Bus Capacity:      " + BUS_CAPACITY);
        System.out
                .println("  Thread Pinning:    " + (KERNEL_THREAD_PINNING ? "Core " + KERNEL_THREAD_CORE : "DISABLED"));
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    /**
     * Get current profile name.
     */
    public static String getProfile() {
        return PROFILE;
    }

    /**
     * Check if running in production mode.
     */
    public static boolean isProduction() {
        return "production".equals(PROFILE);
    }

    /**
     * Check if running in development mode.
     */
    public static boolean isDevelopment() {
        return "development".equals(PROFILE);
    }

    // Private constructor to prevent instantiation
    private VolcanEngineConfig() {
        throw new AssertionError("Cannot instantiate VolcanEngineConfig");
    }
}
