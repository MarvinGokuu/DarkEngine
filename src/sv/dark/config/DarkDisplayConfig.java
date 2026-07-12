// Reading Order: 00100010
//  34
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.config;

import sv.dark.core.AAACertified;

/**
 * Global Display Configuration.
 * Maintains runtime resolution states and allows dynamic FSR resizing
 * without destroying the OpenGL/Vulkan context.
 */
@AAACertified(date = "2026-07-02", maxLatencyNs = 0, minThroughput = 0, lockFree = true, offHeap = false, notes = "Dynamic Display State")
public final class DarkDisplayConfig {

    public static int targetWidth = DarkEngineConfig.GRAPHICS_TARGET_WIDTH;
    public static int targetHeight = DarkEngineConfig.GRAPHICS_TARGET_HEIGHT;

    public static void setTargetResolution(int width, int height) {
        targetWidth = width;
        targetHeight = height;
    }
}
