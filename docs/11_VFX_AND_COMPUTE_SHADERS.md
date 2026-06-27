# 11. VFX y Compute Shaders (Phase 32)

Dark Engine maneja los efectos visuales y la animación de mallas complejas evadiendo por completo los cuellos de botella del procesador central (CPU). Toda la matemática pesada se realiza mediante **Compute Shaders (OpenGL 4.3+)**.

## Sistema de Partículas por GPU (1M+ Entidades)
El paradigma tradicional de videojuegos orientados a objetos calcula la física de cada partícula en la CPU y luego las sube a la tarjeta gráfica. Esto limita la cantidad máxima a unas pocas miles de partículas.

En Dark Engine:
1. **Instanciación Zero-VRAM**: Solo se sube a la memoria de video (VRAM) un buffer de datos primitivos conteniendo Velocidad, Posición, Tiempo de Vida y Color (`DarkGPUParticleSoA`).
2. **Compute Shader (`particles.comp`)**: La GPU ejecuta un hilo de procesamiento en paralelo por cada partícula. Calcula la gravedad, las colisiones elásticas y la disipación de color internamente a más de 1000 FPS.
3. **Renderizado Directo**: Sin que los datos jamás regresen a la RAM principal de Java, el motor invoca `glDrawArraysInstanced` para pintar 1 millón de partículas en pantalla usando la información que ya está en la VRAM.

## Animación Esquelética (Skinning) Zero-Copy
Renderizar un ejército de 10,000 entidades con esqueletos animados (Skeletal Meshes) es una de las tareas más devastadoras para un motor de juego tradicional, ya que requiere multiplicar cada vértice del modelo 3D por las matrices de rotación de cada hueso.

En Dark Engine:
1. **Estructura Estática**: `DarkSkeletonSoA` preasigna un bloque nativo contiguo con capacidad para 10,000 entidades x 64 matrices de transformación por esqueleto. (Aprox. 40MB totales, estáticos, sin recolección de basura).
2. **Transferencia Rápida**: El buffer se sube crudo a la tarjeta gráfica mediante `glBufferSubData`.
3. **GPU Skinning**: El Compute Shader de animación (`skinning.comp`) lee estas matrices y deforma la malla de polígonos asíncronamente justo antes de dibujarla, ahorrándole a la CPU millones de multiplicaciones matriciales por milisegundo.

> **Contrato de Integridad (VRAM Leak Prevention):** Todos los sistemas que invocan Compute Shaders implementan el método `destroy()` para ejecutar `glDeleteProgram` y `glDeleteBuffers` durante el apagado del Kernel. Omitir esto condenaría a la tarjeta gráfica a mantener programas huérfanos, drenando la memoria de video hasta forzar un crash (OOM) en el driver.
