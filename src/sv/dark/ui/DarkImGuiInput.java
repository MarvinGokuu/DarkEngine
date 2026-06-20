package sv.dark.ui;

import imgui.ImGui;
import imgui.ImGuiIO;
import sv.dark.core.systems.DarkGraphicsLinker;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * RESPONSIBILITY: Native Input Handler for Dear ImGui.
 * WHY: We refuse to use LWJGL, so we must manually route our Panama GLFW inputs into ImGui.
 */
public final class DarkImGuiInput {

    private static double time = 0.0;
    
    public static void init() {
        ImGuiIO io = ImGui.getIO();
        io.setBackendPlatformName("imgui_java_impl_darkengine");
        io.setConfigWindowsMoveFromTitleBarOnly(true);
    }

    public static void newFrame(MemorySegment windowPtr) {
        ImGuiIO io = ImGui.getIO();
        
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment wPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment hPtr = arena.allocate(ValueLayout.JAVA_INT);
            
            DarkGraphicsLinker.glfwGetWindowSize.invokeExact(windowPtr, wPtr, hPtr);
            int w = wPtr.get(ValueLayout.JAVA_INT, 0);
            int h = hPtr.get(ValueLayout.JAVA_INT, 0);
            
            io.setDisplaySize((float) w, (float) h);
            
            // Mouse Pos
            MemorySegment xPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
            MemorySegment yPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
            DarkGraphicsLinker.glfwGetCursorPos.invokeExact(windowPtr, xPtr, yPtr);
            io.setMousePos((float) xPtr.get(ValueLayout.JAVA_DOUBLE, 0), (float) yPtr.get(ValueLayout.JAVA_DOUBLE, 0));
            
            // Mouse Buttons
            boolean m0 = (int) DarkGraphicsLinker.glfwGetMouseButton.invokeExact(windowPtr, 0) != 0;
            boolean m1 = (int) DarkGraphicsLinker.glfwGetMouseButton.invokeExact(windowPtr, 1) != 0;
            boolean m2 = (int) DarkGraphicsLinker.glfwGetMouseButton.invokeExact(windowPtr, 2) != 0;
            io.setMouseDown(0, m0);
            io.setMouseDown(1, m1);
            io.setMouseDown(2, m2);
            
            // Time Delta
            io.setDeltaTime(1.0f / 60.0f); // Fixed 60fps delta for now
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
