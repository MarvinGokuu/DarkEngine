# 🗺️ Mapa del Bus de Entrada Atómico (Capa 2: Orquestación)

Para garantizar que el motor nunca pierda la pulsación de una tecla o un clic del ratón, el sistema de Input debe ser Lock-Free (sin cerrojos). Si el Sistema Operativo (Windows) notifica un evento mientras el motor está procesando físicas, bloquear el hilo principal causaría *stuttering* (tirones).

Por eso, el DarkEngine utiliza un Bus de Señales implementado con operaciones atómicas (`AtomicInteger`, `VarHandle`).

<div align="center">

```mermaid
graph TD
    subgraph os_thread [Hilo del Sistema Operativo - UI]
        A("Callback GLFW<br/>Mouse / Teclado") -->|Genera Evento| B{Ring Buffer Atómico}
        note1>Escritura ultra rápida<br/>sin usar 'synchronized']
    end

    subgraph game_thread [Hilo Lógico del Motor]
        B -->|Shadow Buffer Latch<br/>MemorySegment.copy| C[DarkInputSystem]
        C -->|Procesa y Despacha| D[DarkSignalCommands]
        D -->|Señal Atómica| E("DarkEngineMaster<br/>Acción Ejecutada")
    end

    %% Estilos AAA
    classDef os fill:#1e1e1e,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef buffer fill:#27ae60,stroke:#2ecc71,stroke-width:4px,color:#fff;
    classDef game fill:#8e44ad,stroke:#9b59b6,stroke-width:2px,color:#fff;

    class A os;
    class B buffer;
    class C,D,E game;
```

</div>

## Leyenda Técnica:
*   **Ring Buffer Atómico (Memory Visibility):** Un arreglo circular en memoria que permite a un hilo escribir (OS) y a otro leer (Motor) al mismo tiempo sin colisionar ni trabarse. Funciona bajo el principio de semántica de memoria *Volatile*. Tras la última auditoría, todo el acceso al arreglo se gestiona explícitamente mediante *Memory Fences* usando `VarHandle.setRelease` y `getAcquire`, aniquilando las condiciones de carrera (Data Races) o lecturas fantasma.
*   **Callback GLFW:** La función en C (FFI) que Windows dispara instantáneamente cada vez que el jugador oprime un botón mecánico en su escritorio.
*   **Shadow Buffer Latch:** El hilo del juego copia masivamente (vía SIMD Vectorizado `MemorySegment.copy`) el estado del buffer directamente a Off-Heap. Evita todo bloqueo (Zero-Contention) contra el Input de GLFW e ImGui.
