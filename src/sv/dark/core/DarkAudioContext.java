package sv.dark.core;

/**
 * Agnostic interface for Audio Context.
 */
public abstract class DarkAudioContext {
    
    private static DarkAudioContext instance;

    public static void set(DarkAudioContext backend) {
        instance = backend;
    }

    public static DarkAudioContext get() {
        if (instance == null) {
            throw new IllegalStateException("Audio Context not initialized.");
        }
        return instance;
    }

    public abstract void init(sv.dark.audio.DarkAudioSourceSoA sources);
    public abstract void updateListener(float px, float py, float pz);
    public abstract void updateSources(sv.dark.audio.DarkAudioSourceSoA sources);
    public abstract void cleanup();
}
