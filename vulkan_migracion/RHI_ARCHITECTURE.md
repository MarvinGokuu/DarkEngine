# Arquitectura RHI (Render Hardware Interface) - Fase 28

## Objetivo
La arquitectura `DarkRHI` desacopla los sistemas del motor (Partículas, Animación Esquelética, Pipeline Deferred) de la librería gráfica subyacente. Esto nos permite cambiar entre OpenGL 4.3 FFI y Vulkan (Proyecto Panama) sin tocar una sola línea de código en los sistemas de alto nivel (`GameSystem`).

## Componentes Core
1. **`DarkRHI` (Interfaz Abstracta):** Define el contrato gráfico (Buffers, Shaders, Texturas, Dispatch, Barreras de Memoria).
2. **`DarkOpenGLBackend` (Implementación):** Implementa `DarkRHI` usando `DarkOpenGLLinker`.
3. **`DarkRHIContext` (Gestor de Estado):** Singleton global para inyectar y proveer la instancia activa (`DarkRHI`).

## Sistemas RHI-ificados
- **`GPUParticleSystem` y `SkeletalAnimationSystem`:** Ahora suben matrices y despachan Compute Shaders a través de la interfaz genérica `DarkRHI`.
- **`DarkDeferredLightingSystem` y `DarkFSRSystem`:** Ahora ejecutan barreras de memoria e inyectan Uniforms usando RHI.
- **`DarkDeferredPipeline`:** El G-Buffer completo (FBO y Texturas) se crea a través de `DarkRHI`.

## Exclusión Deliberada: ImGui
Se decidió mantener `DarkImGuiRenderer` directamente acoplado a OpenGL/Vulkan mediante rasterización directa.
**Justificación:** ImGui es un módulo auxiliar de diagnóstico (UI) que requiere docenas de llamadas legacy (Vertex Arrays, Indices, Scissor Test, Blending). Ensamblar toda la interfaz Compute-centric de `DarkRHI` para soportar llamadas arcaicas de UI ensuciaría el código Core.

## Siguiente Paso (Fase 29)
Crear `DarkVulkanBackend.java` implementando `DarkRHI` usando las bindings nativas de Vulkan a través de Project Panama, logrando un Multi-Backend instantáneo.
