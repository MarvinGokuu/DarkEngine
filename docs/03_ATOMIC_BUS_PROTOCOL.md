# ⚡ 03. ATOMIC BUS PROTOCOL

La comunicación entre el Kernel, los Sistemas y los Módulos de Hardware jamás se hace mediante llamadas directas a funciones o métodos, lo cual crearía un acoplamiento rígido de dependencias. Todo pasa por el **Sistema de Buses**, una red de comunicación de Ultra-Baja Latencia.

## Tipos de Buses en DarkEngine

### 1. DarkAtomicBus (MPSC - Multi-Producer, Single-Consumer)
Es el canal principal de entrada al Kernel.
Múltiples hilos (como sensores de red, entradas de teclado o timers) pueden escribir datos en este bus simultáneamente sin que existan bloqueos de software (`locks`).
- **Arquitectura Interna**: Usa `VarHandles` para operaciones atómicas (CAS - Compare And Swap) garantizando la linealidad de la escritura.
- **Formato**: Transmite mensajes compactados en un `long` primitivo (64 bits). 32 bits de payload, 16 bits de comando, etc.

### 2. DarkRingBus (SPSC - Single-Producer, Single-Consumer)
Diseñado para la comunicación más rápida posible entre dos puntos fijos (ej. del Kernel a Renderizado).
Al haber solo un lector y un escritor, las comprobaciones atómicas de contención son mucho menores. Es un buffer circular puro donde las cabezas de escritura y lectura corren en círculos infinitos sobrescribiendo memoria antigua de forma segura.
**Gestión de Ciclo de Vida**: No utiliza variables `volatile boolean closed` para apagar el bus, ya que esto agregaría una condicional costosa por cada evento. En su lugar, el cierre se indica inyectando un `TOMBSTONE_EVENT` (`0xFFFFFFFFFFFFFFFFL`) y limpiando la memoria residual instantáneamente sin bloqueos (*Spin-Waits*), previniendo los infames *Deadlocks* durante cierres forzados.

### 3. DarkEventDispatcher
Es el centro logístico que agrupa múltiples carriles (`DarkEventLane`) y enruta los eventos. Permite aplicar "máscaras" y filtros a las señales (SIMD data filtering) para procesarlas masivamente antes de entregárselas a los Sistemas de Juego.

## Latencias de Contención
La clave del rendimiento AAA+ de DarkEngine se encuentra aquí. Al no usar objetos mutables, los eventos transitan a latencias inferiores a los 50 nanosegundos (aprox. 150 ciclos de reloj de CPU).

Si el sistema sobrepasa esta latencia, el `BusSymmetryValidator` hará fallar la inicialización del motor, impidiendo que entre a Producción.
