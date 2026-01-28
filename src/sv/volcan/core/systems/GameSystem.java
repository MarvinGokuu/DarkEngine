package sv.volcan.core.systems;

import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Contrato Fundamental de Estrategia de Sistema.
 * DEPENDENCIAS: WorldStateFrame
 * MÉTRICAS: Zero-Allocation Interface
 * 
 * Interfaz base para todos los sistemas lógicos del motor (Física, IA, Reglas).
 * Impone rigurosamente el patrón Strategy y el principio de Inmutabilidad
 * durante la ejecución.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public interface GameSystem {

    /**
     * Ejecuta la lógica del sistema para un tick del motor.
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * CONTRATO DE EJECUCIÓN
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * PRECONDICIONES (lo que el Kernel garantiza):
     * 1. state != null
     * 2. state es inmutable durante esta llamada
     * 3. deltaTime > 0 y deltaTime < TICK_BUDGET
     * 4. No hay otras llamadas concurrentes a este sistema
     * 
     * POSTCONDICIONES (lo que el sistema debe garantizar):
     * 1. Solo modifica datos vía state.writeXXX()
     * 2. No crea objetos en el Heap
     * 3. Retorna en tiempo determinista (< 1ms típicamente)
     * 4. Mismo input produce mismo output (bit-perfect)
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * CONCURRENCIA
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * MODELO: Single-threaded per system
     * - El Kernel puede ejecutar DIFERENTES sistemas en paralelo
     * - El Kernel NUNCA ejecuta el MISMO sistema concurrentemente
     * - No se requieren locks dentro del sistema
     * 
     * EJEMPLO DE EJECUCIÓN PARALELA:
     * Thread 1: MovementSystem.update(frame, dt)
     * Thread 2: SpriteSystem.update(frame, dt) // OK: diferentes sistemas
     * 
     * EJEMPLO PROHIBIDO:
     * Thread 1: MovementSystem.update(frame1, dt)
     * Thread 2: MovementSystem.update(frame2, dt) // NUNCA: mismo sistema
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * DETERMINISMO
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * DEFINICIÓN: Para el mismo estado de entrada y deltaTime, el sistema
     * debe producir exactamente el mismo estado de salida.
     * 
     * MATEMÁTICAMENTE:
     * update(state₁, dt₁) = result₁
     * update(state₁, dt₁) = result₁ // Siempre el mismo resultado
     * 
     * APLICACIONES:
     * - REPLAY: Grabar inputs y reproducir partida exactamente
     * - NETCODE: Sincronizar clientes con solo inputs (no estado completo)
     * - DEBUGGING: Reproducir bugs de forma consistente
     * - TESTING: Tests deterministas y reproducibles
     * 
     * CÓMO GARANTIZARLO:
     * ✅ Usar solo datos de WorldStateFrame
     * ✅ Usar deltaTime proporcionado (no System.currentTimeMillis())
     * ✅ Usar seeds del frame para random (no Math.random())
     * ✅ Evitar floating-point no-determinista (usar double, no float)
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * PERFORMANCE
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * OBJETIVO: < 1ms por sistema para 60 FPS
     * 
     * PRESUPUESTO DE TIEMPO (60 FPS = 16.6ms por frame):
     * - Input Processing: 1ms
     * - Bus Processing: 0.5ms
     * - Systems Execution: 10ms (distribuido entre N sistemas)
     * - State Hashing: 0.5ms
     * - Rendering: 4ms
     * - Buffer: 0.6ms
     * 
     * TÉCNICAS DE OPTIMIZACIÓN:
     * 1. Cache Locality: Acceso secuencial a memoria
     * 2. SIMD: Operaciones vectorizadas cuando sea posible
     * 3. Branch Prediction: Minimizar if/else en loops
     * 4. Prefetching: CPU puede predecir accesos
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * @param state     Snapshot inmutable del estado del mundo (Single Source of
     *                  Truth).
     *                  Este frame contiene TODOS los datos del juego en memoria
     *                  Off-Heap.
     * 
     *                  ACCESO:
     *                  - Lectura: state.readDouble(offset), state.readInt(offset)
     *                  - Escritura: state.writeDouble(offset, value)
     * 
     *                  INMUTABILIDAD:
     *                  - El frame NO cambia durante update()
     *                  - Las escrituras van a un buffer que se commitea después
     *                  - Esto garantiza que todos los sistemas ven el mismo estado
     * 
     * @param deltaTime Tiempo transcurrido desde el último tick en segundos.
     * 
     *                  RANGO TÍPICO: 0.016 segundos (60 FPS) a 0.033 segundos (30
     *                  FPS)
     * 
     *                  USO:
     *                  - Física: position += velocity * deltaTime
     *                  - Animación: frame += frameRate * deltaTime
     *                  - Timers: timer -= deltaTime
     * 
     *                  DETERMINISMO:
     *                  - El Kernel garantiza que deltaTime es consistente
     *                  - Para el mismo tick, todos los sistemas ven el mismo
     *                  deltaTime
     *                  - No usar System.nanoTime() o similar
     * 
     * @throws RuntimeException Solo en casos de errores irrecuperables
     *                          (corrupción de memoria, violación de contrato)
     * 
     * @see WorldStateFrame Para detalles de acceso a memoria
     * @see EntityLayout Para offsets de entidades
     * @see VolcanStateLayout Para offsets de estado del kernel
     */
    void update(WorldStateFrame state, double deltaTime);

    /**
     * Retorna el nombre del sistema para debugging y telemetría.
     * 
     * IMPLEMENTACIÓN POR DEFECTO: Usa el nombre de la clase.
     * 
     * OVERRIDE: Solo si necesitas un nombre personalizado.
     * 
     * EJEMPLO:
     * 
     * @Override
     *           public String getName() {
     *           return "PhysicsSystem-v2";
     *           }
     * 
     * @return Nombre del sistema (no null, no vacío)
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Retorna los nombres de sistemas de los que este sistema depende.
     * 
     * PROPÓSITO: Construir el grafo de dependencias para ejecución paralela.
     * 
     * SEMÁNTICA:
     * - Si este sistema depende de "PhysicsSystem", debe retornar {"PhysicsSystem"}
     * - El Kernel garantiza que las dependencias se ejecutan ANTES
     * - Sistemas sin dependencias pueden ejecutarse en paralelo
     * 
     * IMPLEMENTACIÓN POR DEFECTO: Sin dependencias (puede ejecutarse primero)
     * 
     * OVERRIDE: Solo si el sistema necesita que otros se ejecuten antes
     * 
     * EJEMPLO:
     * 
     * @Override
     *           public String[] getDependencies() {
     *           return new String[]{"PhysicsSystem", "InputSystem"};
     *           }
     * 
     *           IMPORTANTE:
     *           - No crear dependencias circulares (A→B→A)
     *           - El Kernel detecta ciclos y falla rápido
     *           - Minimizar dependencias para maximizar paralelismo
     * 
     * @return Array de nombres de sistemas (puede ser vacío, no null)
     */
    default String[] getDependencies() {
        return new String[0]; // Sin dependencias por defecto
    }
}
// Creado: 03/01/2026 23:35
// Rol: Software Architect aplicando Strategy Pattern + SOLID
// Principios: ISP, OCP, SSOT, Determinism, Immutability
