package sv.dark.rhi;

import java.lang.foreign.MemorySegment;

/**
 * Render Hardware Interface (RHI).
 * Abstraction layer to keep the Engine agnostic from OpenGL/Vulkan.
 */
public interface DarkRHI {
    
    // Constants for RHI
    int BUFFER_TARGET_SSBO = 0;
    int BUFFER_TARGET_UBO = 1;
    int BUFFER_TARGET_ARRAY = 2;
    int BUFFER_TARGET_ELEMENT = 3;

    int BARRIER_SHADER_STORAGE = 1;
    int BARRIER_SHADER_IMAGE = 2;
    
    int MAP_READ_BIT = 1;
    int MAP_WRITE_BIT = 2;
    int MAP_PERSISTENT_BIT = 4;
    int MAP_COHERENT_BIT = 8;
    
    int ACCESS_READ_ONLY = 0;
    int ACCESS_WRITE_ONLY = 1;
    int ACCESS_READ_WRITE = 2;

    int FORMAT_RGBA16F = 0;
    int FORMAT_RGBA8 = 1;
    
    DarkRHIDevice getDevice();
    DarkRHICommandList getCommandList();
}
