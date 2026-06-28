// Reading Order: 00100063
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene.graph;

import sv.dark.scene.DarkShadowSystem;
import sv.dark.scene.DarkCameraState;

public class ShadowPass implements DarkRenderPass {
    
    // Pre-allocated scratch matrix to ensure zero GC
    private static final float[] RENDER_LIGHT_MATRIX = new float[16];
    private static final float[] RENDER_SUN_DIR = {0.5f, 1.0f, 0.5f};

    @Override
    public String getName() {
        return "Cascaded Shadow Mapping Pass";
    }

    @Override
    public void execute(float[] viewMatrix, float[] projMatrix) {
        float fovY = DarkCameraState.FOV_Y;
        float aspect = DarkCameraState.ASPECT;
        float zNear = DarkCameraState.Z_NEAR;
        float zFar = DarkCameraState.Z_FAR;

        // 1.7 Cascaded Shadow Mapping (CSM) — Zero-GC
        for (int i = 0; i < DarkShadowSystem.CASCADE_COUNT; i++) {
            DarkShadowSystem.calculateCascadeMatrix(i, viewMatrix, fovY, aspect, zNear, zFar, RENDER_SUN_DIR, RENDER_LIGHT_MATRIX);
            DarkShadowSystem.beginShadowPass(i, RENDER_LIGHT_MATRIX);
            // ECS Geometry would be rendered here for shadows
            DarkShadowSystem.endShadowPass();
        }
    }
}
