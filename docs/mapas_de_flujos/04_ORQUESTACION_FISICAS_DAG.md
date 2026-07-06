# 🗺️ Mapa del Flujo de Físicas y Orquestación DAG (Fase 4)

Este diagrama documenta la arquitectura de ejecución asíncrona del DarkEngine. El motor no usa barreras globales ni pausas. Utiliza un Grafo Acíclico Dirigido (DAG) para disparar hilos trabajadores (Worker Threads) elásticamente en cuanto sus dependencias de datos (Data-Oriented) son resueltas.

<div align="center">

```mermaid
graph TD
    %% Inicialización
    subgraph init [1. Arranque de Ciclo]
        A(("TimeKeeper<br/>Tick Delta")) --> B{"DarkTaskDispatcher<br/>Dual MPMC Ring Buffer"}
        B -->|Canal A| B1[workerQueue]
        B -->|Canal B| B2[mainThreadQueue]
    end

    %% Tareas Paralelas sin Dependencias (Se ejecutan al mismo tiempo en N Cores)
    subgraph parallel [2. Tareas Libres (Workers)]
        B1 --> C[DarkInputSystem]
        B1 --> D[DarkCameraState]
        B1 --> E[SpatialHashGrid]
    end

    %% Tareas Secuenciales Dependientes (Físicas)
    subgraph physics [3. Pipeline de Físicas SIMD]
        E --> F[NarrowphaseSystem]
        F -->|Calcula fuerzas de impacto| G[SceneKinematicsSystem]
        G -->|Integra Posición| H[(DarkTransformSoA)]
    end

    %% Tareas con Afinidad de Hilo (Main Thread)
    subgraph opengl [4. Tareas Gráficas FFI (RHI)]
        B2 -.->|Afinidad CPU 0| I["BroadphaseSystem<br/>Compute Shader"]
        B2 -.->|Afinidad CPU 0| J["GPUParticleSystem<br/>Compute Shader"]
    end

    %% Sincronización Final
    subgraph sync [5. Cierre de Estado]
        H --> K{Sincronización Lock-Free}
        C --> K
        D --> K
        K --> L((Fin del Frame Físico))
    end

    %% Estilos AAA
    classDef dispatcher fill:#c0392b,stroke:#e74c3c,stroke-width:3px,color:#fff;
    classDef worker fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef physics fill:#27ae60,stroke:#2ecc71,stroke-width:2px,color:#fff;
    classDef memory fill:#8e44ad,stroke:#9b59b6,stroke-width:4px,color:#fff;
    classDef opengl fill:#e67e22,stroke:#d35400,stroke-width:2px,color:#fff,stroke-dasharray: 5 5;
    classDef queue fill:#d35400,stroke:#e67e22,stroke-width:2px,color:#fff;

    class B,K dispatcher;
    class B1,B2 queue;
    class C,D,E worker;
    class F,G physics;
    class H memory;
    class I,J opengl;
```

</div>

## Leyenda Técnica:
*   **DarkTaskDispatcher (Dual Ring Buffer):** El "cerebro" multicore. Implementa dos arreglos MPMC lock-free (Vyukov Bounded Queues) con un Stride de 64-bytes. 
*   **workerQueue:** Cola Lock-Free de tareas agnósticas (Físicas, Transformaciones) consumidas por los *Workers* paralelos.
*   **mainThreadQueue:** Cola de tareas que obligatoriamente requieren *Afinidad de Hilo* (Thread Affinity). Contiene las llamadas gráficas FFI para aislar OpenGL/Vulkan al hilo de la ventana y prevenir el crasheo `EXCEPTION_ACCESS_VIOLATION` en el driver de Nvidia.
*   **Broadphase / Narrowphase:** Físicas ejecutadas en múltiples capas (VRAM -> RAM -> CPU SIMD).
*   **DarkTransformSoA:** Memoria final en arreglos primitivos alineados a 64 bytes.
