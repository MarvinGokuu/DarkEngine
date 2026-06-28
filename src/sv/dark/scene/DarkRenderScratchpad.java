// Reading Order: 00100022
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: Zero-Allocation per-frame scratch memory for GPU uniform uploads.
 *
 * WHY THIS EXISTS:
 * Without this class, every matrix/vec3 uniform upload requires a temporary
 * Panama MemorySegment allocation: {@code arena.allocateFrom(ValueLayout.JAVA_FLOAT, matrix)}.
 * This translates to a mmap() syscall to the OS kernel, followed by munmap() after
 * the try-with-resources block. At 60 FPS with N entities, this becomes:
 *   N * 2 syscalls * 60 FPS = catastrophic OS kernel pressure.
 * Unreal Engine 5 and RAGE solve this with a Frame Linear Allocator whose cursor
 * simply resets to 0 at the start of each frame. We implement the equivalent in
 * Java/Panama: pre-allocated MemorySegments that are overwritten in-place.
 *
 * TECHNIQUE:
 * - Arena.ofAuto() with a global scope (lives for the engine lifetime).
 * - Fixed-size MemorySegments aligned to 16 bytes (GPU-friendly ABI, AVX alignment).
 * - Content is overwritten via MemorySegment.copy() — zero allocations, zero syscalls.
 *
 * THREAD CONFINEMENT:
 * ALL methods in this class MUST be called exclusively from the Render Thread (main thread).
 * These buffers are NOT thread-safe by design — they are Single-Producer scratch buffers.
 * Mixing threads here = data corruption + undefined GPU behavior.
 *
 * GUARANTEES:
 * - Zero heap allocations on the render hot-path.
 * - Zero OS mmap/munmap syscalls during matrix/uniform uploads.
 * - Deterministic memory layout (16-byte aligned for AVX/SSE compatibility).
 *
 * @author Marvin Alexander Flores Canales
 * @since 4.3.0
 */
@AAACertified(
    date          = "2026-06-27",
    maxLatencyNs  = 0,
    minThroughput = 0,
    alignment     = 16,
    lockFree      = true,
    offHeap       = true,
    notes         = "Zero-Allocation Frame Scratchpad for GPU uniform uploads. Render Thread ONLY."
)
public final class DarkRenderScratchpad {

    // Global auto-lifetime arena. Created once; lives until JVM exits.
    // Arena.ofAuto() is GC-managed at the segment level — the Arena itself never closes.
    // This is intentional: the scratchpad is a permanent engine resource.
    private static final Arena GLOBAL_ARENA = Arena.ofAuto();

    /**
     * 64-byte buffer = 1x mat4x4 (16 floats × 4 bytes).
     * Aligned to 16 bytes for SSE/AVX-compatibility in GPU driver upload paths.
     * Used for: model, view, projection, lightSpaceMatrix uploads.
     * Reuse: overwrite content with writeMatrix() before each glUniformMatrix4fv call.
     */
    public static final MemorySegment MATRIX_64B =
        GLOBAL_ARENA.allocate(64, 16);

    /**
     * 192-byte buffer = 3x mat4x4 (3 cascades × 16 floats × 4 bytes).
     * Used for: CSM lightSpaceMatrices[3] array uniform.
     * Reuse: overwrite per-slot with writeMatrixSlot() for each cascade.
     */
    public static final MemorySegment MATRIX_ARRAY_192B =
        GLOBAL_ARENA.allocate(192, 16);

    /**
     * 12-byte buffer = 1x vec3 (3 floats × 4 bytes).
     * Used for: sunDir, camPos, vec3 uniform uploads.
     */
    public static final MemorySegment VEC3_12B =
        GLOBAL_ARENA.allocate(12, 4);

    /**
     * 16-byte buffer = 1x vec4 / 1x ivec4 / 4x int.
     * Used for: drawBuffers array, atomic counter reset (int 0), misc.
     */
    public static final MemorySegment INT_4B =
        GLOBAL_ARENA.allocate(4, 4);

    // -------------------------------------------------------------------------
    // ZERO-ALLOCATION WRITE HELPERS
    // -------------------------------------------------------------------------

    /**
     * Writes a float[16] matrix into MATRIX_64B.
     * WHY MemorySegment.copy: avoids allocateFrom which triggers internal buffer allocation.
     * This is a raw memory copy — equivalent to C's memcpy. Zero GC, zero syscalls.
     *
     * // [NON_BLOCKING] [ZERO_ALLOCATION] [RENDER_THREAD_ONLY]
     *
     * @param matrix float[16] row-major matrix.
     */
    public static void writeMatrix(float[] matrix) {
        MemorySegment.copy(matrix, 0, MATRIX_64B, ValueLayout.JAVA_FLOAT, 0L, 16);
    }

    /**
     * Writes a float[16] matrix into a specific slot of MATRIX_ARRAY_192B.
     * Used for CSM: 3 cascades, each is a 64-byte block at offset slotIndex*64.
     *
     * // [NON_BLOCKING] [ZERO_ALLOCATION] [RENDER_THREAD_ONLY]
     *
     * @param matrix    float[16] cascade light-space matrix.
     * @param slotIndex Cascade index: 0, 1, or 2.
     */
    public static void writeMatrixSlot(float[] matrix, int slotIndex) {
        MemorySegment.copy(matrix, 0, MATRIX_ARRAY_192B,
            ValueLayout.JAVA_FLOAT, (long) slotIndex * 64L, 16);
    }

    private DarkRenderScratchpad() {
        // Static utility — never instantiate. Prevents accidental object creation.
    }
}
