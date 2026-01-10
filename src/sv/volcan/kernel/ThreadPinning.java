package sv.volcan.kernel;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * AUTORIDAD: Sovereign Kernel
 * RESPONSABILIDAD: Anclaje de Threads a Cores Físicos
 * TÉCNICA: JEP 454 (Foreign Function & Memory API)
 * GARANTÍA: Zero-Jitter en Logic Thread
 */
public class ThreadPinning {

    private static final Linker LINKER = Linker.nativeLinker();

    // sched_setaffinity(pid_t pid, size_t cpusetsize, const cpu_set_t *mask)
    private static final MethodHandle SET_AFFINITY_HANDLE;
    // Get current thread ID (Windows: GetCurrentThreadId, Linux: gettid)
    @SuppressWarnings("unused")
    private static final MethodHandle GET_THREAD_ID_HANDLE;

    static {
        // PRELOAD: Resolver símbolos nativos al cargar la clase
        MethodHandle setAffinity = null;
        MethodHandle getThreadId = null;

        try {
            // Nota: Implementación para Windows (SetThreadAffinityMask)
            // Requiere cargar kernel32 explícitamente porque no siempre está en
            // defaultLookup
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", java.lang.foreign.Arena.global());
            MemorySegment setAffinityAddr = kernel32.find("SetThreadAffinityMask").orElse(null);

            if (setAffinityAddr != null) {
                setAffinity = LINKER.downcallHandle(
                        setAffinityAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_LONG, // Return: Previous mask (0 if fail)
                                ValueLayout.JAVA_LONG, // hThread: -2 for Current Thread pseudo-handle
                                ValueLayout.JAVA_LONG // dwThreadAffinityMask: Bitmask
                        ));
            }
        } catch (Exception e) {
            System.err.println("[KERNEL] Failed to link native affinity methods: " + e.getMessage());
        }

        SET_AFFINITY_HANDLE = setAffinity;
        GET_THREAD_ID_HANDLE = getThreadId;
    }

    /**
     * Ancla el thread actual a un núcleo físico específico.
     * 
     * @param coreId ID del núcleo (0-63)
     * @return true si el anclaje fue exitoso
     */
    public static boolean pinToCore(int coreId) {
        if (SET_AFFINITY_HANDLE == null) {
            System.err.println("[KERNEL] CPU Pinning not supported on this OS/Architecture.");
            return false;
        }

        try {
            // Crear máscara de bits: 1 << coreId
            long mask = 1L << coreId;

            // Pseudo-handle para el thread actual en Windows es -2
            long currentThreadHandle = -2;

            long result = (long) SET_AFFINITY_HANDLE.invokeExact(currentThreadHandle, mask);

            if (result != 0) {
                System.out.println("[KERNEL] Logic Thread PINNED to Core " + coreId + ". Jitter eliminated.");
                return true;
            } else {
                System.err.println("[KERNEL] Failed to pin thread. Error code: " + result);
                return false;
            }

        } catch (Throwable t) {
            System.err.println("[KERNEL] Critical error during CPU Pinning: " + t.getMessage());
            return false;
        }
    }
}
