package sv.dark.audio;

import java.lang.foreign.MemorySegment;
import sv.dark.core.DarkAudioContext;
import sv.dark.core.DarkLogger;
import sv.dark.core.systems.DarkAudioLinker;

public final class DarkOpenALBackend extends DarkAudioContext {

    private MemorySegment device;
    private MemorySegment context;

    @Override
    public void init(DarkAudioSourceSoA sources) {
        try {
            device = (MemorySegment) DarkAudioLinker.alcOpenDevice.invokeExact(MemorySegment.NULL);
            if (device.equals(MemorySegment.NULL)) {
                DarkLogger.warning("AUDIO", "Failed to open OpenAL device. Audio disabled.");
                return;
            }

            context = (MemorySegment) DarkAudioLinker.alcCreateContext.invokeExact(device, MemorySegment.NULL);
            if (context.equals(MemorySegment.NULL)) {
                DarkLogger.warning("AUDIO", "Failed to create OpenAL context.");
                return;
            }

            byte dummy1 = (byte) DarkAudioLinker.alcMakeContextCurrent.invokeExact(context);
            DarkAudioLinker.alGenSources.invokeExact(sources.getCapacity(), sources.sourceIds);
            
            DarkLogger.info("AUDIO", "OpenAL Context Initialized. Spatial Audio Active (1024 Sources).");
        } catch (Throwable t) {
            DarkLogger.warning("AUDIO", "OpenAL Initialization Exception: " + t.getMessage());
        }
    }

    @Override
    public void updateListener(float px, float py, float pz) {
        if (device == null || device.equals(MemorySegment.NULL)) return;
        try {
            DarkAudioLinker.alListener3f.invokeExact(DarkAudioLinker.AL_POSITION, px, py, pz);
        } catch (Throwable e) {
            // Ignore
        }
    }

    @Override
    public void updateSources(DarkAudioSourceSoA sources) {
        if (device == null || device.equals(MemorySegment.NULL)) return;
        try {
            int cap = sources.getCapacity();
            for (int i = 0; i < cap; i++) {
                int sourceId = sources.sourceIds.get(java.lang.foreign.ValueLayout.JAVA_INT, i * 4L);
                if (sourceId == 0) continue;
                
                float spx = sources.posX.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float spy = sources.posY.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float spz = sources.posZ.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                
                float svx = sources.velX.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float svy = sources.velY.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float svz = sources.velZ.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                
                DarkAudioLinker.alSource3f.invokeExact(sourceId, DarkAudioLinker.AL_POSITION, spx, spy, spz);
                DarkAudioLinker.alSource3f.invokeExact(sourceId, DarkAudioLinker.AL_VELOCITY, svx, svy, svz);
            }
        } catch (Throwable e) {
            // Ignore
        }
    }

    @Override
    public void cleanup() {
        try {
            if (context != null && !context.equals(MemorySegment.NULL)) {
                byte dummy2 = (byte) DarkAudioLinker.alcMakeContextCurrent.invokeExact(MemorySegment.NULL);
                DarkAudioLinker.alcDestroyContext.invokeExact(context);
            }
            if (device != null && !device.equals(MemorySegment.NULL)) {
                byte dummy3 = (byte) DarkAudioLinker.alcCloseDevice.invokeExact(device);
            }
        } catch (Throwable e) {
            DarkLogger.warning("AUDIO", "Failed to cleanup OpenAL natively.");
        }
    }
}
