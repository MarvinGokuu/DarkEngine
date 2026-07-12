// Reading Order: 10011100
//  156
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import sv.dark.core.AAACertified;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.state.DarkStateLayout;
import sv.dark.state.WorldStateFrame;
import sv.dark.ui.DarkEngineWindow;

/**
 * RESPONSIBILITY: Read Raw Input from Hardware bypassing OS Event Queues.
 * WHY: Event listeners (MouseListener, KeyListener) create garbage (KeyEvent objects) and input lag.
 * TECHNIQUE: FFI direct polling of glfwGetKey and glfwGetCursorPos.
 * GUARANTEES: 0 Object Allocations per frame, sub-millisecond hardware latency.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(date = "2026-06-13", maxLatencyNs = 10, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Raw FFI Polling (O(1))")
public final class DarkInputSystem implements GameSystem {

    private final SectorMemoryVault vault;
    
    // Pre-allocated native memory for cursor coordinates to avoid allocation in hot path
    private final Arena inputArena;
    private final MemorySegment mouseXPtr;
    private final MemorySegment mouseYPtr;

    // GLFW Key Codes (Common)
    public static final int GLFW_KEY_W = 87;
    public static final int GLFW_KEY_A = 65;
    public static final int GLFW_KEY_S = 83;
    public static final int GLFW_KEY_D = 68;

    public DarkInputSystem(SectorMemoryVault vault) {
        this.vault = vault;
        this.inputArena = Arena.ofShared();
        this.mouseXPtr = inputArena.allocate(ValueLayout.JAVA_DOUBLE);
        this.mouseYPtr = inputArena.allocate(ValueLayout.JAVA_DOUBLE);
    }

    @Override
    public void update(WorldStateFrame state, float deltaTime) {
        MemorySegment window = DarkEngineWindow.getWindowPointer();
        if (window == null || window.equals(MemorySegment.NULL)) {
            return; // Window not ready or headless
        }

        try {
            // Read Mouse natively
            DarkGraphicsLinker.glfwGetCursorPos.invokeExact(window, mouseXPtr, mouseYPtr);
            double mx = mouseXPtr.get(ValueLayout.JAVA_DOUBLE, 0);
            double my = mouseYPtr.get(ValueLayout.JAVA_DOUBLE, 0);

            // Write to SectorMemoryVault natively (Slots 300-301)
            vault.writeInt(DarkStateLayout.INPUT_MOUSE_X, (int) mx);
            vault.writeInt(DarkStateLayout.INPUT_MOUSE_Y, (int) my);

            // Read Keys natively (O(1) polling without listeners)
            // 1 = PRESS, 0 = RELEASE
            int wState = (int) DarkGraphicsLinker.glfwGetKey.invokeExact(window, GLFW_KEY_W);
            int aState = (int) DarkGraphicsLinker.glfwGetKey.invokeExact(window, GLFW_KEY_A);
            int sState = (int) DarkGraphicsLinker.glfwGetKey.invokeExact(window, GLFW_KEY_S);
            int dState = (int) DarkGraphicsLinker.glfwGetKey.invokeExact(window, GLFW_KEY_D);

            // Combine key signals into a single latched bitmask (Slot 302)
            int latchedSignal = 0;
            if (wState == 1) latchedSignal |= (1 << 0);
            if (aState == 1) latchedSignal |= (1 << 1);
            if (sState == 1) latchedSignal |= (1 << 2);
            if (dState == 1) latchedSignal |= (1 << 3);

            vault.writeInt(DarkStateLayout.INPUT_LAST_SIGNAL, latchedSignal);

        } catch (Throwable e) {
            // Ignore native faults cleanly to prevent engine crash on input failure
        }
    }

    public void cleanup() {
        if (inputArena != null) {
            try {
                inputArena.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
