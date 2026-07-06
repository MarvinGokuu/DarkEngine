# 💾 04. MEMORY AND STATE

La memoria convencional en Java se encuentra dentro del **Heap**, una zona controlada por la Máquina Virtual de Java (JVM) y vigilada por el Recolector de Basura (Garbage Collector).

En **DarkEngine**, evitamos el Heap en toda nuestra vía crítica. Usar el Heap implica riesgos de pausas de GC, lo cual es inaceptable para un motor que opera en el orden de nanosegundos.

## SectorMemoryVault (Memoria Off-Heap)

`SectorMemoryVault` es el gestor absoluto de la memoria física de DarkEngine. Su propósito es interactuar directamente con la memoria RAM cruda, puenteando completamente a la JVM.

### ¿Cómo Funciona?
1. **Arena Allocation**: Utiliza la Foreign Function & Memory API (`Arena.ofShared()`) para solicitar un bloque contiguo y gigantesco de memoria al sistema operativo.
2. **MemorySegments**: Trata este bloque como un `MemorySegment`, lo cual es el equivalente en C a un bloque de memoria obtenido por `malloc()`.
3. **Mapeo Dimensional (SoA)**: En lugar de crear un objeto por cada entidad del juego (Ej. 100,000 Objetos de Partícula), almacena 100,000 valores `X`, 100,000 valores `Y`, y 100,000 banderas de estado de forma contigua. Esto permite un recorrido secuencial que la Caché L1 del procesador prefiere (Data Locality).
4. **Topological Sorting (Grafo de Escena)**: Para el Grafo de Escena, DarkEngine implementa una **jerarquía lógica desacoplada** (`parent`, `first_child`, `next_sibling` arrays) dentro de `DarkScene`. Esto permite un ordenamiento topológico determinista en `O(N)` para las dependencias Padre-Hijo sin tocar o reordenar físicamente el bloque `DarkTransformSoA`, preservando íntegra la coalescencia de caché durante las multiplicaciones de matrices locales a globales.
5. **Large World Coordinates (LWC)**: Según el Roadmap Maestro, las coordenadas `X/Y/Z` migrarán de `float` (32-bit) a `double` (64-bit) en estos segmentos de memoria cruda, garantizando un mundo abierto sin "Vertex Jitter", para luego restarse asíncronamente frente a la cámara enviando floats a la GPU.
6. **Shader Storage Buffer Objects (SSBO)**: Dado que nuestros MemorySegments son contiguos, pueden enviarse íntegros y en crudo a la VRAM mediante enlaces de OpenGL FFI. Al llegar a la GPU, se convierten en SSBOs y alimentan instantáneamente los Compute Shaders sin necesidad de serialización.

## La Fachada Orientada a Objetos (Zero-GC Pool)

Aunque el núcleo es puramente Data-Oriented (SoA), exponer esta complejidad al desarrollador final arruinaría la experiencia de usuario (DX). Para solucionar esto, DarkEngine implementa un patrón **Facade** estricto:

1. **`DarkEntity` (El Wrapper):** Es una clase tradicional de Java que el desarrollador utiliza para llamar a métodos familiares como `entity.setPosition(x, y, z)`. Sin embargo, esta clase **no guarda variables**. Es un cascarón vacío que traduce las llamadas inmediatamente a operaciones de escritura/lectura mediante `VarHandles` en el arreglo nativo `DarkTransformSoA`.
2. **Pre-Asignación (Object Pooling):** Para evitar la penalización del Garbage Collector, el motor **jamás** usa la palabra clave `new` durante el gameplay. El orquestador `DarkScene` pre-asigna un arreglo masivo de wrappers `DarkEntity[]` durante el arranque (Boot).
3. **Cero Penalizaciones:** Cuando el desarrollador invoca `spawnEntity()`, el motor simplemente le devuelve el puntero al wrapper correspondiente de la lista pre-asignada. El recolector de basura nunca se entera, logrando un diseño 100% Zero-GC.
4. **Preparación para Valhalla:** Esta fachada cimenta el camino para la llegada de *Value Classes* (Project Valhalla en Java 26+), donde estos wrappers podrán vivir directamente en los registros del CPU sin existir siquiera en el Heap.

## EngineStateChannel (Canalización de Estado)

DarkEngine nunca verifica el estado general con condicionales pesados dentro de su Loop (ej. `if (engine == RUNNING)`). Utiliza el `EngineStateChannel`, un canal hiper-optimizado.

1. Se trata de un entero atómico alojado de manera segura en memoria, accesible atómicamente mediante `VarHandles`.
2. Contiene transiciones de estado strictas (`BOOTING` -> `RUNNING` -> `SHUTDOWN_REQUESTED` -> `TERMINATED`).
3. La lectura del estado no requiere sincronización, el Kernel puede validarlo en menos de 10 ciclos de reloj en su *Hot-Path*.
# 🗺️ Mapa del Flujo de Memoria Zero-GC (Capa 1: Cimientos)

Este mapa arquitectónico documenta la filosofía de memoria estricta del DarkEngine. Para evitar micro-pausas (stutters) producidas por el Recolector de Basura (Garbage Collector) de la JVM, el motor secuestra bloques directos de RAM (Off-Heap) utilizando el Project Panama (`MemorySegment`, `Arena`).

<div align="center">

```mermaid
graph TD
    subgraph jvm [Mundo Lento: Java Heap GC]
        A(Variables Temporales Locales)
        B(Strings Cortos y Logs)
        C(DarkEngineMaster Boot)
        Z[DarkScene / DarkEntity Pool<br/>(Pre-Alocados - Zero GC)]
    end

    subgraph offheap [Mundo Rápido: Memoria Off-Heap Panama]
        D[(SectorMemoryVault<br/>Memoria Compartida Global)]
        E[(DarkTransformSoA<br/>Arreglos Primitivos Alineados)]
        F[(DarkAssetStreamer<br/>Buffers Mapeados a Disco)]
    end

    subgraph hardware [Capa de Silicio]
        G(CPU L1/L2 Cache)
        H(PCIe Bus)
        I[(VRAM Tarjeta Gráfica)]
    end

    %% Flujos de Asignación y Copia
    C -->|Asigna| D
    C -->|Asigna| E
    C -->|Asigna| F
    C -->|Pre-Aloca al inicio| Z

    %% Flujo OOP Facade
    Z == Punteros VarHandles ==> E

    %% Flujo Cero Copias (Zero-Copy AZDO)
    F == Zero-Copy ==> H
    H == DMA Transfer ==> I
    
    %% ECS SoA a Caché
    E -->|Lectura Secuencial Perfecta| G

    %% Estilos AAA
    classDef slow fill:#e74c3c,stroke:#c0392b,stroke-width:2px,color:#fff;
    classDef fast fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef hardware fill:#f39c12,stroke:#d35400,stroke-width:3px,color:#fff;
    classDef prealloc fill:#27ae60,stroke:#2ecc71,stroke-width:2px,color:#fff;

    class A,B,C slow;
    class Z prealloc;
    class D,E,F fast;
    class G,H,I hardware;
```

</div>

## Leyenda Técnica:
*   **SectorMemoryVault:** El cofre central del estado del juego. No guarda objetos Java (`new Object()`), guarda variables atómicas primitivas en memoria nativa C-like.
*   **DarkAssetStreamer:** Lee archivos del disco duro (MMAP) directamente a memoria Off-Heap y los dispara por el PCIe Bus a la VRAM sin jamás tocar el Heap de Java (Zero-Copy).
*   **DarkScene / DarkEntity Pool:** El orquestador y los envoltorios (Wrappers) Orientados a Objetos. Nacen en el Java Heap, pero se *pre-alocan* 100% en el Boot, burlando al GC. Leen/Escriben al SoA crudo usando punteros hiper-rápidos (`VarHandles`).
*   **Zero-GC:** Durante el gameplay (60 FPS), la cantidad de memoria asignada en el Heap de Java debe ser exactamente cero (`0 bytes/frame`). Todo ocurre en el bloque `Off-Heap` o reciclando la *Pool* pre-asignada.

