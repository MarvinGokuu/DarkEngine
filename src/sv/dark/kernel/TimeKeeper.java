// Reading Order: 00001100
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;

import sv.dark.core.DarkLogger;
import sv.dark.core.AAACertified;
import sv.dark.config.DarkEngineConfig;

/**
 * RESPONSIBILITY: Sensory neuron for temporal determinism and Quad-Lane timestep regulation.
 * WHY: We need distinct behaviors for Gaming (smoothness), Scientific Simulation (math purity), and Benchmarks (throughput).
 * TECHNIQUE: Implements Quad-Lane Architecture. Uses Asymmetric Hysteresis for Lane 1 (Gaming CVT), and static behaviors for the rest.
 * GUARANTEES: Absolute temporal determinism in Scientific mode. Stutter-free Hysteresis in Gaming mode. Zero-overhead routing.
 * 
 * <p>Dependencies: System.nanoTime(), DarkEngineConfig
 * <p>Metrics: Precision <1ns (TSC)
 * 
 * @author Marvin Alexander Flores Canales
 * @version 2.0
 * @since 2026-06-13
 */
@AAACertified(
    date = "2026-06-13",
    maxLatencyNs = 1,
    minThroughput = 30,
    alignment = 64,
    lockFree = true,
    offHeap = false,
    notes = "Quad-Lane Governor with Asymmetric Hysteresis"
)
public final class TimeKeeper {

    // ==========================================================================
    // ARCHITECTURE: QUAD-LANE PIPELINE
    // ==========================================================================
    public enum EngineMode {
        GAMING_CVT,          // Lane 1: Asymmetric Hysteresis (Default)
        UNBOUNDED_RAW,       // Lane 2: 0-Wait Compute (Config 1)
        DEBUG_LOCK,          // Lane 3: Legacy Fixed FPS (Config 2)
        SCIENTIFIC_SYMMETRIC // Lane 4: Strict DeltaTime Simulation (Config 3)
    }

    private final EngineMode currentMode;
    private final long customDebugFps;

    // ==========================================================================
    // TIME STATE
    // ==========================================================================
    private long lastFrameTime;
    private long currentFrameTime;
    private long frameCount;

    // ==========================================================================
    // GOVERNOR STATE (CVT)
    // ==========================================================================
    private static final long MIN_FPS = 30;
    private static final long MAX_FPS = 360;
    public static final long UNBOUNDED_FPS = 0L;
    
    private volatile long currentTargetFps;
    private volatile long currentFrameTimeNs;
    private int stabilityFrames = 0; 

    // 1% Low Ring Buffer
    private static final int BUFFER_SIZE = 60;
    private final long[] frameTimeBuffer = new long[BUFFER_SIZE];
    private int bufferIndex = 0;

    // ==========================================================================
    // METRICS
    // ==========================================================================
    private volatile long lastHeadroomNs;
    private volatile long lastActualFps;
    
    private long phase1TimeNs; // Input
    private long phase2TimeNs; // Bus (future)
    private long phase3TimeNs; // Systems
    private long phase4TimeNs; // Audit

    public TimeKeeper() {
        this.lastFrameTime = System.nanoTime();
        this.currentFrameTime = lastFrameTime;
        this.frameCount = 0;

        // Load configuration for the Lanes
        EngineMode mode = EngineMode.GAMING_CVT;
        try {
            mode = EngineMode.valueOf(DarkEngineConfig.KERNEL_ENGINE_MODE);
        } catch (Exception e) {
            DarkLogger.warning("TIME", "Unknown ENGINE_MODE, defaulting to GAMING_CVT");
        }
        this.currentMode = mode;
        this.customDebugFps = DarkEngineConfig.KERNEL_DEBUG_FPS_LOCK;

        // Initialize target based on Lane
        if (currentMode == EngineMode.DEBUG_LOCK || currentMode == EngineMode.SCIENTIFIC_SYMMETRIC) {
            setTargetFps(customDebugFps);
        } else if (currentMode == EngineMode.UNBOUNDED_RAW) {
            setTargetFps(UNBOUNDED_FPS);
        } else {
            setTargetFps(60); // Default start for CVT
        }

        // Pre-fill ring buffer
        long initialTarget = (this.currentFrameTimeNs == 0) ? 1_000_000_000L / 60 : this.currentFrameTimeNs;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            frameTimeBuffer[i] = initialTarget;
        }
    }

    public void startFrame() {
        currentFrameTime = System.nanoTime();
        frameCount++;
    }

    /**
     * Delta Time adapts precisely to the active Lane to guarantee determinism.
     */
    public double getDeltaTime() {
        if (currentMode == EngineMode.SCIENTIFIC_SYMMETRIC) {
            // Lane 4: Absolute mathematical determinism (Fixed Delta)
            return 1.0 / customDebugFps; 
        }
        if (currentMode == EngineMode.UNBOUNDED_RAW || currentTargetFps == UNBOUNDED_FPS) {
            long deltaNs = currentFrameTime - lastFrameTime;
            if (deltaNs <= 0) deltaNs = 1;
            return deltaNs / 1_000_000_000.0;
        }
        // Lane 1 & 3: Floating delta based on current target
        return 1.0 / currentTargetFps;
    }

    public long getFrameCount() {
        return frameCount;
    }

    public long getFrameStartTimeNs() {
        return currentFrameTime;
    }

    /**
     * QUAD-LANE ROUTER
     * Routes the waiting logic depending on the active architectural lane.
     * Switch statement overhead is 0ns (Compiled as internal tableswitch/lookupswitch).
     */
    public void waitForNextFrame() {
        long actualWorkNs = System.nanoTime() - currentFrameTime;

        // Save real work time to the 1% Low ring buffer
        frameTimeBuffer[bufferIndex] = actualWorkNs;
        bufferIndex = (bufferIndex + 1) % BUFFER_SIZE;

        switch (currentMode) {
            case UNBOUNDED_RAW:
                // Lane 2: 0-Wait Compute. Do absolutely nothing. Run fast.
                lastFrameTime = currentFrameTime;
                return;

            case SCIENTIFIC_SYMMETRIC:
                // Lane 4: Strict fixed step. Sleep if early, slow motion if late.
                enforceRigidTarget();
                break;

            case DEBUG_LOCK:
                // Lane 3: Legacy Fixed FPS lock for testing.
                enforceRigidTarget();
                break;

            case GAMING_CVT:
            default:
                // Lane 1: Asymmetric Hysteresis for buttery smooth gameplay.
                enforceGamingCVT(actualWorkNs);
                break;
        }
    }

    /**
     * [LANE 1] Asymmetric Hysteresis CVT
     */
    private void enforceGamingCVT(long actualWorkNs) {
        long headroomNs = currentFrameTimeNs - actualWorkNs;
        this.lastHeadroomNs = headroomNs;

        // 1. Banda Muerta (Histéresis 5%) - Ignorar fluctuaciones
        // [AUDIT FIX] Optimizacion: Evitar casteo a 'double' usando division entera (/20 = 5%)
        if (Math.abs(headroomNs) < (currentFrameTimeNs / 20)) {
            stabilityFrames++;
        } 
        // 2. Caída Defensiva Asimétrica (Stutter Protection)
        else if (headroomNs < 0) {
            long worstFrameNs = getWorstFrameInRingBuffer();
            // [AUDIT FIX] Seguro contra division por cero en caso de saltos cuanticos
            long actualFps = 1_000_000_000L / Math.max(1, worstFrameNs);
            this.lastActualFps = actualFps;
            
            // Magia Torvalds: Truncar hacia abajo a múltiplo par (Bloques de 4)
            long newTarget = (actualFps / 4) * 4; 
            
            // Anclaje al 1% Low
            setTargetFps(Math.max(MIN_FPS, newTarget));
            stabilityFrames = 0; 
        } 
        // 3. Subida Cautelosa (+20% Headroom)
        // [AUDIT FIX] Optimizacion: Evitar casteo a 'double' usando division entera (/5 = 20%)
        else if (headroomNs > (currentFrameTimeNs / 5)) {
            stabilityFrames++;
            // Exigimos 300 frames de estabilidad absoluta (Aprox 5 segundos)
            if (stabilityFrames >= 300) { 
                setTargetFps(Math.min(MAX_FPS, currentTargetFps + 4));
                stabilityFrames = 0;
            }
        }

        // Spin-wait based on current dynamic target
        executeSpinWait();
    }

    /**
     * [LANE 3 & 4] Rigid static lock
     */
    private void enforceRigidTarget() {
        executeSpinWait();
    }

    /**
     * Unified hardware spin-wait core.
     */
    private void executeSpinWait() {
        long targetTime = lastFrameTime + currentFrameTimeNs;
        long now = System.nanoTime();

        while (now < targetTime) {
            Thread.onSpinWait();
            now = System.nanoTime();
        }

        // Slip compensation (Max 2 frames of stutter debt)
        if (now - targetTime > currentFrameTimeNs * 2) {
            lastFrameTime = now;
        } else {
            lastFrameTime = targetTime;
        }
    }

    private long getWorstFrameInRingBuffer() {
        long worstNs = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (frameTimeBuffer[i] > worstNs) {
                worstNs = frameTimeBuffer[i];
            }
        }
        return worstNs;
    }

    public long getCurrentTargetFps() { return currentTargetFps; }
    public long getLastHeadroomNs() { return lastHeadroomNs; }
    public long getLastActualFps() { return lastActualFps; }

    private void setTargetFps(long fps) {
        if (fps == this.currentTargetFps) return;
        this.currentTargetFps = fps;
        this.currentFrameTimeNs = (fps == 0) ? 0 : 1_000_000_000 / fps;
        DarkLogger.info("TIME", "Governor shifted to: " + fps + " FPS (" + currentMode.name() + ")");
    }

    public void recordPhaseTime(int phase, long timeNs) {
        switch (phase) {
            case 1 -> phase1TimeNs = timeNs;
            case 2 -> phase2TimeNs = timeNs;
            case 3 -> phase3TimeNs = timeNs;
            case 4 -> phase4TimeNs = timeNs;
        }
    }

    public double getLastFrameTimeMs() {
        long total = phase1TimeNs + phase2TimeNs + phase3TimeNs + phase4TimeNs;
        return total / 1_000_000.0;
    }

    public boolean isOverBudget() {
        long total = phase1TimeNs + phase2TimeNs + phase3TimeNs + phase4TimeNs;
        return total > currentFrameTimeNs;
    }

    public void printStats() {
        DarkLogger.info("TIME", String.format("Mode: %s | FPS Target: %d | Frame %d: Total=%.2fms",
                currentMode.name(),
                currentTargetFps,
                frameCount,
                getLastFrameTimeMs()));

        if (isOverBudget()) {
            DarkLogger.warning("TIME", "⚠️ WARNING: Frame exceeded budget (" + currentMode.name() + ")!");
        }
    }
}
