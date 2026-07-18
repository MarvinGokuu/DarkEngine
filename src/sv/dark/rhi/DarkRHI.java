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
    int BUFFER_TARGET_UPLOAD = 4; // Para Streaming Asincrono (PBO)

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
    int FORMAT_RGBA = 2;
    int FORMAT_DEPTH_COMPONENT32F = 3;
    int FORMAT_DEPTH_COMPONENT = 4;
    
    int TYPE_UNSIGNED_BYTE = 0;
    int TYPE_FLOAT = 1;
    int TYPE_UNSIGNED_INT = 2;

    int FILTER_NEAREST = 0;
    int FILTER_LINEAR = 1;

    int ATTACHMENT_COLOR0 = 0;
    int ATTACHMENT_COLOR1 = 1;
    int ATTACHMENT_COLOR2 = 2;
    int ATTACHMENT_DEPTH = 3;

    int PRIMITIVE_TRIANGLES = 0;
    
    int CLEAR_DEPTH_BUFFER_BIT = 1;
    int CLEAR_COLOR_BUFFER_BIT = 2;
    
    DarkRHIDevice getDevice();
    DarkRHICommandList getCommandList();
    DarkRHIRendererUI getUIRenderer();
}
