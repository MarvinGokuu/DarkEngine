# 09 - ASSET COMPILER & ZERO-COPY STREAMING (FASE 21)

**Última Actualización:** 2026-06-14
**Certificación:** AAA+ (Zero-GC, Direct Memory Access)
**Módulos Clave:** `DarkAssetCompiler.java`, `DarkAssetStreamer.java`, `DarkEngineWindow.java`

## 1. El Problema de la Lectura Tradicional
En motores basados en Java, la lectura de assets pesados (modelos `.fbx`, texturas `.png`) usando librerías estándar como `ImageIO` o `FileInputStream` crea un cuello de botella fatal:
1. Convierte bytes del disco en miles de objetos Java temporales (Arreglos de bytes, buffers indirectos).
2. Dispara el Garbage Collector (GC), congelando el *Main Thread* (Lag spikes).
3. Contamina el Heap de Java con memoria que de todos modos debe copiarse a la VRAM (Double-Copy).

## 2. La Solución DarkEngine: Separación de Responsabilidades

Para mantener nuestra filosofía de *Latencia Cero* y *Zero-GC*, separamos el proceso en dos capas totalmente independientes:

### 2.1. El Compilador Offline (DarkAssetCompiler)
En lugar de forzar al motor a entender archivos complejos en tiempo real, construimos un compilador que se ejecuta en un hilo virtual aislado (Project Loom). 
Cuando el creador arrastra y suelta un archivo a la ventana GLFW, este compilador:
- Extrae el payload crudo del modelo/imagen.
- Elimina cabeceras y metadatos basura.
- Escribe un binario plano de extensión `.darkasset` que representa bloques de memoria lineal exactos a como los lee la GPU.

### 2.2. Zero-Copy Streaming (DarkAssetStreamer)
Cuando el motor necesita el asset, **nunca** crea un objeto en Java. Utiliza `java.nio.channels.FileChannel.map` acoplado a un `MemorySegment` de *Project Panama*.
- El sistema operativo mapea el archivo físico del disco NVMe directamente a la memoria virtual del proceso (Direct Memory Access).
- El motor obtiene un puntero nativo (`MemorySegment`) listo para ser consumido por Vulkan u OpenGL.
- Cero recolección de basura, carga instantánea y cero uso de CPU para parsear bytes.

## 3. Integración con Interfaz (FFI Drag & Drop)
Para garantizar el mejor flujo de trabajo (UX/DX), el enrutamiento de assets se controla mediante un *Upcall Stub* nativo enlazado a la función de C++ `glfwSetDropCallback`. Esto permite que el flujo nativo del sistema operativo inyecte la ruta del archivo directo al Hilo Virtual del compilador, manteniendo el input lag del motor gráfico en `0ms`.
