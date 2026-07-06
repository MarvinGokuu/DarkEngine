# 🗺️ Mapa de Asset Streaming y Zero-Copy (Capa 3: Magia Sensorial)

En un motor AAA, detener el juego para cargar una textura de 100 MB mostraría una pantalla de carga. Para lograr mundos abiertos y tiempos de carga imperceptibles, DarkEngine utiliza **Streaming Asíncrono** y transferencias de memoria directa por DMA (Direct Memory Access).

<div align="center">

```mermaid
graph TD
    subgraph disco [SSD / NVMe]
        A[(Archivo de Textura / Modelo 3D)]
    end

    subgraph cpu_async [Hilo Secundario: DarkAssetStreamer]
        A -.->|FileChannel.transferTo| B[Memoria Virtual OS]
        B -->|Slice Header 'DARK\0'| C[(MemorySegment Off-Heap)]
        note1>¡Cero arrays de bytes de Java!<br/>Se salta la memoria Heap por completo]
    end

    subgraph gpu_dma [GPU Bus: DarkGPUStreamer]
        C -->|Puntero Directo| D{API Vulkan / OpenGL}
        D == DMA Bus PCI-E ==> E[(Memoria VRAM)]
    end

    %% Tiempos de ejecución
    subgraph game_loop [Hilo Principal: Render Loop]
        F(DarkScene) -->|Verifica si está listo| E
        E -->|Textura lista| G[Dibuja en Pantalla]
    end

    %% Estilos AAA
    classDef ssd fill:#2c3e50,stroke:#34495e,stroke-width:2px,color:#fff;
    classDef memory fill:#27ae60,stroke:#2ecc71,stroke-width:3px,color:#fff;
    classDef gpu fill:#8e44ad,stroke:#9b59b6,stroke-width:3px,color:#fff;
    classDef render fill:#c0392b,stroke:#e74c3c,stroke-width:2px,color:#fff;

    class A disco ssd;
    class B,C cpu_async memory;
    class D,E gpu_dma gpu;
    class F,G game_loop render;
```

</div>

## Leyenda Técnica:
*   **FileChannel.transferTo (MMAP):** Técnica de kernel donde el SO proyecta el disco duro directamente sobre la RAM, permitiendo acceso a velocidad nativa sin el destructivo método `Files.readAllBytes`.
*   **Slice Header 'DARK\0':** El streamer corta los primeros 9 bytes de firma del binario `.darkasset` de forma virtual usando Off-Heap slicing, enviando solo el Payload puro a la VRAM.
*   **DMA (Direct Memory Access):** La tarjeta gráfica extrae los datos de la textura de la RAM por su cuenta, sin que la CPU tenga que enviárselos byte por byte.
*   **Zero-Copy:** El concepto supremo. Los bytes del modelo 3D viajan del SSD a la Pantalla sin ser copiados innecesariamente en memorias intermedias de Java.
