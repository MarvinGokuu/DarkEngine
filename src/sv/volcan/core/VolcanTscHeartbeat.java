package sv.volcan.core; // Sincronizado con la ruta src/sv/volcan/core/

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Sincronización de Alta Precisión (Nano-Time) y Tick
 * Enforcement.
 * DEPENDENCIAS: System.nanoTime()
 * MÉTRICAS: Nanosecond Precision, Zero-Drift
 * 
 * Cronómetro de alta resolución para el loop principal. Controla el presupuesto
 * de tiempo por frame y detecta excesos (Overruns) para mantener 60 FPS
 * estables.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanTscHeartbeat {

    // Presupuesto Industrial: 10ms (100Hz) para simulaciones de misión crítica
    private static final long TICK_BUDGET_NS = 10_000_000L;

    private static long lastTickTime;

    // El acumulador de deriva se reserva para el Hito 4.1 (Time Smoothing)
    @SuppressWarnings("unused")
    private static long driftAccumulator;

    static {
        // Ignición del cronómetro en la carga de la clase (Boot-time)
        lastTickTime = System.nanoTime();
    }

    private VolcanTscHeartbeat() {
    } // Sellado: Solo utilidad de cronometría pura.

    /**
     * Sincroniza el latido del motor.
     * Calcula el delta absoluto y actualiza la referencia temporal.
     * [MECHANICAL SYMPATHY]: Uso de nanoTime() para evitar el jitter del reloj del
     * sistema (Wall-clock).
     */
    public static long sync() {
        long now = System.nanoTime();
        long delta = now - lastTickTime;
        lastTickTime = now;
        return delta;
    }

    /**
     * Valida el presupuesto de tiempo del tick actual.
     * Implementa el [Hito 1.2]: TickBudgetEnforcer.
     * * @param workStartNs El tiempo capturado justo antes de empezar la lógica de
     * simulación.
     * 
     * @return true si el motor está operando dentro de los 10ms permitidos.
     */
    public static boolean checkBudget(long workStartNs) {
        long elapsed = System.nanoTime() - workStartNs;
        return elapsed <= TICK_BUDGET_NS;
    }

    public static long getBudgetNs() {
        return TICK_BUDGET_NS;
    }
}
// actualizado3/1/26