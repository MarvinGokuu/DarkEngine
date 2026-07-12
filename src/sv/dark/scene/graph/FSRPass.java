// Reading Order: 10001101
//  141
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene.graph;

import sv.dark.scene.DarkFSRSystem;

public class FSRPass implements DarkRenderPass {
    @Override
    public String getName() {
        return "FSR Upscaling Pass";
    }

    @Override
    public void execute(float[] viewMatrix, float[] projMatrix) {
        DarkFSRSystem.dispatchFSR();
    }
}
