# ⚙️ 02. MECHANICAL SYMPATHY

La "Simpatía Mecánica" es el entendimiento profundo de cómo funciona el hardware subyacente para poder programar en sintonía con él. DarkEngine descarta las convenciones tradicionales de Java ("escribe una vez, corre donde sea") a favor del rendimiento absoluto ("escribe una vez, compila a la capa nativa").

## El Precio del Objeto

En Java tradicional, crear objetos dinámicamente (`new Object()`) en un ciclo de juego obliga al Garabage Collector (GC) a pausar los hilos ("Stop-The-World") para limpiar la memoria. Esto causa micro-tirones y latencia impredecible.

En **DarkEngine**, evitamos las "Object Allocations" en el Hot-Path. Utilizamos primitivos de 64 bits (`long`) compactados para transmitir instrucciones, offsets y banderas.

## VarHandles y FFI (Panama API)

Para interactuar con la memoria y la CPU sin intermediarios:

1. **VarHandles**: Proporcionan acceso volátil y atómico (`getOpaque`, `compareAndSet`) directo a variables, sin la penalización de bloqueos del sistema operativo (`Mutex`). Es la base del `DarkAtomicBus`.
2. **Foreign Function & Memory API (Panama)**: Permite invocar funciones nativas de C/C++ directamente desde Java (`Downcalls`). El motor utiliza esto en `ThreadPinning` y `SystemStateManager` para saltarse la Máquina Virtual.

## Thread Pinning y Afinidad Física

Si un hilo es movido de un núcleo de CPU a otro por el Sistema Operativo, todas las memorias Caché (L1/L2) locales a ese núcleo quedan inválidas (Cache Miss), resultando en accesos lentos a la RAM principal.

El motor soluciona esto fijando (Pinning) hilos específicos a núcleos físicos de la CPU mediante:
- **Windows**: Llamada a `SetThreadAffinityMask` de `kernel32.dll`.
- **Linux/POSIX**: Llamada a `pthread_setaffinity_np` de `libpthread.so` / `libc.so`.

## Prevención de False Sharing

Cuando dos hilos modifican variables distintas que residen en la misma **Línea de Caché** (Cache Line, típicamente 64 o 128 bytes), los procesadores invalidan mutuamente la línea entera, destrozando el rendimiento en paralelo.

**DarkEngine** implementa variables de *Padding* (relleno de ceros) entre contadores concurrentes. Esto separa artificialmente las variables en memoria para asegurar que cada núcleo tenga su propia Línea de Caché exclusiva.
