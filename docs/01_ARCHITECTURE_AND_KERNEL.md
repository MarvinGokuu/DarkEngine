# 🧠 01. ARCHITECTURE AND KERNEL

El **DarkEngine Kernel** es el orquestador maestro del ciclo de vida del motor. A diferencia de arquitecturas OOP pesadas, este Kernel opera como un micro-controlador incrustado: asume el control del hilo principal (Thread Pinning), inyecta sistemas en arrays primitivos (para Loop Unrolling), e inicializa subsistemas sin recolección de basura.

## Jerarquía de Ejecución

La cadena de mando está estrictamente jerarquizada para garantizar que ninguna instrucción de usuario interfiera con la telemetría y el pipeline de hardware.

1. **`DarkEngineMaster`**: El "Dios" del sistema. Posee el método `main`. Prepara el terreno (Vault, Dispatcher, Kernel) y arranca el hilo de alta prioridad. Inyecta el `DarkEngineWindow` como UI.
2. **`EngineKernel`**: El procesador central. Atado al núcleo físico (vía `ThreadPinning`). Posee el *Main Loop* de 60 FPS (o deslimitado). Manda señales al `DarkEventDispatcher` y despacha los ticks a los sistemas de usuario.
3. **`SystemRegistry`**: El administrador de memoria y ejecución de código de usuario. Todos los *GameSystems* y *RenderSystems* deben registrarse aquí. 
4. **`SystemStateManager`**: Capa superior para manejar transiciones (Ej: `High Performance Mode` en Windows llamando a `PowrProf.dll`).

## Secuencia de Arranque: Ultra Fast Boot

El motor se niega a arrancar de forma perezosa (Lazy Loading). El arranque, encapsulado en `UltraFastBootSequence`, es un asalto directo a la memoria.

1. **Pre-calentamiento del JIT (C2 Compiler)**: Envía eventos señuelo al `DarkAtomicBus` durante la fase de inicialización.
2. **Integridad Estructural**: Mide la latencia de respuesta del bus usando temporizadores de nanosegundos (`System.nanoTime()`). 
3. **Certificación**: Si el bus no es capaz de responder en < 150ns, el boot emite una advertencia o falla. Si lo logra en < 1ms total, recibe la certificación AAA+.

## SystemRegistry y Loop Unrolling

En lugar de recorrer `ArrayList<System>` utilizando iteradores que contaminan las cachés L1, el `SystemRegistry` consolida todos los sistemas en arreglos primitivos estáticos.
Esto le permite a la JVM realizar "Loop Unrolling", desenrollando los ciclos `for` a nivel de ensamblador, haciendo que llamar a 10 sistemas tome el mismo tiempo que llamar a 1 macro-sistema monolítico. Adicionalmente, los sistemas no guardan estado; operan sobre memorias planas **Structure of Arrays (SoA)** (como `DarkTransformSoA`) utilizando procesadores SIMD para máximo rendimiento.

### Directed Acyclic Graph (DAG) Task Dispatcher
Actualmente el motor implementa el **`DarkTaskDispatcher`**. Un grafo de dependencias asíncrono basado en un *Vyukov Bounded MPMC Ring Buffer* lock-free. Los hilos no esperan en barreras globales ni por capas; cada tarea de física o renderizado se desbloquea atómicamente cuando sus predecesores terminan, logrando una saturación del 100% de la CPU multicore sin tiempos muertos ni cuellos de botella de sincronización.

<div align="center">

```mermaid
graph TD
    %% Inicialización
    subgraph init [1. Arranque de Ciclo]
        A((TimeKeeper<br/>Tick Delta)) --> B{DarkTaskDispatcher<br/>Dual Ring Buffer}
    end

    %% Tareas Paralelas sin Dependencias (Se ejecutan al mismo tiempo en N Cores)
    subgraph parallel [2. Tareas Libres (Workers)]
        B --> C[DarkInputSystem]
        B --> D[DarkCameraState]
        B --> E[SpatialHashGrid]
    end

    %% Tareas Secuenciales Dependientes (Físicas)
    subgraph physics [3. Pipeline de Físicas SIMD]
        E --> F[NarrowphaseSystem]
        F -->|Calcula fuerzas de impacto| G[SceneKinematicsSystem]
        G -->|Integra Posición| H[(DarkTransformSoA)]
    end

    %% Tareas con Afinidad de Hilo (Main Thread)
    subgraph opengl [4. Tareas Gráficas FFI (RHI)]
        B -.->|Requires Main Thread| I[BroadphaseSystem<br/>Compute Shader]
        B -.->|Requires Main Thread| J[GPUParticleSystem<br/>Compute Shader]
    end

    %% Sincronización Final
    subgraph sync [5. Cierre de Estado]
        H --> K{Sincronización Lock-Free}
        C --> K
        D --> K
        I --> K
        J --> K
        K --> L((Fin del Frame Físico))
    end

    %% Estilos AAA
    classDef dispatcher fill:#c0392b,stroke:#e74c3c,stroke-width:3px,color:#fff;
    classDef worker fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef physics fill:#27ae60,stroke:#2ecc71,stroke-width:2px,color:#fff;
    classDef memory fill:#8e44ad,stroke:#9b59b6,stroke-width:4px,color:#fff;
    classDef opengl fill:#e67e22,stroke:#d35400,stroke-width:2px,color:#fff,stroke-dasharray: 5 5;

    class B,K dispatcher;
    class C,D,E worker;
    class F,G physics;
    class H memory;
    class I,J opengl;
```

</div>

**Leyenda Técnica:**
*   **DarkTaskDispatcher (Dual Ring Buffer):** El "cerebro" multicore. Implementa dos arreglos MPMC lock-free (Vyukov Bounded Queues) con un Stride de 64-bytes. Uno envía las tareas agnósticas a los *Workers* y el otro (`mainThreadQueue`) envía las llamadas gráficas FFI exclusivas al Hilo Principal para evadir bloqueos en la API Gráfica (OpenGL/Vulkan).
*   **Broadphase (GPU Radix Sort):** Filtra descartes masivos (quién está cerca de quién) procesando miles de colisionadores en paralelo en la VRAM.
*   **Narrowphase (CPU):** Solo se ejecuta cuando el Broadphase termina. Realiza matemática precisa para detectar la profundidad de penetración exacta.
*   **DarkTransformSoA (Struct-of-Arrays):** La memoria final escrita no es un objeto, sino arreglos primitivos crudos (`double[]`) alineados a las líneas de caché de 64 bytes de la CPU.

### Precisión del Mundo: Large World Coordinates (LWC)
Para evitar el "Vertex Jitter" (temblor en flotantes a grandes distancias), el core asume la migración a coordenadas de 64 bits (`double`) para la lógica, las cuales se restan contra la cámara antes de ser subidas como matrices de `float` (32 bits) a la GPU, habilitando mundos abiertos expansivos.

## GPU-Driven Engine (El Asesino de la CPU)
El Kernel delega por completo las matemáticas espaciales masivas (ej. Frustum Culling) a la tarjeta gráfica. Utilizando enlaces nativos FFI (`DarkOpenGLLinker`), el motor transfiere la memoria cruda del `DarkTransformSoA` hacia *Shader Storage Buffer Objects* (SSBO) en la VRAM. Posteriormente, despacha miles de *Compute Shaders* paralelos que resuelven colisiones y descartes, dejando a la CPU 100% dedicada a la Inteligencia Artificial y reglas de juego.

## El Gobernador de Energía (TimeKeeper)

El `TimeKeeper` implementa un *Governor* mecánico:
- Si el motor termina su trabajo antes de su presupuesto de 16.6ms (para 60FPS), no utiliza `Thread.sleep` (lo cual entrega el hilo al SO y arruina la caché).
- En cambio, hace un *Spin-Wait* dinámico o utiliza un *ParkNanos* agresivo para mantenerse despierto y vigilante. 

## Graceful Shutdown (Zero-Deadlock & VRAM Leak Prevention)
Cuando el motor recibe la orden de detención (ej. el usuario cierra la ventana GLFW), el `SystemStateManager` y el `EngineKernel` cortan la ingesta de nuevos eventos y detienen el Bucle Principal. Posteriormente, el `DarkEventDispatcher` apaga los buses atómicos purificando instantáneamente la memoria sobrante (`clear()`) sin emplear esperas giratorias (*Spin-Waits*), evitando deadlocks que podrían colgar la Máquina Virtual. Finalmente, se cierran las *Arenas*, se libera la memoria *Off-Heap* en el `SectorMemoryVault`, y la tarjeta de audio OpenAL se desconecta del hardware nativamente. 

**Regla de Hierro (VRAM Leak):** Durante esta purga o "Poison Pill", el Kernel invoca estrictamente el método `destroy()` de todos los `GameSystem` registrados, forzándolos a ejecutar `glDeleteProgram` y `glDeleteBuffers` vía FFI, lo que erradica por completo procesos zombies y fugas de descriptores en los drivers de la tarjeta gráfica.

**El Seguro Terminator (`halt(0)`):** Al culminar el flujo de apagado, el `EngineKernel` invoca `Runtime.getRuntime().halt(0)`. Esta acción es obligatoria en la arquitectura de DarkEngine, ya que finaliza abruptamente la JVM sin esperar los ganchos de apagado tradicionales. Esto previene deadlocks terminales provocados por consumidores asíncronos en bucles `while(true)` (como el plano de control NIO Telemetry) que podrían resistirse a morir pacíficamente, dejando el proceso colgado en el SO.
