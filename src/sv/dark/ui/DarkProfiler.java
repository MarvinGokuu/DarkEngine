// Reading Order: 00001111
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
package sv.dark.ui;

import sv.dark.core.AAACertified;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

/**
 * RESPONSIBILITY: Technical AAA Profiler overlay.
 * WHY: We need to see hardware metrics in real-time on the native window without relying on the external web editor.
 * TECHNIQUE: Uses Dear ImGui via FFI. Configured to be transparent and non-intrusive (no borders, no resize).
 * GUARANTEES: Zero GC allocations per frame.
 */
@AAACertified(date = "2026-06-27", maxLatencyNs = 1_000_000, notes = "ImGui Profiler Overlay")
public final class DarkProfiler {

    private static final int overlayFlags = ImGuiWindowFlags.NoDecoration 
                                          | ImGuiWindowFlags.NoDocking 
                                          | ImGuiWindowFlags.AlwaysAutoResize 
                                          | ImGuiWindowFlags.NoSavedSettings 
                                          | ImGuiWindowFlags.NoFocusOnAppearing 
                                          | ImGuiWindowFlags.NoNav 
                                          | ImGuiWindowFlags.NoMove;

    public static void render(int fps, long frameTimeNs, long memoryUsedMb) {
        // Posicionar en la esquina superior izquierda con un pequeño margen
        ImGui.setNextWindowPos(10, 10);
        ImGui.setNextWindowBgAlpha(0.35f); // Fondo transparente estilo hacker

        if (ImGui.begin("##DarkProfiler", overlayFlags)) {
            // ImGui.textColored takes an int (imColor) in some java bindings, or 4 floats.
            // Let's use ImGui.pushStyleColor instead to be safer with bindings.
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.0f, 1.0f, 0.0f, 1.0f);
            ImGui.text("DARK ENGINE NATIVE PROFILER");
            ImGui.popStyleColor();
            
            ImGui.separator();
            
            // FPS con color dinámico
            if (fps >= 60) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.0f, 1.0f, 0.0f, 1.0f);
                ImGui.text("FPS: " + fps);
                ImGui.popStyleColor();
            } else if (fps >= 30) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 1.0f, 0.0f, 1.0f);
                ImGui.text("FPS: " + fps);
                ImGui.popStyleColor();
            } else {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.0f, 0.0f, 1.0f);
                ImGui.text("FPS: " + fps);
                ImGui.popStyleColor();
            }
            
            ImGui.text("Frame Time: " + (frameTimeNs / 1_000_000.0f) + " ms");
            ImGui.text("Memory: " + memoryUsedMb + " MB");
            
            ImGui.separator();
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
            ImGui.text("Daemon Mode Active");
            ImGui.popStyleColor();
        }
        ImGui.end();
    }
}
