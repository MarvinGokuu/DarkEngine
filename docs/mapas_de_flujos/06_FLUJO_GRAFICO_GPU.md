# 🗺️ Mapa del Flujo Gráfico de la GPU (Fase 27)

El siguiente diagrama detalla la arquitectura de renderizado diferido y escalado espacial (FSR) que se ejecuta en cada ciclo (frame) dentro de la tarjeta gráfica (VRAM). 

El flujo está diseñado para garantizar latencia ultra-baja y cero recolección de basura (Zero-GC), utilizando Memoria Off-Heap y despachos de Compute Shaders.

<div align="center">

```mermaid
graph TD
    subgraph fase1 [Fase 1: Pase de Geometría]
        A[DarkGeometrySystem] -->|Dibuja Modelos 3D| B(G-Buffer FBO 720p)
        B -->|Genera MRT 0| C1[Albedo Tex]
        B -->|Genera MRT 1| C2[Normal Tex]
        B -->|Genera MRT 2| C3[PBR Tex]
    end

    subgraph fase2 [Fase 2: Iluminación Diferida]
        C1 --> D{DarkDeferredLightingSystem}
        C2 --> D
        C3 --> D
        ECS((ECS: sunDir, camPos)) -.->|Inyección| D
        D -->|GGX PBR| E[Lit HDR Tex 720p]
    end

    subgraph fase3 [Fase 3: Post-Procesado y Escalado]
        E --> F{DarkPostProcessSystem}
        F -->|ACES Filmic + Bloom| E2[Processed Tex 720p]
        E2 --> G{DarkFSRSystem}
        G -->|EASU + RCAS| H((Presentation FBO 4K))
    end
    
    classDef sys fill:#1e1e1e,stroke:#f39c12,stroke-width:2px,color:#fff;
    classDef tex fill:#2c3e50,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef fbo fill:#27ae60,stroke:#2ecc71,stroke-width:4px,color:#fff;
    classDef ecs fill:#8e44ad,stroke:#9b59b6,stroke-width:2px,color:#fff;
    
    class A,D,F,G sys;
    class C1,C2,C3,E,E2 tex;
    class B,H fbo;
    class ECS ecs;
```

</div>

## Leyenda Técnica:
*   **MRT (Multiple Render Targets):** Permite al motor rasterizar múltiples texturas en un solo pase de geometría.
*   **HDR (High Dynamic Range):** La luz se calcula con valores superiores a 1.0 para simular fotones reales.
*   **ACES Filmic:** Algoritmo estándar del cine para mapear HDR a los colores LDR que muestra tu monitor.
*   **EASU/RCAS:** Los dos algoritmos proxy que conforman el FidelityFX Super Resolution de AMD para reconstruir bordes y aplicar nitidez en resolución 4K.
