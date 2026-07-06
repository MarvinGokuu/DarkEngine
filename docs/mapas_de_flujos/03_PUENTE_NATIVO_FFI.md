# 🗺️ Mapa del Puente Nativo FFI (Capa 1: Cimientos)

El DarkEngine no utiliza JNI (Java Native Interface) por su masivo costo de rendimiento (latencia en cada llamada a C++). En su lugar, utilizamos el **Foreign Function & Memory API (FFI - Project Panama)**.

Esto permite que nuestro código Java invoque funciones de OpenGL, GLFW y Dear ImGui nativo con la misma velocidad y eficiencia que si estuviéramos programando en C o Rust.

<div align="center">

```mermaid
graph TD
    subgraph java_side [El Motor Java]
        A(DarkImGuiRenderer)
        B{DarkRHI Abstraction}
        B1[DarkOpenGLBackend]
        C[("Memoria Off-Heap<br/>MemorySegment")]
        B -->|Implementación| B1
    end

    subgraph ffi_bridge [Project Panama FFI]
        D{MethodHandle Downcall}
        E{Linker Upcall}
    end

    subgraph c_side [Bibliotecas Nativas C/C++]
        F[cimgui.dll / .so]
        G[opengl32.dll / libGL.so]
        H[glfw3.dll]
    end

    %% Invocación directa a velocidad de C
    A -->|1. Invoca CImgui| D
    B1 -->|1. Downcall FFI| D
    D -->|2. Puntero Nativo| F
    D -->|2. Puntero Nativo| G

    %% Memoria Compartida sin Copias
    F -.->|3. Escribe en puntero| C
    G -.->|3. Lee de puntero| C

    %% Estilos AAA
    classDef java fill:#c0392b,stroke:#e74c3c,stroke-width:2px,color:#fff;
    classDef panama fill:#8e44ad,stroke:#9b59b6,stroke-width:3px,color:#fff;
    classDef native fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef rhi fill:#d35400,stroke:#e67e22,stroke-width:2px,color:#fff;

    class A,C java;
    class B,B1 rhi;
    class D,E panama;
    class F,G,H native;
```

</div>

## Leyenda Técnica:
*   **MethodHandle Downcall:** Java compila en tiempo de ejecución (JIT) un acceso directo al código ensamblador de la librería `.dll`, esquivando toda la burocracia de JNI.
*   **Memoria Compartida:** En motores viejos, mandar un arreglo a OpenGL requería duplicar los datos. Aquí, le pasamos a C++ la dirección física exacta de la RAM (`MemorySegment.address()`), logrando una transferencia a costo cero.
*   **DarkRHI (Render Hardware Interface):** (Fase 4.5.0+) Capa de abstracción superior que aísla al motor de la implementación gráfica. Dependiendo del Sistema Operativo, `DarkRHI` delegará dinámicamente al backend (ej. `DarkOpenGLBackend`), logrando *Zero-Coupling*.
*   **Trivialización FFI (Global Audit):** Llamadas pesadas al SO o GPU que pueden sufrir demoras (como `glfwPollEvents` o `glfwSwapBuffers` por V-Sync) están decoradas explícitamente con `Linker.Option.critical(false)`. Esto previene que la JVM congele (Pinning) al Recolector de Basura (GC) nativo, evitando Stalls a nivel de sistema operativo.
