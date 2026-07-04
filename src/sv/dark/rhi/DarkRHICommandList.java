package sv.dark.rhi;

import java.lang.foreign.MemorySegment;

public interface DarkRHICommandList {
    void begin();
    void bindPipeline(int pipelineId);
    
    void bindBuffer(int target, int index, int bufferId);
    void updateBufferData(int target, int bufferId, long offset, MemorySegment data, long size);
    
    void setUniform1f(int location, float v0);
    void setUniform2f(int location, float v0, float v1);
    void setUniform3f(int location, float v0, float v1, float v2);
    void setUniform4f(int location, float v0, float v1, float v2, float v3);
    void setUniform1i(int location, int v0);
    void setUniform1ui(int location, int v0);
    void setUniformMatrix4fv(int location, int count, boolean transpose, MemorySegment value);

    void bindTexture2D(int unit, int textureId);
    void bindTexture2DArray(int unit, int textureId);
    void bindImageTexture(int unit, int textureId, int level, boolean layered, int layer, int access, int format);

    void dispatchCompute(int groupsX, int groupsY, int groupsZ);
    void memoryBarrier(int flags);
    
    void bindFramebuffer(int fboId);
    void setViewport(int x, int y, int width, int height);
    void clear(int mask);
    void bindFramebufferTextureLayer(int fboId, int attachment, int textureId, int level, int layer);
    void drawElementsInstanced(int mode, int count, int type, MemorySegment indices, int instancecount);

    void end();
    void submit();
}
