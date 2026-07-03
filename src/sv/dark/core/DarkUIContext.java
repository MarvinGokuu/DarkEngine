package sv.dark.core;

import java.lang.foreign.MemorySegment;

/**
 * Agnostic interface for User Interface (UI) Context.
 */
public abstract class DarkUIContext {
    
    private static DarkUIContext instance;

    public static void set(DarkUIContext backend) {
        instance = backend;
    }

    public static DarkUIContext get() {
        if (instance == null) {
            throw new IllegalStateException("UI Context not initialized.");
        }
        return instance;
    }

    public abstract void init();
    public abstract void newFrame(MemorySegment windowPtr);
    public abstract void render();
    public abstract int getAFKTier();
    public abstract void cleanup();
}
