# 08 - NATIVE GRAPHICS & IMGUI FFI (Fase 9%)

Este documento describe la arquitectura técnica detrás del renderizado visual y la interfaz de usuario nativa de DarkEngine.

## El Problema del Renderizado Java (AWT/Swing/JavaFX)
Las librerías de interfaz gráfica estándar de Java inyectan capas masivas de abstracción y delegan el renderizado a un hilo separado (el Event Dispatch Thread o EDT). Esto genera:
1. **Input Lag**: Retrasos de 15ms a 30ms en capturar la pulsación de una tecla y reflejarla en la pantalla.
2. **Garbage Collection**: Creación de miles de objetos (`MouseEvent`, `KeyEvent`, rectángulos) que disparan pausas GC.
3. **Pérdida de Control**: El motor no puede sincronizar el refresco de pantalla (V-Sync) con precisión de nanosegundos respecto a la actualización de físicas.

## La Solución DarkEngine: FFI Directo

### 1. Sistema de Plataforma Agnóstico (Zero-Coupling)
En lugar de usar `JFrame` o anclarse directamente a GLFW, DarkEngine delega el control a un `DarkPlatformContext` abstracto. La implementación FFI (Project Panama) de GLFW interactúa con `glfw3.dll` de forma transparente, permitiendo ports futuros a consolas sin alterar el `EngineKernel`. 
- La creación de la ventana es nativa y puramente agnóstica.
- El polling de eventos (`glfwPollEvents`) y el swap de buffers (`glfwSwapBuffers`) están inyectados **dentro del Hot-Path del Kernel (EngineKernel)**.
- **Zero-Contention Latch**: El estado de los periféricos capturado se deposita en un Shadow Buffer (memoria nativa fuera del Heap) y se transfiere de inmediato al `currentState` del Vault mediante un barrido vectorial masivo (`MemorySegment.copy()`). Esto previene la disrupción del Caché L1 y erradica los locks atómicos entre el Hilo del Juego y la UI.
- Resultado: **0ms Input Lag** y control absoluto del frame.

### 2. Dear ImGui y DarkUIContext (Editor AAA)
Para proveer herramientas de desarrollo profesionales, se ha integrado un contexto de interfaz `DarkUIContext`. La implementación base es **Dear ImGui**, el estándar C++ de la industria AAA, conectada mediante Project Panama a `cimgui.dll` sin puentes intermedios pesados.
- La interfaz de usuario reside íntegramente en la VRAM de la GPU.
- El motor simplemente inyecta comandos de dibujado atómicos.
- **Zero-GC**: No instanciamos botones ni paneles en el Heap de Java.

### 3. Render Hardware Interface (RHI) - Zero-JAR Abstraction
Para garantizar portabilidad y desacoplamiento, el motor interactúa con la VRAM a través de la abstracción genérica `DarkRHI` (Device y CommandList). Actualmente, el motor inyecta `DarkOpenGLBackend` (enlazado directamente a `opengl32.dll`), pero la arquitectura dicta que cualquier backend es plug-and-play.
- **Hot-Swapping hacia Vulkan**: Al usar Panama (FFI dinámico) en lugar de dependencias JNI pesadas (ej. LWJGL), adoptar Vulkan solo requerirá construir un `DarkVulkanBackend` (enlazando `vulkan-1.dll`). El cambio de API gráfica será dictado por un flag de configuración, sin tocar el código del juego.
- **Graceful Shutdown & Zero-Leak**: La capa RHI maneja internamente la limpieza de memoria no administrada, suprimiendo de forma nativa excepciones silenciosas para garantizar que el apagado borre el 100% de la VRAM (FBOs, SSBOs) sin colapsos.

## Futuro (Roadmap TDR y Assets)
El chasis RHI provee el contexto gráfico. En futuras fases, el motor inyecta recursos binarios pre-compilados (`.darkasset`) directamente a la GPU, y utiliza Compute Shaders para el renderizado masivo diferido (Deferred Pipeline).

**Resiliencia TDR (Roadmap):** Se requerirá utilizar extensiones como `GL_ARB_robustness` para detectar si el driver de video crashea (Timeout Detection and Recovery). El motor pausará, recreará el contexto gráfico en C++, y resumirá el juego sin destruir la sesión JVM.
