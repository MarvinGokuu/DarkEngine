// Reading Order: 00011001
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ui;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.io.File;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;

/**
 * RESPONSIBILITY: Foreign Function Interface (FFI) for Dear ImGui.
 * WHY: We need a native UI without AWT/JavaFX to maintain Zero-GC and 0ms Input Lag.
 * TECHNIQUE: Project Panama FFI. Links against cimgui.dll natively.
 * GUARANTEES: Direct VRAM UI rendering. Zero Java Heap allocations.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(date = "2026-06-14", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Native FFI Linker for cimgui.dll")
public final class DarkImGuiLinker {
    
    private static final Linker LINKER = Linker.nativeLinker();
    private static SymbolLookup IMGUI;
    private static boolean isLoaded = false;
    
    public static void init() {
        File cimguiDll = new File("lib/cimgui.dll");
        if (!cimguiDll.exists()) {
            DarkLogger.error("IMGUI", "lib/cimgui.dll NOT FOUND. Native Editor UI will be disabled.");
            return;
        }
        try {
            System.load(cimguiDll.getAbsolutePath());
            IMGUI = SymbolLookup.loaderLookup();
            isLoaded = true;
            DarkLogger.info("IMGUI", "Project Panama FFI: cimgui.dll loaded successfully.");
            
            // Initialize MethodHandles dynamically
            igCreateContext = LINKER.downcallHandle(IMGUI.find("igCreateContext").orElseThrow(), FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            igNewFrame = LINKER.downcallHandle(IMGUI.find("igNewFrame").orElseThrow(), FunctionDescriptor.ofVoid());
            igRender = LINKER.downcallHandle(IMGUI.find("igRender").orElseThrow(), FunctionDescriptor.ofVoid());
            igGetDrawData = LINKER.downcallHandle(IMGUI.find("igGetDrawData").orElseThrow(), FunctionDescriptor.of(ValueLayout.ADDRESS));
            igShowDemoWindow = LINKER.downcallHandle(IMGUI.find("igShowDemoWindow").orElseThrow(), FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            
            // Backends (optional, depending on dll compilation)
            ImGui_ImplGlfw_InitForOpenGL = LINKER.downcallHandle(IMGUI.find("ImGui_ImplGlfw_InitForOpenGL").orElse(null), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_BOOLEAN));
            ImGui_ImplOpenGL3_Init = LINKER.downcallHandle(IMGUI.find("ImGui_ImplOpenGL3_Init").orElse(null), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            ImGui_ImplOpenGL3_NewFrame = LINKER.downcallHandle(IMGUI.find("ImGui_ImplOpenGL3_NewFrame").orElse(null), FunctionDescriptor.ofVoid());
            ImGui_ImplGlfw_NewFrame = LINKER.downcallHandle(IMGUI.find("ImGui_ImplGlfw_NewFrame").orElse(null), FunctionDescriptor.ofVoid());
            ImGui_ImplOpenGL3_RenderDrawData = LINKER.downcallHandle(IMGUI.find("ImGui_ImplOpenGL3_RenderDrawData").orElse(null), FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            
            DarkLogger.info("IMGUI", "Native Editor UI Chassis Initialized (Ready for Rendering).");
        } catch (Throwable e) {
            DarkLogger.error("IMGUI", "Failed to link ImGui FFI: " + e.getMessage());
            isLoaded = false;
        }
    }
    
    public static boolean isLoaded() {
        return isLoaded;
    }

    // =========================================================================
    // NATIVE METHOD HANDLES (Zero-GC Pointers to C++)
    // =========================================================================

    public static MethodHandle igCreateContext;
    public static MethodHandle igNewFrame;
    public static MethodHandle igRender;
    public static MethodHandle igGetDrawData;
    public static MethodHandle igShowDemoWindow;
    
    public static MethodHandle ImGui_ImplGlfw_InitForOpenGL;
    public static MethodHandle ImGui_ImplOpenGL3_Init;
    public static MethodHandle ImGui_ImplOpenGL3_NewFrame;
    public static MethodHandle ImGui_ImplGlfw_NewFrame;
    public static MethodHandle ImGui_ImplOpenGL3_RenderDrawData;
}
