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
    
    // Anti-GC: Pre-allocated memory for FFI calls
    private static final Arena inputArena = Arena.ofAuto();
    private static final MemorySegment wPtr = inputArena.allocate(ValueLayout.JAVA_INT);
    private static final MemorySegment hPtr = inputArena.allocate(ValueLayout.JAVA_INT);
    private static final MemorySegment xPtr = inputArena.allocate(ValueLayout.JAVA_DOUBLE);
    private static final MemorySegment yPtr = inputArena.allocate(ValueLayout.JAVA_DOUBLE);

    // AFK State Tracking
    private static long lastInputTimeNs = System.nanoTime();
    private static final long AFK_TIMEOUT_NS = 30L * 60L * 1_000_000_000L; // 30 minutes to drop to Tier 3, but let's use 60 seconds for 30fps drop? 
    // Wait, user said "30 minutes to return, 2 hours to return... you don't need to leave it at 1 minute". 
    // Let's set Tier 2 (30fps) after 5 minutes, and Tier 3 (deep sleep) after 30 mins.
    private static final long TIER2_TIMEOUT_NS = 5L * 60L * 1_000_000_000L; // 5 mins
    private static final long TIER3_TIMEOUT_NS = 30L * 60L * 1_000_000_000L; // 30 mins
    
    private static double lastMouseX = -1;
    private static double lastMouseY = -1;
    private static boolean lastM0, lastM1, lastM2;
    private static boolean isMinimized = false;

    public static void init() {
        ImGuiIO io = ImGui.getIO();
        io.setBackendPlatformName("imgui_java_impl_darkengine");
        io.setConfigWindowsMoveFromTitleBarOnly(true);
    }

    public static void newFrame(MemorySegment windowPtr) {
        ImGuiIO io = ImGui.getIO();
        
        try {
            DarkGraphicsLinker.glfwGetWindowSize.invokeExact(windowPtr, wPtr, hPtr);
            int w = wPtr.get(ValueLayout.JAVA_INT, 0);
            int h = hPtr.get(ValueLayout.JAVA_INT, 0);
            
            isMinimized = (w == 0 || h == 0);
            io.setDisplaySize((float) w, (float) h);
            
            DarkGraphicsLinker.glfwGetCursorPos.invokeExact(windowPtr, xPtr, yPtr);
            double mx = xPtr.get(ValueLayout.JAVA_DOUBLE, 0);
            double my = yPtr.get(ValueLayout.JAVA_DOUBLE, 0);
            io.setMousePos((float) mx, (float) my);
            
            boolean m0 = (int) DarkGraphicsLinker.glfwGetMouseButton.invokeExact(windowPtr, 0) != 0;
            boolean m1 = (int) DarkGraphicsLinker.glfwGetMouseButton.invokeExact(windowPtr, 1) != 0;
            boolean m2 = (int) DarkGraphicsLinker.glfwGetMouseButton.invokeExact(windowPtr, 2) != 0;
            io.setMouseDown(0, m0);
            io.setMouseDown(1, m1);
            io.setMouseDown(2, m2);
            
            // Check for input changes to update AFK timer
            if (mx != lastMouseX || my != lastMouseY || m0 != lastM0 || m1 != lastM1 || m2 != lastM2) {
                lastInputTimeNs = System.nanoTime();
                lastMouseX = mx;
                lastMouseY = my;
                lastM0 = m0;
                lastM1 = m1;
                lastM2 = m2;
            }
            
            io.setDeltaTime(1.0f / 60.0f); // Handled dynamically in TimeKeeper? ImGui needs it.
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static int getAFKTier() {
        if (isMinimized) return 3; // Deep sleep
        long idleTime = System.nanoTime() - lastInputTimeNs;
        if (idleTime > TIER3_TIMEOUT_NS) return 3;
        if (idleTime > TIER2_TIMEOUT_NS) return 2;
        return 1; // Active
    }
}
