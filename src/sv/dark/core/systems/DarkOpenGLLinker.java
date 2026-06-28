// Reading Order: 00011001
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;

/**
 * RESPONSIBILITY: Bind OpenGL 4.3 Compute Shaders API directly from the graphics driver.
 * WHY: Multi-platform compatibility without external massive wrappers like LWJGL.
 * TECHNIQUE: glfwGetProcAddress via Panama FFI downcalls.
 * GUARANTEES: 0ms overhead in GPU draw calls / dispatch.
 */
@AAACertified(date = "2026-06-14", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Native FFI Linker for OpenGL 4.3 Compute Shaders")
public final class DarkOpenGLLinker {
    private static final Linker LINKER = Linker.nativeLinker();

    public static MethodHandle glCreateShader;
    public static MethodHandle glShaderSource;
    public static MethodHandle glCompileShader;
    public static MethodHandle glGetShaderiv;
    public static MethodHandle glGetShaderInfoLog;
    public static MethodHandle glCreateProgram;
    public static MethodHandle glDeleteProgram;
    public static MethodHandle glAttachShader;
    public static MethodHandle glLinkProgram;
    public static MethodHandle glUseProgram;
    public static MethodHandle glGenBuffers;
    public static MethodHandle glBindBuffer;
    public static MethodHandle glBufferData;
    public static MethodHandle glBufferSubData;
    public static MethodHandle glBindBufferBase;
    public static MethodHandle glDispatchCompute;
    public static MethodHandle glMemoryBarrier;
    public static MethodHandle glDrawArraysInstanced;
    public static MethodHandle glDrawElementsInstanced;
    public static MethodHandle glDrawElementsInstancedBaseVertexBaseInstance;

    // Phase 35+ AZDO
    public static MethodHandle glBufferStorage;
    public static MethodHandle glMapBufferRange;
    public static MethodHandle glUnmapBuffer;
    public static MethodHandle glDeleteBuffers;

    // Phase 27 - Deferred Pipeline Bindings
    public static MethodHandle glDrawBuffers;
    public static MethodHandle glDeleteShader;
    public static MethodHandle glUniform4f;
    public static MethodHandle glGenTextures;
    public static MethodHandle glDeleteTextures;
    public static MethodHandle glBindTexture;
    public static MethodHandle glTexImage2D;
    public static MethodHandle glTexSubImage2D;
    public static MethodHandle glTexImage3D;
    public static MethodHandle glTexParameteri;
    public static MethodHandle glTexParameterfv;
    public static MethodHandle glGenFramebuffers;
    public static MethodHandle glDeleteFramebuffers;
    public static MethodHandle glBindFramebuffer;
    public static MethodHandle glFramebufferTexture;
    public static MethodHandle glFramebufferTexture2D;
    public static MethodHandle glFramebufferTextureLayer;
    public static MethodHandle glCheckFramebufferStatus;
    public static MethodHandle glBindImageTexture;
    
    // Phase 27 - Dynamic Uniforms & Bugfixes
    public static MethodHandle glGetUniformLocation;
    public static MethodHandle glUniform3f;
    public static MethodHandle glUniform2f;
    public static MethodHandle glUniform1f;
    public static MethodHandle glUniform1ui;
    public static MethodHandle glActiveTexture;
    public static MethodHandle glDrawBuffer;
    public static MethodHandle glReadBuffer;
    public static MethodHandle glClear;

    // Phase 35 - ImGui Rendering
    public static MethodHandle glGenVertexArrays;
    public static MethodHandle glDeleteVertexArrays;
    public static MethodHandle glBindVertexArray;
    public static MethodHandle glEnableVertexAttribArray;
    public static MethodHandle glVertexAttribPointer;
    public static MethodHandle glDrawElements;
    public static MethodHandle glScissor;
    public static MethodHandle glEnable;
    public static MethodHandle glDisable;
    public static MethodHandle glBlendEquation;
    public static MethodHandle glBlendFuncSeparate;
    public static MethodHandle glBlendFunc;
    public static MethodHandle glUniformMatrix4fv;
    public static MethodHandle glUniform1i;
    public static MethodHandle glViewport;
    public static MethodHandle glGetIntegerv;
    public static MethodHandle glIsProgram;
    public static MethodHandle glPixelStorei;

    // Constantes OpenGL Faltantes Hotfix Phase 29
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    public static final int GL_DEPTH_COMPONENT32F = 0x8CAC;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;

    public static final int GL_UNSIGNED_INT = 0x1405;
    
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_PERSISTENT_BIT = 0x0040;
    public static final int GL_MAP_COHERENT_BIT = 0x0080;
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x0100;

    public static final int GL_CLAMP_TO_BORDER = 0x812D;
    public static final int GL_TEXTURE_BORDER_COLOR = 0x1004;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    public static final int GL_NONE = 0;
    public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;

    // Constantes OpenGL
    public static final int GL_COMPUTE_SHADER = 0x91B9;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = 0x2000;
    public static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x00000020;

    // Phase 27 Constants
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_COLOR_ATTACHMENT1 = 0x8CE1;
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_READ_WRITE = 0x88BA;
    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE1 = 0x84C1;
    public static final int GL_TEXTURE2 = 0x84C2;
    public static final int GL_TEXTURE3 = 0x84C3;
    public static final int GL_TEXTURE4 = 0x84C4;
    public static final int GL_TEXTURE5 = 0x84C5;
    public static final int GL_COLOR_ATTACHMENT2 = 0x8CE2;

    // Phase 35 Constants
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_SCISSOR_TEST = 0x0C11;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_FUNC_ADD = 0x8006;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_VIEWPORT = 0x0BA2;
    public static final int GL_UNPACK_ROW_LENGTH = 0x0CF2;
    public static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;

    public static void init() {
        try (Arena arena = Arena.ofConfined()) {
            DarkLogger.info("GRAPHICS", "Vinculando funciones OpenGL 4.3 (Compute Culling)...");

            glCreateShader = bind(arena, "glCreateShader", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glShaderSource = bind(arena, "glShaderSource", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            glCompileShader = bind(arena, "glCompileShader", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glGetShaderiv = bind(arena, "glGetShaderiv", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glGetShaderInfoLog = bind(arena, "glGetShaderInfoLog", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            glCreateProgram = bind(arena, "glCreateProgram", FunctionDescriptor.of(ValueLayout.JAVA_INT));
            glDeleteProgram = bind(arena, "glDeleteProgram", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glAttachShader = bind(arena, "glAttachShader", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glLinkProgram = bind(arena, "glLinkProgram", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glUseProgram = bind(arena, "glUseProgram", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glGenBuffers = bind(arena, "glGenBuffers", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glBindBuffer = bind(arena, "glBindBuffer", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glBufferData = bind(arena, "glBufferData", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            glBufferSubData = bind(arena, "glBufferSubData", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            glBindBufferBase = bind(arena, "glBindBufferBase", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glDispatchCompute = bind(arena, "glDispatchCompute", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glMemoryBarrier = bind(arena, "glMemoryBarrier", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            
            // Phase 27 Bindings & Hotfix
            glDrawBuffers = bind(arena, "glDrawBuffers", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glDeleteShader = bind(arena, "glDeleteShader", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glUniform4f = bind(arena, "glUniform4f", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            glDeleteBuffers = bind(arena, "glDeleteBuffers", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glTexImage3D = bind(arena, "glTexImage3D", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glTexParameterfv = bind(arena, "glTexParameterfv", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glFramebufferTexture = bind(arena, "glFramebufferTexture", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glFramebufferTextureLayer = bind(arena, "glFramebufferTextureLayer", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glDrawBuffer = bind(arena, "glDrawBuffer", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glReadBuffer = bind(arena, "glReadBuffer", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glClear = bind(arena, "glClear", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glDrawArraysInstanced = bind(arena, "glDrawArraysInstanced", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glDrawElementsInstanced = bind(arena, "glDrawElementsInstanced", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            glDrawElementsInstancedBaseVertexBaseInstance = bind(arena, "glDrawElementsInstancedBaseVertexBaseInstance", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            
            // AZDO
            glBufferStorage = bind(arena, "glBufferStorage", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            glMapBufferRange = bind(arena, "glMapBufferRange", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
            glUnmapBuffer = bind(arena, "glUnmapBuffer", FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT));

            // Phase 27 - Textures and FBOs
            glGenTextures = bind(arena, "glGenTextures", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glDeleteTextures = bind(arena, "glDeleteTextures", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glBindTexture = bind(arena, "glBindTexture", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glTexImage2D = bind(arena, "glTexImage2D", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glTexSubImage2D = bind(arena, "glTexSubImage2D", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glTexParameteri = bind(arena, "glTexParameteri", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            
            glGenFramebuffers = bind(arena, "glGenFramebuffers", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glDeleteFramebuffers = bind(arena, "glDeleteFramebuffers", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glBindFramebuffer = bind(arena, "glBindFramebuffer", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glFramebufferTexture2D = bind(arena, "glFramebufferTexture2D", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glCheckFramebufferStatus = bind(arena, "glCheckFramebufferStatus", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            
            // Note: GLboolean is typically 1 byte in C, mapped to JAVA_BYTE, but Project Panama handles JAVA_BOOLEAN internally as 1 byte too.
            glBindImageTexture = bind(arena, "glBindImageTexture", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            // Dynamic Uniforms & Bugfixes
            glGetUniformLocation = bind(arena, "glGetUniformLocation", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glUniform3f = bind(arena, "glUniform3f", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            glUniform2f = bind(arena, "glUniform2f", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            glUniform1f = bind(arena, "glUniform1f", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT));
            glUniform1ui = bind(arena, "glUniform1ui", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glActiveTexture = bind(arena, "glActiveTexture", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));

            // Phase 35 - ImGui Rendering
            glGenVertexArrays = bind(arena, "glGenVertexArrays", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glDeleteVertexArrays = bind(arena, "glDeleteVertexArrays", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glBindVertexArray = bind(arena, "glBindVertexArray", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glEnableVertexAttribArray = bind(arena, "glEnableVertexAttribArray", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glVertexAttribPointer = bind(arena, "glVertexAttribPointer", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
            glDrawElements = bind(arena, "glDrawElements", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
            glScissor = bind(arena, "glScissor", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glEnable = bind(arena, "glEnable", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glDisable = bind(arena, "glDisable", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glBlendEquation = bind(arena, "glBlendEquation", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glBlendFuncSeparate = bind(arena, "glBlendFuncSeparate", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glBlendFunc = bind(arena, "glBlendFunc", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glUniformMatrix4fv = bind(arena, "glUniformMatrix4fv", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS));
            glUniform1i = bind(arena, "glUniform1i", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glViewport = bind(arena, "glViewport", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glGetIntegerv = bind(arena, "glGetIntegerv", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glIsProgram = bind(arena, "glIsProgram", FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT));
            glPixelStorei = bind(arena, "glPixelStorei", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            DarkLogger.info("GRAPHICS", "Punteros de OpenGL 4.3 FFI mapeados exitosamente.");
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Fallo catastrofico al vincular OpenGL FFI. El SO soporta OpenGL 4.3?", e);
            System.exit(1);
        }
    }

    private static MethodHandle bind(Arena arena, String funcName, FunctionDescriptor desc) throws Throwable {
        MemorySegment funcStr = arena.allocateFrom(funcName);
        MemorySegment ptr = (MemorySegment) DarkGraphicsLinker.glfwGetProcAddress.invokeExact(funcStr);
        if (ptr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("No se encontro puntero nativo para " + funcName);
        }
        return LINKER.downcallHandle(ptr, desc);
    }
}
