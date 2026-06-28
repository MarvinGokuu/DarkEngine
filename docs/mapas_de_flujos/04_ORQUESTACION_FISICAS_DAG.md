# 🗺️ Mapa del Flujo de Físicas y Orquestación DAG (Fase 4)

Este diagrama documenta la arquitectura de ejecución asíncrona del DarkEngine. El motor no usa barreras globales ni pausas. Utiliza un Grafo Acíclico Dirigido (DAG) para disparar hilos trabajadores (Worker Threads) elásticamente en cuanto sus dependencias de datos (Data-Oriented) son resueltas.

<div align="center">

```mermaid
graph TD
    %% Inicialización
    subgraph init [1. Arranque de Ciclo]
        A((TimeKeeper<br/>Tick Delta)) --> B{DarkTaskDispatcher}
    end

    %% Tareas Paralelas sin Dependencias (Se ejecutan al mismo tiempo en N Cores)
    subgraph parallel [2. Tareas Libres]
        B --> C[DarkInputSystem]
        B --> D[DarkCameraState]
        B --> E[SpatialHashGrid]
    end

    %% Tareas Secuenciales Dependientes (Físicas)
    subgraph physics [3. Pipeline de Físicas SIMD]
        E --> F[BroadphaseSystem]
        F -->|Identifica pares cercanos| G[NarrowphaseSystem]
        G -->|Calcula fuerzas de impacto| H[SceneKinematicsSystem]
        H -->|Integra Posición| I[(DarkTransformSoA)]
    end

    %% Sincronización Final
    subgraph sync [4. Cierre de Estado]
        I --> J{Sincronización Lock-Free}
        C --> J
        D --> J
        J --> K((Fin del Frame Físico))
    end

    %% Estilos AAA
    classDef dispatcher fill:#c0392b,stroke:#e74c3c,stroke-width:3px,color:#fff;
    classDef worker fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef physics fill:#27ae60,stroke:#2ecc71,stroke-width:2px,color:#fff;
    classDef memory fill:#8e44ad,stroke:#9b59b6,stroke-width:4px,color:#fff;

    class B,J dispatcher;
    class C,D,E worker;
    class F,G,H physics;
    class I memory;
```

</div>

## Leyenda Técnica:
*   **DarkTaskDispatcher:** El "cerebro" multicore. No espera a que terminen todas las tareas para lanzar la siguiente. Si una tarea no depende de otra, la inyecta inmediatamente al Core más libre.
*   **Broadphase (GPU Radix Sort):** Filtra descartes masivos (quién está cerca de quién) procesando miles de colisionadores en paralelo en la VRAM.
*   **Narrowphase (CPU):** Solo se ejecuta cuando el Broadphase termina. Realiza matemática precisa para detectar la profundidad de penetración exacta.
*   **DarkTransformSoA (Struct-of-Arrays):** La memoria final escrita no es un objeto, sino arreglos primitivos crudos (`double[]`) alineados a las líneas de caché de 64 bytes de la CPU.
