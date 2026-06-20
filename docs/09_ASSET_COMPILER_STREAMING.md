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
- Utiliza la API nativa de `FileChannel.transferTo()` para mover los datos puros desde el archivo origen al archivo binario `.darkasset` de destino.
- Elude completamente la clase destructiva `Files.readAllBytes()`, evitando así subir *Arreglos de Bytes* masivos a la memoria Heap de Java, manteniendo un GC (Garbage Collection) en cero absoluto durante la compilación.
- Concatena una firma de metadatos `DARK\0` (9 bytes de cabecera) antes del payload puro.

### 2.2. Zero-Copy Streaming (DarkAssetStreamer)
Cuando el motor necesita el asset, **nunca** crea un objeto en Java. Utiliza `java.nio.channels.FileChannel.map` acoplado a un `MemorySegment` de *Project Panama*.
- El sistema operativo mapea el archivo físico del disco NVMe directamente a la memoria virtual del proceso (Direct Memory Access).
- El Streamer realiza una validación ultra rápida de la firma de metadatos de los primeros 9 bytes, y posteriormente aplica un `MemorySegment.asSlice(9)` para aislar el *Payload* gráfico.
- El motor envía a la tarjeta gráfica (VRAM) **exclusivamente** el segmento cortado, evitando corromper texturas y modelos con metadatos ajenos.
- Cero recolección de basura, carga instantánea y cero uso de CPU para parsear bytes.

## 3. Integración con Interfaz (FFI Drag & Drop)
Para garantizar el mejor flujo de trabajo (UX/DX), el enrutamiento de assets se controla mediante un *Upcall Stub* nativo enlazado a la función de C++ `glfwSetDropCallback`. Esto permite que el flujo nativo del sistema operativo inyecte la ruta del archivo directo al Hilo Virtual del compilador, manteniendo el input lag del motor gráfico en `0ms`.
