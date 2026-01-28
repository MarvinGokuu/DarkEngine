package sv.volcan.bus;

import java.util.HashMap;
import java.util.Map;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Orquestador Central de Eventos y Despacho Multi-Lane.
 * DEPENDENCIAS: VolcanEventLane, VolcanRingBus, BackpressureStrategy
 * MÉTRICAS: Zero-Allocation Dispatch, Routing Determinista
 * 
 * Fachada principal del sistema de eventos. Gestiona múltiples canales (lanes)
 * especializados para diferentes tipos de tráfico (Network, Physics, System,
 * etc.)
 * con estrategias de backpressure independientes.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanEventDispatcher {

    private final Map<String, VolcanEventLane> lanes;

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Crea un dispatcher vacío.
     * Los lanes deben ser registrados manualmente con registerLane().
     */
    public VolcanEventDispatcher() {
        this.lanes = new HashMap<>();
    }

    /**
     * Crea un dispatcher con lanes predefinidos.
     * 
     * @param busSize Tamaño de cada bus (power of 2)
     */
    public static VolcanEventDispatcher createDefault(int busSize) {
        VolcanEventDispatcher dispatcher = new VolcanEventDispatcher();

        // Lane de Input: DROP (eventos no críticos, alta frecuencia)
        dispatcher.registerLane(
                "Input",
                VolcanEventType.INPUT,
                new VolcanRingBus(busSize),
                BackpressureStrategy.DROP);

        // Lane de Network: BLOCK (eventos críticos, no pueden perderse)
        dispatcher.registerLane(
                "Network",
                VolcanEventType.NETWORK,
                new VolcanRingBus(busSize),
                BackpressureStrategy.BLOCK);

        // Lane de System: BLOCK (eventos críticos del motor)
        dispatcher.registerLane(
                "System",
                VolcanEventType.SYSTEM,
                new VolcanRingBus(busSize),
                BackpressureStrategy.BLOCK);

        // Lane de Audio: DROP (eventos no críticos)
        dispatcher.registerLane(
                "Audio",
                VolcanEventType.AUDIO,
                new VolcanRingBus(busSize),
                BackpressureStrategy.DROP);

        // Lane de Physics: OVERWRITE (solo importa el estado más reciente)
        dispatcher.registerLane(
                "Physics",
                VolcanEventType.PHYSICS,
                new VolcanRingBus(busSize),
                BackpressureStrategy.OVERWRITE);

        // Lane de Render: DROP (eventos visuales no críticos)
        dispatcher.registerLane(
                "Render",
                VolcanEventType.RENDER,
                new VolcanRingBus(busSize),
                BackpressureStrategy.DROP);

        return dispatcher;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGISTRO DE LANES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Registra un lane especializado.
     * 
     * @param name     Nombre del lane
     * @param type     Tipo de eventos
     * @param bus      Implementación del bus
     * @param strategy Estrategia de backpressure
     */
    public void registerLane(String name, VolcanEventType type, IEventBus bus, BackpressureStrategy strategy) {
        VolcanEventLane lane = new VolcanEventLane(name, type, bus, strategy);
        lanes.put(name, lane);
    }

    /**
     * Obtiene un lane por nombre.
     * 
     * @param name Nombre del lane
     * @return Lane o null si no existe
     */
    public VolcanEventLane getLane(String name) {
        return lanes.get(name);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DISPATCH DE EVENTOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Despacha un evento a un lane específico.
     * 
     * @param laneName Nombre del lane
     * @param event    Evento codificado
     * @return true si el evento fue aceptado
     */
    public boolean dispatch(String laneName, long event) {
        VolcanEventLane lane = lanes.get(laneName);
        if (lane == null) {
            // Lane no existe, descartamos el evento
            return false;
        }
        return lane.offer(event);
    }

    /**
     * Despacha un evento al lane correspondiente según su tipo.
     * 
     * @param event Evento codificado (debe incluir tipo en el command ID)
     * @return true si el evento fue aceptado
     */
    public boolean dispatchAuto(long event) {
        int commandId = VolcanSignalPacker.unpackCommandId(event);
        VolcanEventType type = VolcanEventType.fromCommandId(commandId);

        // Buscar lane por tipo
        for (VolcanEventLane lane : lanes.values()) {
            if (lane.getType() == type) {
                return lane.offer(event);
            }
        }

        return false; // No hay lane para este tipo
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROCESAMIENTO DE EVENTOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Procesa todos los eventos de un lane específico.
     * 
     * @param laneName  Nombre del lane
     * @param processor Función que procesa cada evento
     * @return Número de eventos procesados
     */
    public int processLane(String laneName, java.util.function.LongConsumer processor) {
        VolcanEventLane lane = lanes.get(laneName);
        if (lane == null) {
            return 0;
        }
        return lane.processAll(processor);
    }

    /**
     * Procesa todos los eventos de todos los lanes en orden de prioridad.
     * Orden: System > Network > Input > Physics > Audio > Render
     * 
     * @param processor Función que procesa cada evento
     * @return Número total de eventos procesados
     */
    public int processAll(java.util.function.LongConsumer processor) {
        int total = 0;

        // Orden de prioridad
        String[] priorityOrder = { "System", "Network", "Input", "Physics", "Audio", "Render" };

        for (String laneName : priorityOrder) {
            total += processLane(laneName, processor);
        }

        return total;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OBSERVABILIDAD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Imprime el estado de todos los lanes.
     */
    public void printStatus() {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  VOLCAN EVENT DISPATCHER - STATUS REPORT");
        System.out.println("═══════════════════════════════════════════════════════");

        for (VolcanEventLane lane : lanes.values()) {
            System.out.println(lane.getStatusReport());
        }

        System.out.println("═══════════════════════════════════════════════════════");
    }

    /**
     * Limpia todos los lanes.
     */
    public void clearAll() {
        for (VolcanEventLane lane : lanes.values()) {
            lane.clear();
        }
    }

    /**
     * Retorna el número total de eventos pendientes en todos los lanes.
     * 
     * @return Suma de eventos en todos los lanes
     */
    public int getTotalPendingEvents() {
        int total = 0;
        for (VolcanEventLane lane : lanes.values()) {
            total += lane.size();
        }
        return total;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GRACEFUL SHUTDOWN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cierre seguro del dispatcher con liberación de todos los lanes.
     * 
     * PROPÓSITO:
     * - Cerrar todos los buses de prioridad
     * - Validar que todos los eventos fueron procesados
     * - Liberar referencias a los buses
     * 
     * POSTCONDICIONES:
     * - Todos los lanes cerrados
     * - No hay eventos pendientes
     */
    public void shutdown() {
        System.out.println("[EVENT DISPATCHER] Iniciando shutdown de todos los lanes...");

        // Validar que no hay eventos pendientes
        int pendingEvents = getTotalPendingEvents();
        if (pendingEvents > 0) {
            System.err.printf("[EVENT DISPATCHER] WARNING: %d eventos pendientes al cerrar%n", pendingEvents);
        }

        // Cerrar todos los lanes en orden inverso de prioridad
        String[] reverseOrder = { "Render", "Audio", "Physics", "Input", "Network", "System" };

        for (String laneName : reverseOrder) {
            VolcanEventLane lane = lanes.get(laneName);
            if (lane != null) {
                System.out.printf("[EVENT DISPATCHER] Cerrando lane: %s%n", laneName);

                // Cerrar el bus subyacente si tiene gracefulShutdown()
                IEventBus bus = lane.getBus();
                if (bus instanceof VolcanAtomicBus) {
                    ((VolcanAtomicBus) bus).gracefulShutdown();
                } else if (bus instanceof VolcanRingBus) {
                    ((VolcanRingBus) bus).gracefulShutdown();
                } else {
                    // Para otros buses, solo limpiar
                    bus.clear();
                }
            }
        }

        // Limpiar referencias
        lanes.clear();

        System.out.println("[EVENT DISPATCHER] Shutdown completado");
    }
}
