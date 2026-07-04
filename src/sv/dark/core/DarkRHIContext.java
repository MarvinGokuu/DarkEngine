package sv.dark.core;

import sv.dark.rhi.DarkRHI;
import sv.dark.rhi.DarkOpenGLBackend;

/**
 * Context manager for the active RHI backend.
 */
public final class DarkRHIContext {
    private static sv.dark.rhi.DarkOpenGLBackend activeBackend;

    public static void init() {
        // Here we could read from dark-production.properties to choose the backend.
        // For now, we inject the OpenGL backend.
        activeBackend = new sv.dark.rhi.DarkOpenGLBackend();
        activeBackend.init();
    }

    public static void destroy() {
        if (activeBackend != null) {
            activeBackend.destroy();
            activeBackend = null;
        }
    }

    public static DarkRHI get() {
        return activeBackend;
    }
}
