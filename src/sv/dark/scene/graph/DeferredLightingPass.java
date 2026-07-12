// Reading Order: 10101001
//  169
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene.graph;

import sv.dark.scene.DarkDeferredLightingSystem;

public class DeferredLightingPass implements DarkRenderPass {
    @Override
    public String getName() {
        return "Deferred Lighting Pass";
    }

    @Override
    public void execute(float[] viewMatrix, float[] projMatrix) {
        DarkDeferredLightingSystem.dispatchLighting();
    }
}
