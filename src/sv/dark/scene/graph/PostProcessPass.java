// Reading Order: 00100065
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene.graph;

import sv.dark.scene.DarkPostProcessSystem;

public class PostProcessPass implements DarkRenderPass {
    @Override
    public String getName() {
        return "Post-Processing Pass";
    }

    @Override
    public void execute(float[] viewMatrix, float[] projMatrix) {
        DarkPostProcessSystem.dispatchPostProcess();
    }
}
