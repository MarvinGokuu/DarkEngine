// Reading Order: 10100101
//  165
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.DarkLogger;
import sv.dark.scene.DarkTransformSoA;
import sv.dark.scene.DarkComputeCullingSystem;
import sv.dark.ui.DarkEngineWindow;

public class SystemGPUCullingTest {
    
    private static final int ENTITY_COUNT = 1_000_000;

    public static void main(String[] args) {
        DarkLogger.info("TEST", "[17/17] Running GPU Compute Culling (Frustum vs 1,000,000 AABBs)...");

        // 1. Iniciar la capa de la ventana nativa (Esto habilita OpenGL 4.3)
        DarkEngineWindow.initNativeWindow();

        // 2. Alojamiento Off-Heap
        DarkTransformSoA soa = new DarkTransformSoA(ENTITY_COUNT);

        // 3. Llenado con datos dispersos para evitar optimizaciones vacías de la GPU
        for (int i = 0; i < ENTITY_COUNT; i++) {
            soa.posX.set(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L, i * 0.1f);
            soa.posY.set(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L, i * 0.1f);
        }

        // 4. Compilar el Compute Shader
        DarkComputeCullingSystem.init();

        // WARMUP JIT y Pipeline Gráfico
        for(int i=0; i<5; i++) {
            DarkComputeCullingSystem.dispatchCulling(soa);
        }

        // 5. Benchmark de latencia de VRAM (Transferencia + Compute)
        long start = System.nanoTime();
        DarkComputeCullingSystem.dispatchCulling(soa);
        long end = System.nanoTime();

        double latencyMs = (end - start) / 1_000_000.0;
        
        DarkLogger.info("TEST", "[17/17] VRAM Compute Dispatch Completado: " + latencyMs + " ms");

        // El umbral se establece a 5.0ms para absorber el ancho de banda del bus PCIe de transferencia inicial
        if (latencyMs > 5.0) {
            DarkLogger.fatal("TEST", "[17/17] FAILED: GPU Culling Latency too high! Expected < 5.0ms, got: " + latencyMs + "ms", new RuntimeException("Latency Exceeded"));
            System.exit(1);
        }

        DarkLogger.info("TEST", "[17/17] AAA+ GPU CULLING PASSED.");
        soa.destroy();
    }
}
