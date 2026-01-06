package sv.volcan.bus;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Lane especializado con métricas y backpressure.
 * GARANTÍAS: Observabilidad completa, manejo determinista de saturación.
 * PROHIBICIONES: Prohibido crear objetos en hot-path durante offer/poll.
 * DOMINIO CRÍTICO: Arquitectura / Observabilidad
 * 
 * PATRÓN: Decorator Pattern + Strategy Pattern
 * CONCEPTO: Separation of Concerns + Metrics Collection
 * ROL: Specialized Event Channel
 * 
 * @author MarvinDev
 * @version 2.0
 * @since 2026-01-04
 */
public final class VolcanEventLane {

    private final IEventBus bus;
    private final String name;
    private final VolcanEventType type;
    private final BackpressureStrategy strategy;

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTRICAS (Zero-Allocation Counters)
    // ═══════════════════════════════════════════════════════════════════════

    private long totalOffered = 0;
    private long totalAccepted = 0;
    private long totalDropped = 0;
    private long totalPolled = 0;

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Crea un lane especializado con estrategia de backpressure.
     * 
     * @param name     Nombre del lane (ej: "Input", "Network")
     * @param type     Tipo de eventos que maneja
     * @param bus      Implementación del bus subyacente
     * @param strategy Estrategia de backpressure
     */
    public VolcanEventLane(String name, VolcanEventType type, IEventBus bus, BackpressureStrategy strategy) {
        this.name = name;
        this.type = type;
        this.bus = bus;
        this.strategy = strategy;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OPERACIONES PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Ofrece un evento al lane con manejo de backpressure.
     * 
     * @param event Evento codificado
     * @return true si el evento fue aceptado
     */
    public boolean offer(long event) {
        totalOffered++;

        boolean accepted = bus.offer(event);

        if (accepted) {
            totalAccepted++;
            return true;
        }

        // Manejo de backpressure según estrategia
        switch (strategy) {
            case DROP:
                totalDropped++;
                return false;

            case BLOCK:
                // [ADVERTENCIA]: Esto puede causar deadlock si el bus no se consume
                while (!bus.offer(event)) {
                    Thread.onSpinWait(); // Hint al CPU para reducir consumo
                }
                totalAccepted++;
                return true;

            case OVERWRITE:
                // Descartamos el evento más antiguo y reintentamos
                bus.poll(); // Elimina el más antiguo
                totalDropped++;
                boolean retryAccepted = bus.offer(event);
                if (retryAccepted) {
                    totalAccepted++;
                }
                return retryAccepted;

            default:
                totalDropped++;
                return false;
        }
    }

    /**
     * Consume el siguiente evento del lane.
     * 
     * @return El evento o -1 si está vacío
     */
    public long poll() {
        long event = bus.poll();
        if (event != -1) {
            totalPolled++;
        }
        return event;
    }

    /**
     * Lee el siguiente evento sin consumirlo.
     * 
     * @return El evento o -1 si está vacío
     */
    public long peek() {
        return bus.peek();
    }

    /**
     * Procesa todos los eventos disponibles en el lane.
     * 
     * @param processor Función que procesa cada evento
     * @return Número de eventos procesados
     */
    public int processAll(java.util.function.LongConsumer processor) {
        int count = 0;
        long event;
        while ((event = poll()) != -1) {
            processor.accept(event);
            count++;
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OBSERVABILIDAD
    // ═══════════════════════════════════════════════════════════════════════

    public String getName() {
        return name;
    }

    public VolcanEventType getType() {
        return type;
    }

    public int size() {
        return bus.size();
    }

    public int capacity() {
        return bus.capacity();
    }

    public int remainingCapacity() {
        return bus.remainingCapacity();
    }

    public boolean isEmpty() {
        return bus.isEmpty();
    }

    public boolean isFull() {
        return bus.isFull();
    }

    public long getTotalOffered() {
        return totalOffered;
    }

    public long getTotalAccepted() {
        return totalAccepted;
    }

    public long getTotalDropped() {
        return totalDropped;
    }

    public long getTotalPolled() {
        return totalPolled;
    }

    /**
     * Retorna la tasa de aceptación del lane (0.0 a 1.0).
     * 
     * @return Porcentaje de eventos aceptados
     */
    public double getAcceptanceRate() {
        if (totalOffered == 0)
            return 1.0;
        return (double) totalAccepted / totalOffered;
    }

    /**
     * Retorna la tasa de descarte del lane (0.0 a 1.0).
     * 
     * @return Porcentaje de eventos descartados
     */
    public double getDropRate() {
        if (totalOffered == 0)
            return 0.0;
        return (double) totalDropped / totalOffered;
    }

    /**
     * Limpia el lane y resetea métricas.
     */
    public void clear() {
        bus.clear();
        totalOffered = 0;
        totalAccepted = 0;
        totalDropped = 0;
        totalPolled = 0;
    }

    /**
     * Genera un reporte de estado del lane.
     * 
     * @return String con métricas del lane
     */
    public String getStatusReport() {
        return String.format(
                "[LANE: %s] Type=%s | Size=%d/%d | Offered=%d | Accepted=%d | Dropped=%d | Rate=%.2f%%",
                name, type, size(), capacity(), totalOffered, totalAccepted, totalDropped, getAcceptanceRate() * 100);
    }
}
