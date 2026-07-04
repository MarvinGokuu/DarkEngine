package sv.dark.rhi;

import sv.dark.core.systems.DarkOpenGLLinker;
import sv.dark.core.DarkLogger;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class DarkOpenGLBackend implements DarkRHI, DarkRHIDevice, DarkRHICommandList {

    @Override
    public DarkRHIDevice getDevice() {
        return this;
    }

    @Override
    public DarkRHICommandList getCommandList() {
        return this;
    }

    // --- DarkRHIDevice Implementation ---
    @Override
    public void init() {
        DarkLogger.info("RHI", "Inicializando DarkOpenGLBackend...");
    }

    @Override
    public void destroy() {
        DarkLogger.info("RHI", "Destruyendo DarkOpenGLBackend...");
    }

    @Override
    public int createComputePipeline(String source) {
        try {
            int shaderId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_COMPUTE_SHADER);
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment srcPtr = arena.allocateFrom(source);
                MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
                DarkOpenGLLinker.glShaderSource.invokeExact(shaderId, 1, srcArrayPtr, MemorySegment.NULL);
            }
            DarkOpenGLLinker.glCompileShader.invokeExact(shaderId);
            
            int programId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
            DarkOpenGLLinker.glAttachShader.invokeExact(programId, shaderId);
            DarkOpenGLLinker.glLinkProgram.invokeExact(programId);
            DarkOpenGLLinker.glDeleteShader.invokeExact(shaderId);
            return programId;
        } catch (Throwable e) {
            DarkLogger.fatal("RHI", "Error creando Compute Pipeline", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getUniformLocation(int programId, String name) {
        try (Arena arena = Arena.ofConfined()) {
            return (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(programId, arena.allocateFrom(name));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int createGraphicsPipeline(String vertSource, String fragSource) {
        try {
            int programId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
            int vertId = 0, fragId = 0;
            
            try (Arena arena = Arena.ofConfined()) {
                if (vertSource != null) {
                    vertId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_VERTEX_SHADER);
                    MemorySegment srcPtr = arena.allocateFrom(vertSource);
                    MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
                    DarkOpenGLLinker.glShaderSource.invokeExact(vertId, 1, srcArrayPtr, MemorySegment.NULL);
                    DarkOpenGLLinker.glCompileShader.invokeExact(vertId);
                    DarkOpenGLLinker.glAttachShader.invokeExact(programId, vertId);
                }
                if (fragSource != null) {
                    fragId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_FRAGMENT_SHADER);
                    MemorySegment srcPtr = arena.allocateFrom(fragSource);
                    MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
                    DarkOpenGLLinker.glShaderSource.invokeExact(fragId, 1, srcArrayPtr, MemorySegment.NULL);
                    DarkOpenGLLinker.glCompileShader.invokeExact(fragId);
                    DarkOpenGLLinker.glAttachShader.invokeExact(programId, fragId);
                }
            }
            
            DarkOpenGLLinker.glLinkProgram.invokeExact(programId);
            
            if (vertId != 0) DarkOpenGLLinker.glDeleteShader.invokeExact(vertId);
            if (fragId != 0) DarkOpenGLLinker.glDeleteShader.invokeExact(fragId);
            
            return programId;
        } catch (Throwable e) {
            DarkLogger.fatal("RHI", "Error creando Graphics Pipeline", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deletePipeline(int pipelineId) {
        try {
            DarkOpenGLLinker.glDeleteProgram.invokeExact(pipelineId);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private int mapTarget(int target) {
        return switch (target) {
            case BUFFER_TARGET_SSBO -> DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER;
            case BUFFER_TARGET_UBO -> 0x8A11;
            case BUFFER_TARGET_ARRAY -> DarkOpenGLLinker.GL_ARRAY_BUFFER;
            case BUFFER_TARGET_ELEMENT -> DarkOpenGLLinker.GL_ELEMENT_ARRAY_BUFFER;
            default -> 0;
        };
    }

    @Override
    public int createBuffer(long sizeBytes, int flags) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bufferPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenBuffers.invokeExact(1, bufferPtr);
            int bufferId = bufferPtr.get(ValueLayout.JAVA_INT, 0);
            
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_ARRAY_BUFFER, bufferId);
            int glFlags = 0;
            if ((flags & MAP_READ_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_READ_BIT;
            if ((flags & MAP_WRITE_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_WRITE_BIT;
            if ((flags & MAP_PERSISTENT_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT;
            if ((flags & MAP_COHERENT_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_COHERENT_BIT;
            
            DarkOpenGLLinker.glBufferStorage.invokeExact(DarkOpenGLLinker.GL_ARRAY_BUFFER, sizeBytes, MemorySegment.NULL, glFlags);
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_ARRAY_BUFFER, 0);
            
            return bufferId;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MemorySegment mapBuffer(int target, int bufferId, long offset, long length, int flags) {
        try {
            int glTarget = mapTarget(target);
            DarkOpenGLLinker.glBindBuffer.invokeExact(glTarget, bufferId);
            
            int glFlags = 0;
            if ((flags & MAP_READ_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_READ_BIT;
            if ((flags & MAP_WRITE_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_WRITE_BIT;
            if ((flags & MAP_PERSISTENT_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT;
            if ((flags & MAP_COHERENT_BIT) != 0) glFlags |= DarkOpenGLLinker.GL_MAP_COHERENT_BIT;
            
            MemorySegment ptr = (MemorySegment) DarkOpenGLLinker.glMapBufferRange.invokeExact(glTarget, offset, length, glFlags);
            return ptr.reinterpret(length);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unmapBuffer(int target, int bufferId) {
        try {
            int glTarget = mapTarget(target);
            DarkOpenGLLinker.glBindBuffer.invokeExact(glTarget, bufferId);
            DarkOpenGLLinker.glUnmapBuffer.invokeExact(glTarget);
        } catch (Throwable e) {
            sv.dark.core.DarkLogger.error("RHI", "Error in unmapBuffer: " + e.getMessage());
        }
    }

    @Override
    public void deleteBuffer(int bufferId) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bufferPtr = arena.allocate(ValueLayout.JAVA_INT);
            bufferPtr.set(ValueLayout.JAVA_INT, 0L, bufferId);
            DarkOpenGLLinker.glDeleteBuffers.invokeExact(1, bufferPtr);
        } catch (Throwable e) {}
    }

    @Override
    public void deleteBuffers(int[] bufferIds) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT, bufferIds.length);
            for(int i=0; i<bufferIds.length; i++) buffers.set(ValueLayout.JAVA_INT, i*4L, bufferIds[i]);
            DarkOpenGLLinker.glDeleteBuffers.invokeExact(bufferIds.length, buffers);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public int createTexture2D(int width, int height, int internalFormat, int format, int type, int filter) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenTextures.invokeExact(1, texPtr);
            int tex = texPtr.get(ValueLayout.JAVA_INT, 0);
            
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, tex);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, MemorySegment.NULL);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, filter);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, filter);
            return tex;
        } catch(Throwable t){ throw new RuntimeException(t); }
    }

    @Override
    public int createTexture2DArray(int width, int height, int depth, int internalFormat, int format, int type, int filter) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenTextures.invokeExact(1, texPtr);
            int tex = texPtr.get(ValueLayout.JAVA_INT, 0);
            
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, tex);
            DarkOpenGLLinker.glTexImage3D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, 0, internalFormat, width, height, depth, 0, format, type, MemorySegment.NULL);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, filter);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, filter);
            return tex;
        } catch(Throwable t){ throw new RuntimeException(t); }
    }

    @Override
    public void resizeTexture2D(int textureId, int width, int height, int internalFormat, int format, int type) {
        try {
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, textureId);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, MemorySegment.NULL);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void deleteTextures(int[] textureIds) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT, textureIds.length);
            for(int i=0; i<textureIds.length; i++) texPtr.set(ValueLayout.JAVA_INT, i*4, textureIds[i]);
            DarkOpenGLLinker.glDeleteTextures.invokeExact(textureIds.length, texPtr);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public int createFramebuffer() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fboPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenFramebuffers.invokeExact(1, fboPtr);
            return fboPtr.get(ValueLayout.JAVA_INT, 0);
        } catch(Throwable t){ throw new RuntimeException(t); }
    }

    @Override
    public void framebufferTexture2D(int fboId, int attachment, int textureId, int level) {
        try {
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, fboId);
            DarkOpenGLLinker.glFramebufferTexture2D.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, attachment, DarkOpenGLLinker.GL_TEXTURE_2D, textureId, level);
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void framebufferTexture(int fboId, int attachment, int textureId, int level) {
        try {
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, fboId);
            DarkOpenGLLinker.glFramebufferTexture.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, attachment, textureId, level);
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public boolean checkFramebufferStatus(int fboId) {
        try {
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, fboId);
            boolean ok = (int)DarkOpenGLLinker.glCheckFramebufferStatus.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER) == DarkOpenGLLinker.GL_FRAMEBUFFER_COMPLETE;
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);
            return ok;
        } catch(Throwable t){ return false; }
    }

    @Override
    public void setDrawBuffers(int fboId, int[] attachments) {
        try (Arena arena = Arena.ofConfined()) {
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, fboId);
            MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT, attachments.length);
            for(int i=0; i<attachments.length; i++) buffers.set(ValueLayout.JAVA_INT, i*4, attachments[i]);
            DarkOpenGLLinker.glDrawBuffers.invokeExact(attachments.length, buffers);
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void setDrawBufferNone(int fboId) {
        try {
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, fboId);
            DarkOpenGLLinker.glDrawBuffer.invokeExact(DarkOpenGLLinker.GL_NONE);
            DarkOpenGLLinker.glReadBuffer.invokeExact(DarkOpenGLLinker.GL_NONE);
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void deleteFramebuffer(int fboId) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fboPtr = arena.allocate(ValueLayout.JAVA_INT);
            fboPtr.set(ValueLayout.JAVA_INT, 0L, fboId);
            DarkOpenGLLinker.glDeleteFramebuffers.invokeExact(1, fboPtr);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void deleteFramebuffers(int[] fboIds) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fboPtr = arena.allocate(ValueLayout.JAVA_INT, fboIds.length);
            for(int i=0; i<fboIds.length; i++) fboPtr.set(ValueLayout.JAVA_INT, i*4L, fboIds[i]);
            DarkOpenGLLinker.glDeleteFramebuffers.invokeExact(fboIds.length, fboPtr);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    // --- DarkRHICommandList Implementation ---
    @Override
    public void begin() {
        // No-op in OpenGL
    }

    @Override
    public void bindPipeline(int pipelineId) {
        try { DarkOpenGLLinker.glUseProgram.invokeExact(pipelineId); } catch(Throwable t){}
    }

    @Override
    public void bindBuffer(int target, int index, int bufferId) {
        try {
            int glTarget = mapTarget(target);
            DarkOpenGLLinker.glBindBufferBase.invokeExact(glTarget, index, bufferId);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateBufferData(int target, int bufferId, long offset, MemorySegment data, long size) {
        try {
            int glTarget = mapTarget(target);
            DarkOpenGLLinker.glBindBuffer.invokeExact(glTarget, bufferId);
            DarkOpenGLLinker.glBufferSubData.invokeExact(glTarget, offset, size, data);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUniform1f(int location, float v0) {
        try { DarkOpenGLLinker.glUniform1f.invokeExact(location, v0); } catch(Throwable t){}
    }

    @Override
    public void setUniform2f(int location, float v0, float v1) {
        try { DarkOpenGLLinker.glUniform2f.invokeExact(location, v0, v1); } catch(Throwable t){}
    }

    @Override
    public void setUniform3f(int location, float v0, float v1, float v2) {
        try { DarkOpenGLLinker.glUniform3f.invokeExact(location, v0, v1, v2); } catch(Throwable t){}
    }

    @Override
    public void setUniform4f(int location, float v0, float v1, float v2, float v3) {
        try { DarkOpenGLLinker.glUniform4f.invokeExact(location, v0, v1, v2, v3); } catch(Throwable t){}
    }

    @Override
    public void setUniform1i(int location, int v0) {
        try { DarkOpenGLLinker.glUniform1i.invokeExact(location, v0); } catch(Throwable t){}
    }
    
    @Override
    public void setUniform1ui(int location, int v0) {
        try { DarkOpenGLLinker.glUniform1ui.invokeExact(location, v0); } catch(Throwable t){}
    }

    @Override
    public void setUniformMatrix4fv(int location, int count, boolean transpose, MemorySegment value) {
        try { DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(location, count, transpose, value); } catch(Throwable t){}
    }

    @Override
    public void bindTexture2D(int unit, int textureId) {
        try {
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE0 + unit);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, textureId);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void bindTexture2DArray(int unit, int textureId) {
        try {
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE0 + unit);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, textureId);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void bindImageTexture(int unit, int textureId, int level, boolean layered, int layer, int access, int format) {
        try {
            int glAccess = access == ACCESS_READ_WRITE ? DarkOpenGLLinker.GL_READ_WRITE : 0;
            int glFormat = format == FORMAT_RGBA16F ? DarkOpenGLLinker.GL_RGBA16F : DarkOpenGLLinker.GL_RGBA8;
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE0 + unit);
            DarkOpenGLLinker.glBindImageTexture.invokeExact(unit, textureId, level, layered, layer, glAccess, glFormat);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void dispatchCompute(int groupsX, int groupsY, int groupsZ) {
        try { DarkOpenGLLinker.glDispatchCompute.invokeExact(groupsX, groupsY, groupsZ); } catch(Throwable t){}
    }

    @Override
    public void memoryBarrier(int flags) {
        try {
            int glFlags = 0;
            if ((flags & BARRIER_SHADER_STORAGE) != 0) glFlags |= DarkOpenGLLinker.GL_SHADER_STORAGE_BARRIER_BIT;
            if ((flags & BARRIER_SHADER_IMAGE) != 0) glFlags |= DarkOpenGLLinker.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
            DarkOpenGLLinker.glMemoryBarrier.invokeExact(glFlags);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }
    
    @Override
    public void bindFramebuffer(int fboId) {
        try { DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, fboId); } catch(Throwable t){}
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        try { DarkOpenGLLinker.glViewport.invokeExact(x, y, width, height); } catch(Throwable t){}
    }

    @Override
    public void clear(int mask) {
        try { DarkOpenGLLinker.glClear.invokeExact(mask); } catch(Throwable t){}
    }

    @Override
    public void bindFramebufferTextureLayer(int fboId, int attachment, int textureId, int level, int layer) {
        try {
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, fboId);
            DarkOpenGLLinker.glFramebufferTextureLayer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, attachment, textureId, level, layer);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void drawElementsInstanced(int mode, int count, int type, MemorySegment indices, int instancecount) {
        try {
            DarkOpenGLLinker.glDrawElementsInstanced.invokeExact(mode, count, type, indices, instancecount);
        } catch(Throwable t) {
            sv.dark.core.DarkLogger.error("RHI", "Error in deleteBuffers: " + t.getMessage());
        }
    }

    @Override
    public void end() {
        // No-op
    }

    @Override
    public void submit() {
        // No-op in OpenGL
    }
}
