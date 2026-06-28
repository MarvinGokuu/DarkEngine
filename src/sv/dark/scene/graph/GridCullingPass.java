// Reading Order: 00100062
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene.graph;

import sv.dark.scene.DarkClusteredSystem;
import sv.dark.scene.DarkLightSystem;

public class GridCullingPass implements DarkRenderPass {
    @Override
    public String getName() {
        return "Clustered Grid & Culling Pass";
    }

    @Override
    public void execute(float[] viewMatrix, float[] projMatrix) {
        DarkClusteredSystem.dispatchGrid(projMatrix, viewMatrix);
        DarkLightSystem.syncToGPU();
        DarkClusteredSystem.dispatchCulling(viewMatrix);
    }
}
