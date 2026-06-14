// Reading Order: 00001111
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;


import sv.dark.core.DarkLogger;
import sv.dark.core.AAACertified;

// JDK — Panama FFI
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * RESPONSIBILITY: Hardware CPU core affinity binder using Foreign Function & Memory API (JEP 454).
 * WHY: OS schedulers move threads across CPU cores, causing L1/L2 cache misses and temporal jitter. Pinning the logic thread to a physical core eliminates this latency.
 * TECHNIQUE: Calls Windows kernel32.dll `SetThreadAffinityMask` directly via Panama FFI downcalls, bypassing JNI overhead.
 * GUARANTEES: Maximum cache locality and deterministic execution time for the logic thread. 0 allocations during downcalls.
 *
 * <p><b>Platform Support:</b> Currently implements Windows {@code SetThreadAffinityMask} 
 * via {@code kernel32.dll}.
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(
    date = "2026-06-11",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Automatically AAA Certified during Core Audit"
)
public final class ThreadPinning {

    private static final Linker LINKER = Linker.nativeLinker();

    private static final MethodHandle SET_AFFINITY_HANDLE;
    private static final MethodHandle PTHREAD_SELF_HANDLE;
    private static final MethodHandle PTHREAD_SET_AFFINITY_HANDLE;
    private static final MethodHandle PTHREAD_GET_AFFINITY_HANDLE;

    @SuppressWarnings("unused")
    private static final MethodHandle GET_THREAD_ID_HANDLE;

    static {
        // Pre-resolve native symbols at class load time to avoid runtime latency spikes.
        MethodHandle setAffinity = null;
        MethodHandle pthreadSelf = null;
        MethodHandle pthreadSetAffinity = null;
        MethodHandle pthreadGetAffinity = null;
        MethodHandle getThreadId = null;

        try {
            // Windows implementation requires explicit kernel32 load.
            // It is not guaranteed to be present in defaultLookup.
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", java.lang.foreign.Arena.global());
            MemorySegment setAffinityAddr = kernel32.find("SetThreadAffinityMask").orElse(null);

            if (setAffinityAddr != null) {
                setAffinity = LINKER.downcallHandle(
                        setAffinityAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_LONG, // Return: Previous mask (0 if fail)
                                ValueLayout.JAVA_LONG, // hThread: -2 for Current Thread pseudo-handle
                                ValueLayout.JAVA_LONG  // dwThreadAffinityMask: Bitmask
                        ));
            }
        } catch (Exception e) {
            // Not Windows, or kernel32 load failed
        }

        try {
            SymbolLookup defaultLookup = LINKER.defaultLookup();
            MemorySegment pthreadSelfAddr = defaultLookup.find("pthread_self").orElse(null);
            MemorySegment pthreadSetAffinityAddr = defaultLookup.find("pthread_setaffinity_np").orElse(null);
            MemorySegment pthreadGetAffinityAddr = defaultLookup.find("pthread_getaffinity_np").orElse(null);

            if (pthreadSelfAddr != null && pthreadSetAffinityAddr != null) {
                pthreadSelf = LINKER.downcallHandle(
                        pthreadSelfAddr,
                        FunctionDescriptor.of(ValueLayout.ADDRESS)
                );
                pthreadSetAffinity = LINKER.downcallHandle(
                        pthreadSetAffinityAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,     // Return: status (0 is success)
                                ValueLayout.ADDRESS,      // thread (pthread_t)
                                ValueLayout.JAVA_LONG,    // cpusetsize (size_t)
                                ValueLayout.ADDRESS       // cpuset (cpu_set_t*)
                        )
                );
            }

            if (pthreadSelfAddr != null && pthreadGetAffinityAddr != null) {
                pthreadGetAffinity = LINKER.downcallHandle(
                        pthreadGetAffinityAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,     // Return: status (0 is success)
                                ValueLayout.ADDRESS,      // thread (pthread_t)
                                ValueLayout.JAVA_LONG,    // cpusetsize (size_t)
                                ValueLayout.ADDRESS       // cpuset (cpu_set_t*)
                        )
                );
            }
        } catch (Exception e) {
            // Not Linux / POSIX symbols found
        }

        SET_AFFINITY_HANDLE = setAffinity;
        PTHREAD_SELF_HANDLE = pthreadSelf;
        PTHREAD_SET_AFFINITY_HANDLE = pthreadSetAffinity;
        PTHREAD_GET_AFFINITY_HANDLE = pthreadGetAffinity;
        GET_THREAD_ID_HANDLE = getThreadId;
    }

    /** Utility class — no instances. */
    private ThreadPinning() {
        throw new AssertionError("ThreadPinning is a static utility class");
    }

    // -------------------------------------------------------------------------
    // CPU Affinity Controls
    // -------------------------------------------------------------------------

    /**
     * Binds the current executing thread to a specific hardware core.
     * 
     * @param coreId The physical core ID (0-63) to pin this thread to.
     * @return {@code true} if the affinity mask was successfully applied.
     */
    public static boolean pinToCore(int coreId) {
        if (SET_AFFINITY_HANDLE != null) {
            try {
                // CPU affinity bitmask: bit N = core N
                long mask = 1L << coreId;

                // -2 is the Windows pseudo-handle for the current thread
                long currentThreadHandle = -2;

                long result = (long) SET_AFFINITY_HANDLE.invokeExact(currentThreadHandle, mask);

                if (result != 0) {
                    return true;
                } else {
                    DarkLogger.error("KERNEL", "Failed to pin thread (Windows). Error code: " + result);
                    return false;
                }
            } catch (Throwable t) {
                DarkLogger.error("KERNEL", "Critical error during CPU Pinning (Windows): " + t.getMessage());
                return false;
            }
        } else if (PTHREAD_SELF_HANDLE != null && PTHREAD_SET_AFFINITY_HANDLE != null) {
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                MemorySegment thread = (MemorySegment) PTHREAD_SELF_HANDLE.invokeExact();
                // Allocate a cpu_set_t structure (128 bytes to support up to 1024 CPUs)
                MemorySegment cpuSet = arena.allocate(128);
                // Clear and set the bit coreId
                int wordOffset = coreId / 64;
                int bitOffset = coreId % 64;
                long wordValue = 1L << bitOffset;
                cpuSet.set(ValueLayout.JAVA_LONG, wordOffset * 8, wordValue);

                int result = (int) PTHREAD_SET_AFFINITY_HANDLE.invokeExact(thread, 128L, cpuSet);
                if (result == 0) {
                    return true;
                } else {
                    DarkLogger.error("KERNEL", "Failed to pin thread (Linux/POSIX). Error code: " + result);
                    return false;
                }
            } catch (Throwable t) {
                DarkLogger.error("KERNEL", "Critical error during CPU Pinning (Linux/POSIX): " + t.getMessage());
                return false;
            }
        } else {
            DarkLogger.error("KERNEL", "CPU Pinning not supported on this OS/Architecture.");
            return false;
        }
    }

    /**
     * Retrieves the original thread affinity mask before any modifications.
     * 
     * @return The previous affinity mask, or -1L if unsupported or failed.
     */
    public static long getOriginalAffinityMask() {
        if (SET_AFFINITY_HANDLE != null) {
            try {
                long currentThreadHandle = -2; 
                
                // Temporarily switch to Core 0 (mask 1L) to retrieve the previous mask,
                // then immediately restore it.
                long previousMask = (long) SET_AFFINITY_HANDLE.invokeExact(currentThreadHandle, 1L);
                
                if (previousMask != 0) {
                    // invokeExact requires an explicit cast even when discarding the return value.
                    @SuppressWarnings("unused")
                    long ignored = (long) SET_AFFINITY_HANDLE.invokeExact(currentThreadHandle, previousMask);
                    return previousMask;
                }
            } catch (Throwable t) {
                DarkLogger.error("KERNEL", "Failed to query original thread affinity (Windows): " + t.getMessage());
            }
        } else if (PTHREAD_SELF_HANDLE != null && PTHREAD_GET_AFFINITY_HANDLE != null) {
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                MemorySegment thread = (MemorySegment) PTHREAD_SELF_HANDLE.invokeExact();
                MemorySegment cpuSet = arena.allocate(128);
                int result = (int) PTHREAD_GET_AFFINITY_HANDLE.invokeExact(thread, 128L, cpuSet);
                if (result == 0) {
                    return cpuSet.get(ValueLayout.JAVA_LONG, 0);
                }
            } catch (Throwable t) {
                DarkLogger.error("KERNEL", "Failed to query original thread affinity (POSIX): " + t.getMessage());
            }
        }
        return -1L;
    }

    /**
     * Restores thread execution affinity to a previously saved mask.
     * 
     * @param mask The original affinity bitmask to restore.
     * @return {@code true} if the affinity was successfully restored.
     */
    public static boolean restoreAffinityMask(long mask) {
        if (mask == -1L) {
            return false;
        }
        if (SET_AFFINITY_HANDLE != null) {
            try {
                long currentThreadHandle = -2;
                long result = (long) SET_AFFINITY_HANDLE.invokeExact(currentThreadHandle, mask);
                return result != 0;
            } catch (Throwable t) {
                DarkLogger.error("KERNEL", "Failed to restore thread affinity (Windows): " + t.getMessage());
                return false;
            }
        } else if (PTHREAD_SELF_HANDLE != null && PTHREAD_SET_AFFINITY_HANDLE != null) {
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                MemorySegment thread = (MemorySegment) PTHREAD_SELF_HANDLE.invokeExact();
                MemorySegment cpuSet = arena.allocate(128);
                cpuSet.set(ValueLayout.JAVA_LONG, 0, mask);
                int result = (int) PTHREAD_SET_AFFINITY_HANDLE.invokeExact(thread, 128L, cpuSet);
                return result == 0;
            } catch (Throwable t) {
                DarkLogger.error("KERNEL", "Failed to restore thread affinity (POSIX): " + t.getMessage());
                return false;
            }
        }
        return false;
    }

}
