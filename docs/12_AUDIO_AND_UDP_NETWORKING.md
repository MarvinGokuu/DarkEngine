# 12. Audio Espacial y Networking Data-Oriented (Phase 33)

El motor Dark Engine no delega funciones críticas a librerías de Java pesadas que consuman memoria dinámica; esto incluye el procesamiento de Sonido Tridimensional y la comunicación de Sockets para el Multijugador.

## Motor de Audio Abstracto (Zero-Coupling con OpenAL)
El sistema de sonido está diseñado de forma agnóstica (`DarkAudioContext`). En PC, la implementación activa es `DarkOpenALBackend`, que enlaza la librería nativa de C **OpenAL Soft** mediante *Project Panama* (FFI).
- **DarkAudioSourceSoA**: Asigna de forma estática 1024 fuentes de sonido simultáneas en memoria Off-Heap.
- **Sincronización Física y DAG (Roadmap)**: Durante el Main Loop, el motor lee las posiciones espaciales de la ECS (Entity Component System) y las inyecta crudas a OpenAL mediante la llamada a sistema de C (`alSource3f`). En el futuro, el *Task Graph (DAG)* agendará la computación de audio espacial estricta dependiente del fin del cálculo de Oclusión por Geometría.
- **HRTF y Doppler**: Esto habilita sonido binaural tridimensional auténtico, reverberación de entorno (EFX) y efecto Doppler por velocidad sin crear un solo objeto Java.

## Servidor Autoritario Zero-Allocation (UDP)
Las arquitecturas estándar de Red de Java instancian un objeto `InetSocketAddress` o generan un array de bytes por cada paquete de Red recibido. A 60 actualizaciones por segundo, con 100 jugadores, esto dispara el Garbage Collector letalmente.

Para evadir esta arquitectura:
1. **Canales Conectados (Connected DatagramChannels)**: Dark Engine fija el enchufe UDP directamente a la IP objetivo (`channel.connect()`).
2. **Lectura/Escritura Rápida (Fast-Path)**: Al estar conectado, se omite la capa envolvente de Java y se usan comandos directos del sistema operativo (`channel.read(ByteBuffer)`), reduciendo la asignación de memoria dinámica (GC Allocations) a cero absoluto.
3. **Serialización ECS Plana**: `NetworkReplicationSystem` mapea las estructuras de posiciones y entidades desde el bloque nativo y las vuelca crudas a un payload binario que se dispara por la red. Cero serialización XML/JSON. Cero latencia.
