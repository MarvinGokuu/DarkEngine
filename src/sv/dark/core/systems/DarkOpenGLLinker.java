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
    public static MethodHandle glAttachShader;
    public static MethodHandle glLinkProgram;
    public static MethodHandle glUseProgram;
    public static MethodHandle glGenBuffers;
    public static MethodHandle glBindBuffer;
    public static MethodHandle glBufferData;
    public static MethodHandle glBindBufferBase;
    public static MethodHandle glDispatchCompute;
    public static MethodHandle glMemoryBarrier;

    // Constantes OpenGL
    public static final int GL_COMPUTE_SHADER = 0x91B9;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = 0x2000;

    public static void init() {
        try (Arena arena = Arena.ofConfined()) {
            DarkLogger.info("GRAPHICS", "Vinculando funciones OpenGL 4.3 (Compute Culling)...");

            glCreateShader = bind(arena, "glCreateShader", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glShaderSource = bind(arena, "glShaderSource", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            glCompileShader = bind(arena, "glCompileShader", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glGetShaderiv = bind(arena, "glGetShaderiv", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glGetShaderInfoLog = bind(arena, "glGetShaderInfoLog", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            glCreateProgram = bind(arena, "glCreateProgram", FunctionDescriptor.of(ValueLayout.JAVA_INT));
            glAttachShader = bind(arena, "glAttachShader", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glLinkProgram = bind(arena, "glLinkProgram", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glUseProgram = bind(arena, "glUseProgram", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            glGenBuffers = bind(arena, "glGenBuffers", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            glBindBuffer = bind(arena, "glBindBuffer", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glBufferData = bind(arena, "glBufferData", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            glBindBufferBase = bind(arena, "glBindBufferBase", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glDispatchCompute = bind(arena, "glDispatchCompute", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            glMemoryBarrier = bind(arena, "glMemoryBarrier", FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));

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
