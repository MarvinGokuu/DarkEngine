# 🌋 VULKAN MIGRATION MASTER PLAN: The Zero-Debt Architecture

**Clasificación:** Confidencial / Arquitectura Core  
**Estado:** Debate Abierto (Requiere Aprobación del CEO)

Me pediste que no alucine, que no sea un yes-man y que analice exhaustivamente cómo dar el salto a **Vulkan (Java 22 FFM API)** sin que el motor colapse en producción en 10 días. Tras auditar la base de código actual mediante escáneres de sintaxis FFI, he detectado la *deuda técnica subyacente* que podría ser fatal. Aquí tienes la realidad cruda.

---

## 1. El Diagnóstico Actual: Acoplamiento Mortal
Nuestro motor es una bestia asíncrona de alto rendimiento que se comunica directamente por FFI (Project Panama). No dependemos de LWJGL para la lógica central. 

Sin embargo, he detectado que **11 sistemas centrales del motor están fuertemente acoplados a OpenGL**.
Archivos como `SpatialHashGrid.java`, `DarkDeferredLightingSystem.java`, y `DarkFSRSystem.java` hacen llamadas FFI **directas** a funciones de C como `glCreateShader`, `glBindBuffer` y `glDispatchCompute`.

**¿El Problema?**
Si cambiamos a Vulkan mañana, tendríamos que **borrar y reescribir por completo esos 11 archivos de físicas y renderizado**. Eso es un fracaso arquitectónico. Un sistema de colisiones (SpatialHash) no debería saber qué API gráfica se está usando, solo debería decirle a la GPU "calcula esto".

---

## 2. El Patrón de Oro (The RHI Layer)
Si miras motores AAA como Unreal Engine 5 o DOOM (idTech 7), nunca llaman a Vulkan u OpenGL directamente en la capa lógica. Usan una **RHI (Render Hardware Interface)**.

Para hacer una transición a Vulkan *sin errores*, **DEBEMOS** inyectar una capa RHI en el DarkEngine antes de migrar. 

### Lo que DEBE CAMBIAR (Refactor RHI):
1. **Extracción de Comandos:** En lugar de que `DarkFSRSystem` llame a `DarkOpenGLLinker.glDispatchCompute()`, debe llamar a un abstracto `DarkCommandBuffer.dispatch(x, y, z)`.
2. **Contexto de Ventana:** `DarkEngineWindow` debe dejar de pedir un contexto OpenGL a GLFW y simplemente pedirle un puntero de superficie genérica (`glfwCreateWindowSurface`).
3. **Manejo de Pipelines:** Vulkan usa *Pipelines* (Estados inmutables pre-compilados). OpenGL usa máquinas de estado mutables (`glEnable`, `glUseProgram`). El RHI debe forzar un comportamiento inmutable desde Java para obligarnos a pensar en Vulkan desde hoy.

### Lo que se MANTIENE INTACTO (Zero-GC Win):
1. **Memoria Pura (Panama):** El 100% de los arreglos primitivos (`DarkTransformSoA`, SSBOs, buffers de Luces). La forma en que inyectamos bytes crudos a C no cambia en lo absoluto.
2. **El DAG (DarkTaskDispatcher):** El gestor de hilos Lock-Free funcionará igual, despachando Comandos Gráficos en hilos paralelos. Vulkan, de hecho, se beneficia inmensamente de esto porque permite compilar `VkCommandBuffers` en múltiples núcleos a la vez (algo que OpenGL prohíbe).

---

## 3. Las Trampas de Vulkan (Por qué crashearía en 10 días)
No te voy a mentir, Vulkan no te perdona nada. Aquí están las dos cosas ocultas que destrozan motores indie si no se diseñan bien desde el día cero:

1. **La Recreación del Swapchain (Resolución Dinámica):**
   - En OpenGL, si minimizas la ventana, el SO se encarga. 
   - En Vulkan, minimizar la ventana, cambiar de monitor o cambiar la resolución invalida instantáneamente la memoria de la VRAM. Si el motor envía un frame en ese nanosegundo, **Pantalla Azul o Crash de GPU (VK_ERROR_OUT_OF_DATE_KHR)**.
   - *Solución:* Necesitamos un Gestor de Resolución (`DarkDisplayConfig`) que detenga por completo el anillo del DAG Dispatcher de forma atómica antes de que Vulkan mate el Swapchain.

2. **Sincronización Host-Device (Semáforos y Fences):**
   - En OpenGL, `glMemoryBarrier` frena a la CPU automáticamente si es necesario.
   - En Vulkan, si Java sobrescribe un `MemorySegment` de las físicas mientras la GPU aún lo está leyendo del frame anterior, ocurren *Artefactos Visuales Aleatorios*.
   - *Solución:* Un sistema de *Double Buffering* o *Triple Buffering* obligatorio en los SSBOs (Luces y Transformaciones), gestionado por `VkFence` desde Panama.

---

## 4. El Debate de Migración (Tu Turno, CEO)

Jefe, esta es la encrucijada arquitectónica. Tenemos dos caminos para abordar el salto:

**Ruta A (The Clean Slate):** 
Detenemos temporalmente las nuevas funciones visuales de la Fase 27. Refactorizamos los 11 archivos acoplados actuales para que pasen por la nueva arquitectura **DarkRHI**. Una vez purgado el acoplamiento de OpenGL, inyectar el backend de Vulkan será tan fácil como enchufar un módulo nuevo. (Cero Deuda Técnica, pero retrasa el FSR).

**Ruta B (The Prototype Path):**
Terminamos la Fase 27 (G-Buffer, FSR, Luces Múltiples) en crudo con OpenGL para tener el producto visual listo. Luego, en la Fase 28, hacemos una gran quema de código donde extraemos todo hacia el RHI de golpe para preparar la entrada de Vulkan. (Resultados visuales rápidos, pero con deuda técnica acumulada en la rama).

¿Cómo quieres jugar esta partida táctica?
