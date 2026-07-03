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
    
    // Core Pipeline
    void init();
    void destroy();

    // Compute Shaders
    int createComputeShader(String source);
    int getUniformLocation(int programId, String name);
    void useProgram(int programId);
    void deleteProgram(int programId);

    // Buffers
    int createBuffer(long sizeBytes, int flags);
    MemorySegment mapBuffer(int target, int bufferId, long offset, long length, int flags);
    void unmapBuffer(int target, int bufferId);
    void bindBufferBase(int target, int index, int bufferId);
    void deleteBuffer(int bufferId);
    void updateBufferSubData(int target, int bufferId, long offset, MemorySegment data, long size);

    // Uniforms
    void setUniform1f(int location, float v0);
    void setUniform3f(int location, float v0, float v1, float v2);
    void setUniform1i(int location, int v0);
    void setUniformMatrix4fv(int location, int count, boolean transpose, MemorySegment value);

    // Textures & Images
    int createTexture2D(int width, int height, int internalFormat, int format, int type, int filter);
    int createTexture2DArray(int width, int height, int depth, int internalFormat, int format, int type, int filter);
    void activeTexture(int unit);
    void bindTexture2D(int textureId);
    void bindTexture2DArray(int textureId);
    void bindImageTexture(int unit, int textureId, int level, boolean layered, int layer, int access, int format);
    void resizeTexture2D(int textureId, int width, int height, int internalFormat, int format, int type);
    void deleteTextures(int[] textureIds);

    // Framebuffers
    int createFramebuffer();
    void bindFramebuffer(int fboId);
    void framebufferTexture2D(int attachment, int textureId, int level);
    boolean checkFramebufferStatus();
    void setDrawBuffers(int[] attachments);
    void deleteFramebuffer(int fboId);
    
    // Dispatch
    void dispatchCompute(int groupsX, int groupsY, int groupsZ);
    void memoryBarrier(int flags);
}
