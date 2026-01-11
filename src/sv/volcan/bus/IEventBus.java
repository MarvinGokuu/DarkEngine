// Reading Order: 00000011
package sv.volcan.bus;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Contrato de comunicación para buses de eventos.
 * GARANTÍAS: Abstracción pura, permite múltiples implementaciones (Ring, LMAX,
 * JCTools).
 * PROHIBICIONES: Prohibido crear objetos en hot-path; solo primitivos.
 * DOMINIO CRÍTICO: Concurrencia / Abstracción
 * 
 * PATRÓN: Strategy Pattern + Interface Segregation
 * CONCEPTO: Dependency Inversion Principle
 * ROL: Contract Definition
 * 
 * @author MarvinDev
 * @version 2.0
 * @since 2026-01-04
 */
public interface IEventBus {

    /**
     * Intenta insertar un evento en el bus.
     * Operación no bloqueante.
     * 
     * @param event Evento codificado como long (64 o 128 bits)
     * @return true si el evento fue aceptado, false si el buffer está saturado
     */
    boolean offer(long event);

    /**
     * Consume el siguiente evento del bus.
     * Operación destructiva: el evento se elimina del bus.
     * 
     * @return El evento (long) o -1 si el bus está vacío
     */
    long poll();

    /**
     * Lee el siguiente evento sin consumirlo.
     * Operación no destructiva: el evento permanece en el bus.
     * 
     * @return El evento (long) o -1 si el bus está vacío
     */
    long peek();

    /**
     * Retorna el número de eventos pendientes en el bus.
     * 
     * @return Cantidad de eventos disponibles para consumir
     */
    int size();

    /**
     * Retorna la capacidad total del bus.
     * 
     * @return Número máximo de eventos que puede almacenar
     */
    int capacity();

    /**
     * Retorna la capacidad restante del bus.
     * 
     * @return Número de eventos que aún pueden ser insertados
     */
    default int remainingCapacity() {
        return capacity() - size();
    }

    /**
     * Limpia todos los eventos del bus.
     * Operación destructiva: todos los eventos se descartan.
     */
    void clear();

    /**
     * Verifica si el bus está vacío.
     * 
     * @return true si no hay eventos pendientes
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Verifica si el bus está lleno.
     * 
     * @return true si no se pueden insertar más eventos
     */
    default boolean isFull() {
        return remainingCapacity() == 0;
    }
}
