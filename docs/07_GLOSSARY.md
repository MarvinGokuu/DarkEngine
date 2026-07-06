# 📖 07. GLOSSARY

Este glosario consolida la terminología técnica exclusiva de DarkEngine. Para programar o mantener este motor, es obligatorio hablar el mismo lenguaje arquitectónico.

## A
**AAA+ Certification**: Certificación interna del motor que garantiza cero recolección de basura en la vía crítica, inicialización en < 1 milisegundo y latencias atómicas inferiores a 150ns.

**AtomicBus (DarkAtomicBus)**: Bus asíncrono y atómico utilizado para transferir datos de Múltiples Productores a Un Consumidor (MPSC) sin utilizar Mutex o bloqueos del Sistema Operativo.

## C
**Cache Line**: La unidad mínima de transferencia de datos entre la CPU y la RAM (típicamente 64 bytes). Compartir una línea de caché entre hilos concurrentes destruye el rendimiento (ver *False Sharing*).

**Compute Shader**: Un programa (GLSL/HLSL) que se ejecuta enteramente en la GPU fuera del proceso de renderizado clásico, permitiendo utilizar miles de núcleos de video para matemáticas masivas paralelas (como el descarte de geometría/Culling).

## F
**False Sharing**: Fenómeno donde dos núcleos de CPU modifican variables independientes que residen en la misma Línea de Caché, forzando invalidaciones mutuas. Se soluciona mediante variables de *Padding*.

**Foreign Function Interface (FFI)**: Capacidad de invocar código de C/C++ directamente desde Java. Panama API permite realizar esto en DarkEngine a velocidad nativa.

## G
**Graceful Shutdown**: La secuencia de apagado elegante del motor. Libera la memoria física (Off-Heap) atada a los Sectores, y finaliza limpiamente los hilos de Kernel y Logging sin exceder 1 segundo de timeout.

## H
**Hot-Path**: El bucle principal de ejecución de 60 FPS (o más) del motor. El código aquí se ejecuta millones de veces por segundo. Está estrictamente prohibido instanciar objetos (`new`) o usar I/O bloqueante (como `System.out.println`) en este camino.

## M
**Mechanical Sympathy**: La filosofía base del motor. Escribir software entendiendo profundamente cómo funciona el hardware subyacente (Cachés L1/L2, predicción de saltos, NUMA, etc.).

**MemorySegment**: Abstracción de bajo nivel provista por Panama API que representa un bloque de memoria física no manejada por la JVM. Equivalente a un puntero en C (`void*`).

## P
**Padding**: Insertar variables fantasma (`long pad1, pad2...`) alrededor de una variable atómica caliente para aislarla físicamente en una Línea de Caché, evitando el *False Sharing*.

## S
**SectorMemoryVault**: La clase que rige todas las *arenas* de memoria fuera del Heap. El motor no tiene objetos, tiene sectores contiguos mapeados matemáticamente.

**SIMD (Single Instruction, Multiple Data)**: Arquitectura de hardware donde una sola instrucción de CPU procesa múltiples datos simultáneamente (Ej. sumar 8 posiciones X a la vez).

**SoA (Structure of Arrays)**: Patrón de diseño Data-Oriented opuesto a OOP (Array of Objects). En lugar de tener una clase `Entity` con `x` e `y`, se tienen arreglos gigantes de datos (`float[] X` y `float[] Y`), garantizando aciertos continuos en la Caché L1 al iterar.

**SSBO (Shader Storage Buffer Object)**: Una estructura de memoria masiva y desestructurada en OpenGL. DarkEngine utiliza SSBOs para mapear directamente sus `MemorySegments` nativos hacia la memoria VRAM sin serialización.

## T
**Thread Pinning**: Práctica de forzar a un Hilo (Thread) del Sistema Operativo a ejecutarse exclusivamente en un Núcleo Físico (Core) específico del procesador, maximizando el aprovechamiento de la Caché L1/L2 local.

**TimeKeeper (Gobernador)**: El componente del Kernel encargado de contar los nanosegundos transcurridos en un cuadro (Frame), y decidir si realizar *Spin-Wait* o `LockSupport.parkNanos` para mantener los 60 FPS estables sin entregar el CPU al Sistema Operativo.

## V
**VarHandle**: Primitiva de la JVM que provee acceso atómico relajado o estricto (`Compare-And-Swap`, `getOpaque`, `setVolatile`) a posiciones de memoria o campos de clase de forma mucho más barata que un bloque `synchronized`.

**VRAM (Video RAM)**: Memoria interna de la tarjeta gráfica, que cuenta con anchos de banda masivos (>500 GB/s) comparado con la RAM del sistema. El motor transfiere carga matemática pesada aquí para desatascar la CPU.

## Z
**Zero-Copy I/O**: Lectura de disco masiva (DirectStorage / `io_uring`) que mapea el disco NVMe directamente a la VRAM saltando el Kernel y el Heap.

---
*(Términos Futuros del Master Roadmap: **LWC** (Large World Coordinates, precisión en double 64-bit), **DAG** (Directed Acyclic Graph para manejo de hilos elásticos), **TDR Recovery** (Recuperación en caliente ante crasheos del driver de GPU sin cerrar el motor)).*
