package sv.volcan.kernel;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GUARDIÁN DEL TIEMPO (Time Keeper)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * AUTORIDAD: Sovereign Kernel
 * RESPONSABILIDAD: Mantener el tiempo determinista del motor.
 * 
 * CONCEPTO: Fixed Timestep
 * PRINCIPIO: Determinism
 * ROL: Game Engine Architect
 * 
 * GARANTÍAS:
 * - Fixed timestep (16.6ms para 60 FPS)
 * - deltaTime consistente
 * - Medición de tiempo por fase
 * 
 * DOMINIO CRÍTICO: Kernel / Tiempo
 * 
 * @author MarvinDev
 * @version 1.0
 * @since 2026-01-03
 */
public final class TimeKeeper {

    // Constantes de tiempo
    private static final long TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000 / TARGET_FPS; // 16.666ms en nanosegundos
    private static final double FRAME_TIME_SECONDS = 1.0 / TARGET_FPS; // 0.01666 segundos

    // Estado del tiempo
    private long lastFrameTime;
    private long currentFrameTime;
    private long frameCount;

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
        return FRAME_TIME_SECONDS;
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
     */
    public void waitForNextFrame() {
        long targetTime = lastFrameTime + FRAME_TIME_NS;
        long now = System.nanoTime();

        // Spin-wait agresivo para precisión
        while (now < targetTime) {
            Thread.onSpinWait();
            now = System.nanoTime();
        }

        lastFrameTime = targetTime;
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
        return total > FRAME_TIME_NS;
    }

    /**
     * Imprime estadísticas de tiempo.
     */
    public void printStats() {
        System.out.printf("[TIME] Frame %d: Total=%.2fms (P1=%.2f P2=%.2f P3=%.2f P4=%.2f)%n",
                frameCount,
                getLastFrameTimeMs(),
                phase1TimeNs / 1_000_000.0,
                phase2TimeNs / 1_000_000.0,
                phase3TimeNs / 1_000_000.0,
                phase4TimeNs / 1_000_000.0);

        if (isOverBudget()) {
            System.out.println("[TIME] ⚠️ WARNING: Frame exceeded budget!");
        }
    }
}
// Creado: 03/01/2026 23:52
// Autor: MarvinDev
// Concepto: Fixed Timestep + Determinism
