package sv.dark.rhi;

import java.lang.foreign.MemorySegment;

public interface DarkRHIDevice {
    void init();
    void destroy();

    int createComputePipeline(String source);
    int createGraphicsPipeline(String vertSource, String fragSource);
    int getUniformLocation(int programId, String name);
    void deletePipeline(int pipelineId);

    int createBuffer(long sizeBytes, int flags);
    MemorySegment mapBuffer(int target, int bufferId, long offset, long length, int flags);
    void unmapBuffer(int target, int bufferId);
    void deleteBuffer(int bufferId);
    void deleteBuffers(int[] bufferIds);

    int createTexture2D(int width, int height, int internalFormat, int format, int type, int filter);
    int createTexture2DArray(int width, int height, int depth, int internalFormat, int format, int type, int filter);
    void resizeTexture2D(int textureId, int width, int height, int internalFormat, int format, int type);
    void deleteTextures(int[] textureIds);

    int createFramebuffer();
    void framebufferTexture2D(int fboId, int attachment, int textureId, int level);
    void framebufferTexture(int fboId, int attachment, int textureId, int level);
    boolean checkFramebufferStatus(int fboId);
    void setDrawBuffers(int fboId, int[] attachments);
    void setDrawBufferNone(int fboId);
    void deleteFramebuffer(int fboId);
    void deleteFramebuffers(int[] fboIds);
}
