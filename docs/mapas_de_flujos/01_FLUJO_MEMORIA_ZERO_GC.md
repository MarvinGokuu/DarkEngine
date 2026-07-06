# 🗺️ Mapa del Flujo de Memoria Zero-GC (Capa 1: Cimientos)

Este mapa arquitectónico documenta la filosofía de memoria estricta del DarkEngine. Para evitar micro-pausas (stutters) producidas por el Recolector de Basura (Garbage Collector) de la JVM, el motor secuestra bloques directos de RAM (Off-Heap) utilizando el Project Panama (`MemorySegment`, `Arena`).

<div align="center">

```mermaid
graph TD
    subgraph jvm [Mundo Lento: Java Heap GC]
        A(Variables Temporales Locales)
        B(Strings Cortos y Logs)
        C(DarkEngineMaster Boot)
        Z["DarkScene / DarkEntity Pool<br/>(Pre-Alocados - Zero GC)"]
    end

    subgraph offheap [Mundo Rápido: Memoria Off-Heap Panama]
        D[("SectorMemoryVault<br/>Memoria Compartida Global")]
        E[("DarkTransformSoA<br/>Arreglos Primitivos Alineados")]
        F[("DarkAssetStreamer<br/>Buffers Mapeados a Disco")]
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
