# 08 - NATIVE GRAPHICS & IMGUI FFI (Fase 9%)

Este documento describe la arquitectura técnica detrás del renderizado visual y la interfaz de usuario nativa de DarkEngine.

## El Problema del Renderizado Java (AWT/Swing/JavaFX)
Las librerías de interfaz gráfica estándar de Java inyectan capas masivas de abstracción y delegan el renderizado a un hilo separado (el Event Dispatch Thread o EDT). Esto genera:
1. **Input Lag**: Retrasos de 15ms a 30ms en capturar la pulsación de una tecla y reflejarla en la pantalla.
2. **Garbage Collection**: Creación de miles de objetos (`MouseEvent`, `KeyEvent`, rectángulos) que disparan pausas GC.
3. **Pérdida de Control**: El motor no puede sincronizar el refresco de pantalla (V-Sync) con precisión de nanosegundos respecto a la actualización de físicas.

## La Solución DarkEngine: FFI Directo

### 1. GLFW (Lienzo Nativo)
En lugar de usar `JFrame`, DarkEngine usa **Project Panama (Foreign Function Interface)** para comunicarse directamente con `glfw3.dll` en Windows o `.so` en Linux. 
- La creación de la ventana es 100% nativa.
- El polling de eventos (`glfwPollEvents`) y el swap de buffers (`glfwSwapBuffers`) están inyectados **dentro del Hot-Path del Kernel (EngineKernel)**.
- **Zero-Contention Latch**: El estado de los periféricos capturado se deposita en un Shadow Buffer (memoria nativa fuera del Heap) y se transfiere de inmediato al `currentState` del Vault mediante un barrido vectorial masivo (`MemorySegment.copy()`). Esto previene la disrupción del Caché L1 y erradica los locks atómicos entre el Hilo del Juego y la UI.
- Resultado: **0ms Input Lag** y control absoluto del frame.

### 2. Dear ImGui (Editor AAA)
Para proveer herramientas de desarrollo profesionales, se ha integrado **Dear ImGui**, el estándar C++ de la industria AAA.
A través de `DarkImGuiLinker`, conectamos la librería `cimgui.dll` sin utilizar puentes intermedios pesados como JNI.
- La interfaz de usuario reside íntegramente en la VRAM de la GPU.
- El motor simplemente inyecta comandos de dibujado atómicos.
- **Zero-GC**: No instanciamos botones ni paneles en el Heap de Java.

## Futuro (Fase 21% y 27%, Roadmap TDR)
El chasis actual provee el contexto OpenGL. En futuras fases, el motor usará este mismo puente FFI para inyectar recursos binarios pre-compilados (`.darkasset`) directamente a la GPU, y utilizará Compute Shaders para el renderizado masivo diferido (Deferred Pipeline).

**Resiliencia TDR (Roadmap):** Se requerirá utilizar puentes FFI hacia extensiones como `GL_ARB_robustness` para detectar si el driver de video crashea (TDR - Timeout Detection and Recovery) por exceso de Compute Shaders. El motor deberá pausar silenciosamente, recrear el contexto gráfico en C++, recargar los SSBOs y resumir el juego sin destruir la sesión de JVM del usuario.
