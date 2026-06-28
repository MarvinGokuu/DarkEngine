# 🗺️ Mapa del Puente Nativo FFI (Capa 1: Cimientos)

El DarkEngine no utiliza JNI (Java Native Interface) por su masivo costo de rendimiento (latencia en cada llamada a C++). En su lugar, utilizamos el **Foreign Function & Memory API (FFI - Project Panama)**.

Esto permite que nuestro código Java invoque funciones de OpenGL, GLFW y Dear ImGui nativo con la misma velocidad y eficiencia que si estuviéramos programando en C o Rust.

<div align="center">

```mermaid
graph TD
    subgraph java_side [El Motor Java]
        A(DarkImGuiRenderer)
        B(DarkOpenGLLinker)
        C[(Memoria Off-Heap<br/>MemorySegment)]
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
    B -->|1. Invoca OpenGL| D
    D -->|2. Llama a Puntero de Función| F
    D -->|2. Llama a Puntero de Función| G

    %% Memoria Compartida sin Copias
    F -.->|3. Escribe en puntero| C
    G -.->|3. Lee de puntero| C

    %% Estilos AAA
    classDef java fill:#c0392b,stroke:#e74c3c,stroke-width:2px,color:#fff;
    classDef panama fill:#8e44ad,stroke:#9b59b6,stroke-width:3px,color:#fff;
    classDef native fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;

    class A,B,C java;
    class D,E panama;
    class F,G,H native;
```

</div>

## Leyenda Técnica:
*   **MethodHandle Downcall:** Java compila en tiempo de ejecución (JIT) un acceso directo al código ensamblador de la librería `.dll`, esquivando toda la burocracia de JNI.
*   **Memoria Compartida:** En motores viejos, mandar un arreglo a OpenGL requería duplicar los datos. Aquí, le pasamos a C++ la dirección física exacta de la RAM (`MemorySegment.address()`), logrando una transferencia a costo cero.
