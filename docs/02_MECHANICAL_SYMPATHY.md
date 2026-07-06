# ⚙️ 02. MECHANICAL SYMPATHY

La "Simpatía Mecánica" es el entendimiento profundo de cómo funciona el hardware subyacente para poder programar en sintonía con él. DarkEngine descarta las convenciones tradicionales de Java ("escribe una vez, corre donde sea") a favor del rendimiento absoluto ("escribe una vez, compila a la capa nativa").

## El Precio del Objeto

En Java tradicional, crear objetos dinámicamente (`new Object()`) en un ciclo de juego obliga al Garabage Collector (GC) a pausar los hilos ("Stop-The-World") para limpiar la memoria. Esto causa micro-tirones y latencia impredecible.

En **DarkEngine**, evitamos las "Object Allocations" en el Hot-Path. Utilizamos primitivos de 64 bits (`long`) compactados para transmitir instrucciones, offsets y banderas.

Además, erradicamos el paradigma Orientado a Objetos (OOP) para la lógica de alto rendimiento (ECS Clásico) e implementamos **Structure of Arrays (SoA)**. Las posiciones espaciales y físicas de millones de entidades se almacenan en arreglos contiguos gigantes.

## Procesamiento SIMD (Vector API)

Para procesar esta memoria plana, el motor invoca instrucciones SIMD (Single Instruction, Multiple Data) a través de *Project Panama Vector API*. En lugar de actualizar entidades una por una, los procesadores matemáticos (Ej: `DarkKinematicsSystem`) cargan vectores nativos y calculan 8 a 16 entidades en un solo ciclo de reloj de la CPU utilizando registros AVX-512.

## VRAM Offloading (Compute Shaders)
Cuando ni siquiera SIMD AVX-512 es suficiente para procesar 1 Millón de colisiones espaciales en pantalla, la Simpatía Mecánica dicta que se debe cambiar de chip. DarkEngine transfiere la carga masiva paralelizable a la GPU. Al enviar los arreglos SoA vía PCI-Express hacia la VRAM, un *Compute Shader* en GLSL puede descartar geometría invisible usando miles de núcleos gráficos dedicados, esquivando totalmente a la CPU.

## VarHandles y FFI (Panama API)

Para interactuar con la memoria y la CPU sin intermediarios:

1. **VarHandles**: Proporcionan acceso volátil y atómico (`getOpaque`, `compareAndSet`) directo a variables, sin la penalización de bloqueos del sistema operativo (`Mutex`). Es la base del `DarkAtomicBus`.
2. **Foreign Function & Memory API (Panama)**: Permite invocar funciones nativas de C/C++ directamente desde Java (`Downcalls`). El motor utiliza esto en `ThreadPinning` y `SystemStateManager` para saltarse la Máquina Virtual.
3. **Zero-Contention Latch (Shadow Buffer)**: Para evitar colisiones (`Locks`) entre los callbacks asíncronos de Interfaz (GLFW/ImGui) y el hilo principal del Juego, el estado del teclado/ratón se captura en un "Shadow Buffer" nativo paralelo. Terminando el barrido, se hace una transferencia SIMD vectorial (`MemorySegment.copy()`) masiva e instantánea al Vault de memoria principal. Input Lag = 0ms.

## Thread Pinning y Afinidad Física (Hacia el Grafo DAG)

Si un hilo es movido de un núcleo de CPU a otro por el Sistema Operativo, todas las memorias Caché (L1/L2) locales a ese núcleo quedan inválidas (Cache Miss), resultando en accesos lentos a la RAM principal.

El motor soluciona esto fijando (Pinning) hilos específicos a núcleos físicos de la CPU mediante:
- **Windows**: Llamada a `SetThreadAffinityMask` de `kernel32.dll`.
- **Linux/POSIX**: Llamada a `pthread_setaffinity_np` de `libpthread.so` / `libc.so`.

**Evolución Arquitectónica (Master Roadmap):** Para maximizar la simpatía mecánica de estos hilos fijos, el motor migrará de barreras estáticas globales a un **Directed Acyclic Task Graph (DAG)**. Los hilos nunca esperarán ociosos; al terminar una micro-tarea, desbloquearán atómicamente a las dependientes, saturando los núcleos al 100% sin pausas (Zero-Stall).

## Prevención de False Sharing

Cuando dos hilos modifican variables distintas que residen en la misma **Línea de Caché** (Cache Line, típicamente 64 o 128 bytes), los procesadores invalidan mutuamente la línea entera, destrozando el rendimiento en paralelo.

**DarkEngine** implementa variables de *Padding* (relleno de ceros) entre contadores concurrentes. Esto separa artificialmente las variables en memoria para asegurar que cada núcleo tenga su propia Línea de Caché exclusiva.
