package sv.dark.platform;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.dark.core.DarkLogger;
import sv.dark.core.DarkPlatformContext;
import sv.dark.core.systems.DarkGraphicsLinker;

public final class DarkGLFWBackend extends DarkPlatformContext {

    private MemorySegment windowPointer;
    private MemorySegment dropCallbackStub;
    
    private final Arena inputArena = Arena.ofShared();
    private final MemorySegment shadowBuffer = inputArena.allocate(16);
    private final MemorySegment xPtr = inputArena.allocate(ValueLayout.JAVA_DOUBLE);
    private final MemorySegment yPtr = inputArena.allocate(ValueLayout.JAVA_DOUBLE);

    @Override
    public void initWindow(String title, int width, int height) {
        try {
            DarkLogger.info("GRAPHICS", "Initializing Native Graphics Pipeline (GLFW)...");
            int initResult = (int) DarkGraphicsLinker.glfwInit.invokeExact();
            if (initResult == 0) {
                DarkLogger.fatal("GRAPHICS", "Failed to init GLFW native library.", null);
                return;
            }

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment titleSeg = arena.allocateFrom(title);
                windowPointer = (MemorySegment) DarkGraphicsLinker.glfwCreateWindow.invokeExact(
                    width, height, titleSeg, MemorySegment.NULL, MemorySegment.NULL
                );
            }

            if (windowPointer.equals(MemorySegment.NULL)) {
                DarkLogger.fatal("GRAPHICS", "Failed to create Native Window via GLFW.", null);
                return;
            }

            initDropCallback();

            DarkLogger.info("GRAPHICS", "Native Window successfully created (0ms Input Lag).");
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Native FFI Exception in Window Init", e);
            System.exit(1);
        }
    }

    private void initDropCallback() {
        try {
            java.lang.invoke.MethodHandle handle = java.lang.invoke.MethodHandles.lookup().findStatic(
                DarkGLFWBackend.class, 
                "onDrop", 
                java.lang.invoke.MethodType.methodType(void.class, MemorySegment.class, int.class, MemorySegment.class)
            );
            
            dropCallbackStub = java.lang.foreign.Linker.nativeLinker().upcallStub(
                handle, 
                java.lang.foreign.FunctionDescriptor.ofVoid(java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.JAVA_INT, java.lang.foreign.ValueLayout.ADDRESS),
                Arena.global()
            );
            
            DarkGraphicsLinker.glfwSetDropCallback.invokeExact(windowPointer, dropCallbackStub);
            DarkLogger.info("UI", "Drag & Drop Asset Compiler link enabled.");
        } catch (Throwable t) {
            DarkLogger.error("UI", "Failed to init drop callback.");
        }
    }

    public static void onDrop(MemorySegment window, int count, MemorySegment pathsArray) {
        MemorySegment safeArray = pathsArray.reinterpret(count * java.lang.foreign.ValueLayout.ADDRESS.byteSize());
        for (int i = 0; i < count; i++) {
            MemorySegment pathPtr = safeArray.getAtIndex(java.lang.foreign.ValueLayout.ADDRESS, i);
            String path = pathPtr.reinterpret(Long.MAX_VALUE).getString(0);
            sv.dark.editor.DarkAssetCompiler.compileAsync(path);
        }
    }

    @Override
    public void pollEvents(MemorySegment vaultSegment) {
        if (windowPointer == null || windowPointer.equals(MemorySegment.NULL)) return;
        try {
            DarkGraphicsLinker.glfwPollEvents.invokeExact();

            DarkGraphicsLinker.glfwGetCursorPos.invokeExact(windowPointer, xPtr, yPtr);
            int mx = (int) xPtr.get(ValueLayout.JAVA_DOUBLE, 0);
            int my = (int) yPtr.get(ValueLayout.JAVA_DOUBLE, 0);
            
            shadowBuffer.set(ValueLayout.JAVA_INT, 0, mx);
            shadowBuffer.set(ValueLayout.JAVA_INT, 4, my);

            if (vaultSegment != null) {
                MemorySegment.copy(shadowBuffer, 0, vaultSegment, 1200, 8);
            }
        } catch (Throwable e) {
            // Ignore
        }
    }

    @Override
    public boolean shouldClose() {
        if (windowPointer == null || windowPointer.equals(MemorySegment.NULL)) return false;
        try {
            int close = (int) DarkGraphicsLinker.glfwWindowShouldClose.invokeExact(windowPointer);
            return close != 0;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public MemorySegment getWindowPointer() {
        return windowPointer;
    }

    @Override
    public void makeContextCurrent() {
        if (windowPointer != null && !windowPointer.equals(MemorySegment.NULL)) {
            try {
                DarkGraphicsLinker.glfwMakeContextCurrent.invokeExact(windowPointer);
            } catch (Throwable e) {
                DarkLogger.warning("GRAPHICS", "Failed to make GLFW context current.");
            }
        }
    }

    @Override
    public void setSwapInterval(int interval) {
        try {
            DarkGraphicsLinker.glfwSwapInterval.invokeExact(interval);
        } catch (Throwable e) {
            DarkLogger.warning("GRAPHICS", "Failed to set swap interval.");
        }
    }

    @Override
    public void cleanup() {
        if (windowPointer != null && !windowPointer.equals(MemorySegment.NULL)) {
            try {
                DarkGraphicsLinker.glfwDestroyWindow.invokeExact(windowPointer);
                DarkGraphicsLinker.glfwTerminate.invokeExact();
            } catch (Throwable e) {
                DarkLogger.warning("GRAPHICS", "Failed to cleanup GLFW natively.");
            }
        }
    }
}
