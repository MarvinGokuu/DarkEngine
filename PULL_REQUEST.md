# Titulo del Pull Request
[FEAT] Fase 21 Completada: Compilador de Assets y Streaming Zero-Copy (FFI Drag & Drop)

# Descripcion

## Resumen Arquitectonico
Este Pull Request implementa la Fase 21 del roadmap. Construye la capa "digestiva" del motor, permitiendo ingerir assets pesados (.png, .fbx) eludiendo por completo el parseo bloqueante de Java y el Garbage Collector. El proceso es hibrido: una compilacion asincrona y un streaming a memoria nativa en crudo.

## Cambios Principales

### 1. Ingestion Nativa y UI
- Enlace en `DarkGraphicsLinker` de la funcion nativa `glfwSetDropCallback` de C++.
- Modificacion de `DarkEngineWindow` para inyectar un *Upcall Stub* de Project Panama.
- Captura de eventos Drag & Drop directamente desde el OS con 0ms de Input Lag.

### 2. Compilador de Assets Offline (`DarkAssetCompiler`)
- Inyeccion de un hilo virtual (Loom) para ejecutar la extraccion de datos pesados fuera del hilo de renderizado grafico (Main/Hot-Path).
- Creacion del formato de archivos binarios crudos `.darkasset` que almacenan la cabecera magica y el payload de memoria sin metadatos inutiles.

### 3. Streaming Zero-Copy (`DarkAssetStreamer`)
- Uso de `java.nio.channels.FileChannel.map` acoplado a un `MemorySegment` de Project Panama.
- Los archivos compilados son proyectados directamente desde el disco NVMe a la memoria (Direct Memory Access) saltandose la construccion de arreglos y el escaneo del Garbage Collector.

### 4. Construccion y Documentacion
- Adicion de los nuevos modulos al `compile_list.txt`.
- Actualizacion del progreso en `DARK_ENGINE_ROADMAP.md` cruzando el hito global del 35%.

## Criterios de Aceptacion (Certificacion AAA+)
- [x] Zero-GC verificado: `MemorySegment` inyecta la lectura nativa exitosamente.
- [x] Resuelto bug de firma estricta en el `invokeExact` de la Fase 9 (WrongMethodTypeException).
- [x] Historial commiteado estrictamente siguiendo las normas (core, ui, build, docs).
- [x] Ejecucion compilada en entorno AAA+ exitosamente y sin crashes.
