package sv.volcan.bus;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Procesamiento de señales sin boxing.
 * GARANTÍAS: Zero-allocation, hot-path optimizado.
 * 
 * DIFERENCIA CON LongConsumer:
 * - No hereda de java.util.function (evita boxing)
 * - Especializada para el dominio del motor
 * - Permite optimizaciones del JIT específicas
 * 
 * PROPÓSITO:
 * Interfaz funcional para procesamiento de señales de 64 bits sin crear
 * objetos en el hot-path. A diferencia de java.util.function.LongConsumer,
 * esta interfaz está diseñada específicamente para el motor Volcan y permite
 * al JIT aplicar optimizaciones más agresivas.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
@FunctionalInterface
public interface SignalProcessor {

    /**
     * Procesa una señal de 64 bits.
     * 
     * ADVERTENCIA: Este método se llama en hot-path.
     * - NO crear objetos
     * - NO hacer I/O
     * - NO bloquear threads
     * - NO lanzar excepciones (usar códigos de error)
     * 
     * LATENCIA ESPERADA: <50ns por señal
     * 
     * @param signal Señal empaquetada (64 bits)
     */
    void process(long signal);
}
