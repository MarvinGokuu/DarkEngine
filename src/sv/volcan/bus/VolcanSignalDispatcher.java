package sv.volcan.bus;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Orquestación y despacho de señales sin boxing.
 * DEPENDENCIAS: VolcanAtomicBus, VolcanSignalPacker
 * MÉTRICAS: Zero-GC, Latencia <150ns
 * 
 * Fachada de alto rendimiento para el acceso al bus atómico.
 * Garantiza integridad en el enrutamiento y cero asignaciones en el hot-path.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanSignalDispatcher {

    // Fachada exclusiva para el Bus Atómico de alto rendimiento
    private final VolcanAtomicBus bus;
    private static final int BUS_SIZE_POWER = 16; // 65536 Slots

    public VolcanSignalDispatcher() {
        this.bus = new VolcanAtomicBus(BUS_SIZE_POWER);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // OPERACIONES BÁSICAS DE DISPATCH
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Propaga un evento al bus principal.
     * 
     * CORRECCIÓN AAA+: Cambiado de push() a offer() para compatibilidad con
     * la interfaz IEventBus implementada en VolcanAtomicBus.
     * 
     * LATENCIA ESPERADA: <150ns
     * 
     * @param event Señal empaquetada (64 bits)
     * @return true si el evento fue encolado exitosamente
     */
    public boolean dispatch(long event) {
        return bus.offer(event);
    }

    /**
     * Consume el siguiente evento del bus.
     * 
     * LATENCIA ESPERADA: <150ns
     * 
     * @return El evento (long) o -1 si el bus está vacío
     */
    public long pollEvent() {
        return bus.poll();
    }

    /**
     * Procesa todos los eventos disponibles en el bus.
     * 
     * OPTIMIZACIÓN AAA+: Usa SignalProcessor en lugar de LongConsumer para
     * evitar boxing y permitir optimizaciones del JIT.
     * 
     * MECÁNICA:
     * - Consume eventos hasta que el bus esté vacío
     * - Aplica el procesador a cada evento
     * - Sin allocations en hot-path
     * 
     * THROUGHPUT ESPERADO: >10M eventos/segundo
     * 
     * @param processor Procesador de señales (sin boxing)
     * @return Número de eventos procesados
     */
    public int processAllEvents(SignalProcessor processor) {
        int count = 0;
        long event;
        while ((event = bus.poll()) != -1L) {
            processor.process(event);
            count++;
        }
        return count;
    }

    /**
     * Verifica si hay eventos pendientes en el bus.
     * CORRECCIÓN: Ahora usa size() en lugar de poll() para evitar consumir eventos.
     * 
     * @return true si hay al menos un evento disponible
     */
    public boolean hasEvents() {
        return bus.size() > 0;
    }

    /**
     * Limpia todos los eventos del bus.
     * Útil para reiniciar el estado entre tests o al reiniciar el kernel.
     */
    public void clear() {
        bus.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // OPERACIONES BATCH (Procesamiento Masivo)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Despacha múltiples eventos en una sola operación.
     * 
     * OPTIMIZACIÓN AAA+:
     * - Reduce operaciones volatile (1 setRelease vs N)
     * - Optimiza bus de direcciones del CPU
     * - Permite prefetching secuencial
     * 
     * THROUGHPUT ESPERADO: >10M eventos/segundo
     * 
     * @param events Array de eventos a despachar
     * @param offset Índice inicial en el array
     * @param length Número de eventos a despachar
     * @return Número de eventos realmente despachados
     */
    public int dispatchBatch(long[] events, int offset, int length) {
        return bus.batchOffer(events, offset, length);
    }

    /**
     * Consume múltiples eventos en una sola operación.
     * 
     * OPTIMIZACIÓN AAA+:
     * - Reduce operaciones Acquire
     * - Permite procesamiento vectorizado
     * - Ideal para pipelines masivos
     * 
     * @param outputBuffer Array donde se escribirán los eventos
     * @param maxEvents    Número máximo de eventos a consumir
     * @return Número de eventos realmente consumidos
     */
    public int pollBatch(long[] outputBuffer, int maxEvents) {
        return bus.batchPoll(outputBuffer, maxEvents);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // DISPATCH DE DATOS ESPECIALIZADOS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Despacha un GUID (identificador único de 64 bits).
     * 
     * PROPÓSITO:
     * - Identificadores de entidades espaciales
     * - Tracking de paquetes de red
     * - Referencias a objetos masivos
     * 
     * @param guid Identificador único (64 bits)
     * @return true si fue despachado exitosamente
     */
    public boolean dispatchGUID(long guid) {
        return bus.offer(VolcanSignalPacker.packGUID(guid));
    }

    /**
     * Despacha un vector 2D (2 floats empaquetados en 1 long).
     * 
     * PROPÓSITO:
     * - Coordenadas de posición
     * - Vectores de velocidad
     * - Datos de física en tiempo real
     * 
     * @param x Coordenada X (32-bit float)
     * @param y Coordenada Y (32-bit float)
     * @return true si fue despachado exitosamente
     */
    public boolean dispatchVector2D(float x, float y) {
        return bus.offer(VolcanSignalPacker.packFloats(x, y));
    }

    /**
     * Despacha datos de telemetría espacial.
     * 
     * PROPÓSITO:
     * - Datos orbitales
     * - Telemetría de satélites
     * - Comunicación de larga distancia
     * 
     * @param telemetryData Datos empaquetados (64 bits)
     * @return true si fue despachado exitosamente
     */
    public boolean dispatchSpatialData(long telemetryData) {
        return bus.offer(telemetryData);
    }

    /**
     * Despacha un puntero a memoria off-heap.
     * 
     * PROPÓSITO:
     * - Referencias a MemorySegment (Project Panama)
     * - Punteros a datos masivos (mapas estelares)
     * - Zero-copy desde fuentes externas
     * 
     * ADVERTENCIA: Solo válido en la misma sesión de JVM.
     * 
     * @param memoryAddress Dirección de memoria (64 bits)
     * @return true si fue despachado exitosamente
     */
    public boolean dispatchOffHeapPointer(long memoryAddress) {
        return bus.offer(VolcanSignalPacker.packOffHeapPointer(memoryAddress));
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // EDGE COMPUTING INTEGRATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Inyecta datos desde fuente externa con zero-copy.
     * 
     * MECÁNICA:
     * - Datos llegan por satélite/red
     * - Se escriben directamente en el buffer del bus
     * - Sin copias intermedias
     * - Preserva alineación de cache line
     * 
     * PROPÓSITO:
     * - Telemetría de larga distancia
     * - Edge computing
     * - Latencia mínima
     * 
     * @param externalBuffer Buffer externo (ya en formato long[])
     * @param count          Número de señales a inyectar
     * @return Número de señales inyectadas exitosamente
     */
    public int injectFromExternal(long[] externalBuffer, int count) {
        return bus.batchOffer(externalBuffer, 0, count);
    }

    /**
     * Retorna referencia directa al bus para operaciones avanzadas.
     * 
     * ADVERTENCIA: Uso avanzado. Romper encapsulación solo cuando sea
     * absolutamente necesario para optimizaciones de nivel kernel.
     * 
     * @return Referencia al bus atómico subyacente
     */
    public VolcanAtomicBus getUnderlyingBus() {
        return bus;
    }
}
