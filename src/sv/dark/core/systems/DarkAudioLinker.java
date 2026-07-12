// Reading Order: 00101111
//  47
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.io.File;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;

/**
 * RESPONSIBILITY: Foreign Function Interface (FFI) for Native Spatial Audio.
 * WHY: Java Sound API (javax.sound) is slow, blocking, and lacks 3D spatial hardware acceleration.
 * TECHNIQUE: Linker.nativeLinker().downcallHandle() binds OpenAL Soft functions at runtime.
 * GUARANTEES: Direct sound card communication. Massive polyphony without GC overhead.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(date = "2026-06-13", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Native FFI Linker for OpenAL Soft")
public final class DarkAudioLinker {
    
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup OPENAL;
    
    static {
        // Load the OpenAL Soft native library dynamically using cross-platform resolver
        File oalDll = sv.dark.core.util.NativeLibraryResolver.resolveLibrary("soft_oal");
        if (!oalDll.exists()) {
            DarkLogger.fatal("AUDIO", "Missing " + oalDll.getName() + "! Spatial Audio disabled.", null);
            throw new RuntimeException("Missing " + oalDll.getName());
        }
        System.load(oalDll.getAbsolutePath());
        OPENAL = SymbolLookup.loaderLookup();
        DarkLogger.info("AUDIO", "Project Panama FFI: " + oalDll.getName() + " loaded successfully.");
    }
    
    // =========================================================================
    // NATIVE METHOD HANDLES (Zero-GC Pointers to OpenAL)
    // =========================================================================

    public static final MethodHandle alcOpenDevice = LINKER.downcallHandle(
        OPENAL.find("alcOpenDevice").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS) // Returns ALCdevice*, takes ALCchar*
    );
    
    public static final MethodHandle alcCreateContext = LINKER.downcallHandle(
        OPENAL.find("alcCreateContext").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS) // Returns ALCcontext*, takes ALCdevice*, ALCint*
    );
    
    public static final MethodHandle alcMakeContextCurrent = LINKER.downcallHandle(
        OPENAL.find("alcMakeContextCurrent").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.ADDRESS) // Returns ALCboolean, takes ALCcontext*
    );

    // alListener3f(ALenum param, ALfloat value1, ALfloat value2, ALfloat value3)
    public static final MethodHandle alListener3f = LINKER.downcallHandle(
        OPENAL.find("alListener3f").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT)
    );

    // alGenSources(ALsizei n, ALuint *sources)
    public static final MethodHandle alGenSources = LINKER.downcallHandle(
        OPENAL.find("alGenSources").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    );

    // alGenBuffers(ALsizei n, ALuint *buffers)
    public static final MethodHandle alGenBuffers = LINKER.downcallHandle(
        OPENAL.find("alGenBuffers").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    );

    // alDeleteSources(ALsizei n, const ALuint *sources)
    public static final MethodHandle alDeleteSources = LINKER.downcallHandle(
        OPENAL.find("alDeleteSources").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    );

    // alDeleteBuffers(ALsizei n, const ALuint *buffers)
    public static final MethodHandle alDeleteBuffers = LINKER.downcallHandle(
        OPENAL.find("alDeleteBuffers").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    );

    // alBufferData(ALuint buffer, ALenum format, const ALvoid *data, ALsizei size, ALsizei freq)
    public static final MethodHandle alBufferData = LINKER.downcallHandle(
        OPENAL.find("alBufferData").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
    );

    // alSourcePlay(ALuint source)
    public static final MethodHandle alSourcePlay = LINKER.downcallHandle(
        OPENAL.find("alSourcePlay").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT)
    );

    // alSource3f(ALuint source, ALenum param, ALfloat value1, ALfloat value2, ALfloat value3)
    public static final MethodHandle alSource3f = LINKER.downcallHandle(
        OPENAL.find("alSource3f").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT)
    );

    // alSourcei(ALuint source, ALenum param, ALint value)
    public static final MethodHandle alSourcei = LINKER.downcallHandle(
        OPENAL.find("alSourcei").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
    );

    public static final MethodHandle alcDestroyContext = LINKER.downcallHandle(
        OPENAL.find("alcDestroyContext").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    public static final MethodHandle alcCloseDevice = LINKER.downcallHandle(
        OPENAL.find("alcCloseDevice").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.ADDRESS)
    );

    // OpenAL Constants
    public static final int AL_POSITION = 0x1004;
    public static final int AL_VELOCITY = 0x1006;
    public static final int AL_BUFFER = 0x1009;
    public static final int AL_LOOPING = 0x1007;
}
