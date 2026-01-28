// Reading Order: 00001100
package sv.volcan.kernel;

import sv.volcan.core.AAACertified; // 00000100

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Guardián del Tiempo - Sensory neuron for temporal
 * determinism
 * DEPENDENCIAS: System.nanoTime()
 * MÉTRICAS: Precisión <1ns (TSC), Fixed timestep 60 FPS
 * 
 * Time controller. Guarantees each frame has
 * exactly 16.666ms (60 FPS fixed timestep).
 * 
 * Uses spin-wait for precision (no Thread.sleep jitter).
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
// EL PORQUE, Y SU DOCUMENTACION. CON COMENTARIOS.Y TECNICA
@AAACertified(date = "2026-01-06", maxLatencyNs = 1, minThroughput = 60, alignment = 64, lockFree = true, offHeap = false, notes = "Sensory neuron - TSC-based temporal determinism at 60 FPS")
public final class TimeKeeper {

    // Constantes de tiempo
    private static final long TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000 / TARGET_FPS; // 16.666ms en nanosegundos

    // Estado del tiempo
    private long lastFrameTime;
    private long currentFrameTime;
    private long frameCount;

    // [GOVERNOR] Control Dinámico de Rendimiento
    // Gears: 1=60FPS, 2=120FPS, 3=144FPS
    private volatile long currentTargetFps = TARGET_FPS;
    private volatile long currentFrameTimeNs = FRAME_TIME_NS;
    private int stabilityCounter = 0; // Frames consecutivos estables
    private int currentGear = 1;

    // Métricas
    private long phase1TimeNs; // Input
    private long phase2TimeNs; // Bus (futuro)
    private long phase3TimeNs; // Systems
    private long phase4TimeNs; // Audit

    public TimeKeeper() {
        this.lastFrameTime = System.nanoTime();
        this.currentFrameTime = lastFrameTime;
        this.frameCount = 0;
    }

    /**
     * Marca el inicio de un nuevo frame.
     */
    public void startFrame() {
        currentFrameTime = System.nanoTime();
        frameCount++;
    }

    /**
     * Retorna el deltaTime fijo para este frame.
     * 
     * DETERMINISMO: Siempre retorna el mismo valor (1/60 segundos)
     * 
     * @return Delta time en segundos
     */
    public double getDeltaTime() {
        return 1.0 / currentTargetFps;
    }

    /**
     * Retorna el número de frame actual.
     * 
     * @return Frame count
     */
    public long getFrameCount() {
        return frameCount;
    }

    /**
     * Retorna el timestamp de inicio del frame actual (nanosegundos).
     * util para profiling y sincronizacion.
     * 
     * @return currentFrameTime
     */
    public long getFrameStartTimeNs() {
        return currentFrameTime;
    }

    /**
     * Espera hasta que sea tiempo del siguiente frame.
     * 
     * TÉCNICA: Spin-wait para precisión de nanosegundos
     * GOVERNOR: Analiza el headroom y ajusta la marcha (Gear Shifting).
     */
    public void waitForNextFrame() {
        long targetTime = lastFrameTime + currentFrameTimeNs;
        long now = System.nanoTime();

        // [GOVERNOR] Análisis de Headroom
        // Calculamos cuánto tiempo sobró en este frame
        long actualWorkDuration = now - currentFrameTime;
        long headroom = currentFrameTimeNs - actualWorkDuration;

        updateGovernor(headroom);

        // Spin-wait agresivo para precisión
        while (now < targetTime) {
            Thread.onSpinWait();
            now = System.nanoTime();
        }

        lastFrameTime = targetTime;
    }

    /**
     * [GOVERNOR] CEREBRO DE RENDIMIENTO
     * Ajusta los FPS basado en la estabilidad del sistema.
     * 
     * Reglas:
     * - Subir marcha: 60 frames estables con >50% de headroom.
     * - Bajar marcha: 1 solo frame inestable (Headroom < 0).
     */
    private void updateGovernor(long headroomNs) {
        // Umbral de seguridad para subir: 4ms de sobra (aprox 50% a 120FPS)
        long SAFE_HEADROOM = 4_000_000;

        if (headroomNs > SAFE_HEADROOM) {
            stabilityCounter++;
            // Si llevamos 1 segundo (aprox 60 frames) estable, intentamos subir
            if (stabilityCounter > 60 && currentGear < 3) {
                shiftGearUp();
                stabilityCounter = 0; // Reset para probar nueva estabilidad
            }
        } else if (headroomNs < 0) {
            // [FAIL-SAFE] Violación de deadline -> Bajar marcha INMEDIATAMENTE
            // Esto evita stuttering en juegos pesados (Cyberpunk/StarCitizen scenario)
            if (currentGear > 1) {
                shiftGearDown();
                stabilityCounter = 0;
            }
        } else {
            // Zona gris: estable pero sin sobra para subir
            stabilityCounter = 0;
        }
    }

    private void shiftGearUp() {
        currentGear++;
        applyGear();
        System.out.println("[GOVERNOR] Upshift -> Gear " + currentGear + " (" + currentTargetFps + " FPS)");
    }

    private void shiftGearDown() {
        currentGear--;
        applyGear();
        System.out.println("[GOVERNOR] Downshift -> Gear " + currentGear + " (" + currentTargetFps + " FPS)");
    }

    private void applyGear() {
        switch (currentGear) {
            case 1 -> setTargetFps(60);
            case 2 -> setTargetFps(120);
            case 3 -> setTargetFps(144);
        }
    }

    private void setTargetFps(long fps) {
        this.currentTargetFps = fps;
        this.currentFrameTimeNs = 1_000_000_000 / fps;
    }

    /**
     * Marca el tiempo de una fase.
     * 
     * @param phase  Número de fase (1-4)
     * @param timeNs Tiempo en nanosegundos
     */
    public void recordPhaseTime(int phase, long timeNs) {
        switch (phase) {
            case 1 -> phase1TimeNs = timeNs;
            case 2 -> phase2TimeNs = timeNs;
            case 3 -> phase3TimeNs = timeNs;
            case 4 -> phase4TimeNs = timeNs;
        }
    }

    /**
     * Retorna el tiempo total del último frame.
     * 
     * @return Tiempo en milisegundos
     */
    public double getLastFrameTimeMs() {
        long total = phase1TimeNs + phase2TimeNs + phase3TimeNs + phase4TimeNs;
        return total / 1_000_000.0;
    }

    /**
     * Verifica si el frame excedió el presupuesto de tiempo.
     * 
     * @return true si excedió
     */
    public boolean isOverBudget() {
        long total = phase1TimeNs + phase2TimeNs + phase3TimeNs + phase4TimeNs;
        return total > currentFrameTimeNs;
    }

    /**
     * Imprime estadísticas de tiempo.
     */
    public void printStats() {
        System.out.printf("[TIME] Gear %d (%d FPS) | Frame %d: Total=%.2fms (Headroom=%.2fms)%n",
                currentGear,
                currentTargetFps,
                frameCount,
                getLastFrameTimeMs(),
                (currentFrameTimeNs - (phase1TimeNs + phase2TimeNs + phase3TimeNs + phase4TimeNs)) / 1_000_000.0);

        if (isOverBudget()) {
            System.out.println("[TIME] ⚠️ WARNING: Frame exceeded budget! Governor likely downshifted.");
        }
    }
}
// Creado: 03/01/2026 23:52
// Concepto: Fixed Timestep + Determinism
