# 💾 04. MEMORY AND STATE

La memoria convencional en Java se encuentra dentro del **Heap**, una zona controlada por la Máquina Virtual de Java (JVM) y vigilada por el Recolector de Basura (Garbage Collector).

En **DarkEngine**, evitamos el Heap en toda nuestra vía crítica. Usar el Heap implica riesgos de pausas de GC, lo cual es inaceptable para un motor que opera en el orden de nanosegundos.

## SectorMemoryVault (Memoria Off-Heap)

`SectorMemoryVault` es el gestor absoluto de la memoria física de DarkEngine. Su propósito es interactuar directamente con la memoria RAM cruda, puenteando completamente a la JVM.

### ¿Cómo Funciona?
1. **Arena Allocation**: Utiliza la Foreign Function & Memory API (`Arena.ofShared()`) para solicitar un bloque contiguo y gigantesco de memoria al sistema operativo.
2. **MemorySegments**: Trata este bloque como un `MemorySegment`, lo cual es el equivalente en C a un bloque de memoria obtenido por `malloc()`.
3. **Mapeo Dimensional**: En lugar de crear un objeto por cada entidad del juego (Ej. 100,000 Objetos de Partícula), almacena 100,000 valores `X`, 100,000 valores `Y`, y 100,000 banderas de estado de forma contigua. Esto permite un recorrido secuencial que la Caché L1 del procesador prefiere (Data Locality).

## EngineStateChannel (Canalización de Estado)

DarkEngine nunca verifica el estado general con condicionales pesados dentro de su Loop (ej. `if (engine == RUNNING)`). Utiliza el `EngineStateChannel`, un canal hiper-optimizado.

1. Se trata de un entero atómico alojado de manera segura en memoria, accesible atómicamente mediante `VarHandles`.
2. Contiene transiciones de estado strictas (`BOOTING` -> `RUNNING` -> `SHUTDOWN_REQUESTED` -> `TERMINATED`).
3. La lectura del estado no requiere sincronización, el Kernel puede validarlo en menos de 10 ciclos de reloj en su *Hot-Path*.
