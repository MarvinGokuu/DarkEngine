// Reading Order: 00011111
//  31
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * High-performance 64-bit atomic signal packer (Zero-Heap).
 *
 * <p>Provides deterministic bitwise operations to encode multiple scalar values 
 * (commands, payloads, vectors, pointers) into single 64-bit primitives. This 
 * guarantees zero object allocation on the hot path, maximizing CPU cache 
 * efficiency and eliminating garbage collection overhead.
 *
 * <p>Standard Layout: {@code [32 bits: Command ID] | [32 bits: Payload/Value]}
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date         = "2026-01-05",
    maxLatencyNs = 5,
    minThroughput = 200_000_000,
    alignment    = 0,
    lockFree     = true,
    offHeap      = false,
    notes        = "Pure static utility for binary packing — Zero allocation"
)
public final class DarkSignalPacker {

    /** Utility class — no instances. */
    private DarkSignalPacker() {
        throw new AssertionError("DarkSignalPacker is a static utility class");
    }

    /**
     * Packs a command ID and an integer value into a single 64-bit signal.
     * 
     * <p>Executes entirely in CPU registers using bitwise shifts and masks.
     * 
     * @param commandId The upper 32 bits.
     * @param value     The lower 32 bits.
     * @return The 64-bit packed signal.
     */
    public static long pack(int commandId, int value) {
        return ((long) commandId << 32) | (value & 0xFFFFFFFFL);
    }

    /**
     * Packs a system command without an additional payload.
     * 
     * <p>Optimized to skip the OR mask since the lower bits are implicitly zero.
     * 
     * @param commandId The command identifier.
     * @return The 64-bit packed signal.
     */
    public static long packCmd(int commandId) {
        return (long) commandId << 32;
    }

    /**
     * Extracts the command ID from a packed 64-bit signal.
     * 
     * @param signal The packed 64-bit signal.
     * @return The command ID (upper 32 bits).
     */
    public static int unpackCommandId(long signal) {
        // Arithmetic shift right to extract the upper 32 bits
        return (int) (signal >>> 32);
    }

    /**
     * Extracts the value/payload from a packed 64-bit signal.
     * 
     * @param signal The packed 64-bit signal.
     * @return The payload value (lower 32 bits).
     */
    public static int unpackValue(long signal) {
        // AND mask to discard upper bits and retain only the lower 32 bits
        return (int) (signal & 0xFFFFFFFFL);
    }

    // -------------------------------------------------------------------------
    // Specialized Formats (Vectors, Coordinates, GUIDs)
    // -------------------------------------------------------------------------

    /**
     * Packs two 32-bit floating point numbers into a 64-bit long.
     * 
     * <p>Format: {@code [float X: 32 bits][float Y: 32 bits]}
     * <p>Preserves IEEE 754 binary representation without loss of precision.
     * Used heavily in 2D coordinate tracking and physics calculations to maintain 
     * CPU register uniformity.
     * 
     * @param x The X coordinate (32-bit float).
     * @param y The Y coordinate (32-bit float).
     * @return The packed vector (64-bit long).
     */
    public static long packFloats(float x, float y) {
        int xBits = Float.floatToRawIntBits(x);
        int yBits = Float.floatToRawIntBits(y);
        return ((long) xBits << 32) | (yBits & 0xFFFFFFFFL);
    }

    /**
     * Extracts the X coordinate from a packed float vector.
     * 
     * @param packed The packed vector (64-bit long).
     * @return The X coordinate (32-bit float).
     */
    public static float unpackX(long packed) {
        return Float.intBitsToFloat((int) (packed >>> 32));
    }

    /**
     * Extracts the Y coordinate from a packed float vector.
     * 
     * @param packed The packed vector (64-bit long).
     * @return The Y coordinate (32-bit float).
     */
    public static float unpackY(long packed) {
        return Float.intBitsToFloat((int) (packed & 0xFFFFFFFFL));
    }

    /**
     * Packs 3D coordinates using mixed precision to optimize bandwidth.
     * 
     * <p>Format: {@code [X: 16 bits][Y: 16 bits][Z: 32 bits]}
     * <br>Used for spatial telemetry where X and Y ranges are bounded, but Z 
     * requires full integer precision.
     * 
     * @param x The X coordinate (short, -32768 to 32767).
     * @param y The Y coordinate (short, -32768 to 32767).
     * @param z The Z coordinate (int, full range).
     * @return The packed coordinates (64-bit long).
     */
    public static long packCoordinates3D(short x, short y, int z) {
        return ((long) x << 48) | ((long) y << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Extracts the X coordinate from a packed 3D coordinate signal.
     * 
     * @param packed The packed 3D coordinates.
     * @return The X coordinate (short).
     */
    public static short unpack3DX(long packed) {
        return (short) (packed >>> 48);
    }

    /**
     * Extracts the Y coordinate from a packed 3D coordinate signal.
     * 
     * @param packed The packed 3D coordinates.
     * @return The Y coordinate (short).
     */
    public static short unpack3DY(long packed) {
        return (short) (packed >>> 32);
    }

    /**
     * Extracts the Z coordinate from a packed 3D coordinate signal.
     * 
     * @param packed The packed 3D coordinates.
     * @return The Z coordinate (int).
     */
    public static int unpack3DZ(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    /**
     * Standardizes API passage for 64-bit GUIDs.
     * 
     * <p>Acts as a pass-through function to maintain uniformity across the 
     * packing API for unique entity identifiers.
     * 
     * @param guid The 64-bit unique identifier.
     * @return The unchanged GUID.
     */
    public static long packGUID(long guid) {
        return guid; 
    }

    /**
     * Standardizes API passage for off-heap memory pointers.
     * 
     * <p>WARNING: Pointers are strictly bound to the active JVM session. 
     * They must never be serialized or persisted across restarts.
     * 
     * @param memoryAddress The 64-bit memory address.
     * @return The packed pointer.
     */
    public static long packOffHeapPointer(long memoryAddress) {
        return memoryAddress;
    }

    // -------------------------------------------------------------------------
    // Atomic Flag Operations
    // -------------------------------------------------------------------------

    /**
     * Packs multiple boolean signals into a single long flag mask.
     * 
     * <p>Format: {@code [63 bits of flags][1 bit reserved]}
     * 
     * @param flags Bitmask where each bit represents a discrete signal.
     * @return The packed signals.
     */
    public static long packAtomicSignals(long flags) {
        return flags;
    }

    /**
     * Retrieves the state of a specific bit in a packed signal mask.
     * 
     * @param packed   The packed signal mask.
     * @param bitIndex The index of the bit to check (0-62).
     * @return {@code true} if the bit is 1, {@code false} otherwise.
     */
    public static boolean getSignalBit(long packed, int bitIndex) {
        return ((packed >>> bitIndex) & 1L) == 1L;
    }

    /**
     * Modifies the state of a specific bit in a packed signal mask.
     * 
     * @param packed   The original signal mask.
     * @param bitIndex The index of the bit to modify (0-62).
     * @param value    The new state (true = 1, false = 0).
     * @return The updated signal mask.
     */
    public static long setSignalBit(long packed, int bitIndex, boolean value) {
        if (value) {
            return packed | (1L << bitIndex);
        } else {
            return packed & ~(1L << bitIndex);
        }
    }

    // -------------------------------------------------------------------------
    // Hot-Path Arithmetic Operations
    // -------------------------------------------------------------------------

    /**
     * Calculates the differential between two packed 2D orbital vectors.
     * 
     * <p>Executes direct floating-point arithmetic without heap allocations, 
     * resolving directly within CPU registers.
     * 
     * @param orbit1 The primary orbit vector (packed).
     * @param orbit2 The secondary orbit vector (packed).
     * @return The differential vector (packed).
     */
    public static long computeOrbitalDifferential(long orbit1, long orbit2) {
        float x1 = unpackX(orbit1);
        float y1 = unpackY(orbit1);
        float x2 = unpackX(orbit2);
        float y2 = unpackY(orbit2);

        float dx = x1 - x2;
        float dy = y1 - y2;

        return packFloats(dx, dy);
    }

    /**
     * Scales a packed 2D float vector by a specified percentage.
     * 
     * @param flowData   The flow vector (packed).
     * @param percentage The scaling percentage (0-100).
     * @return The scaled flow vector (packed).
     */
    public static long scaleFlowPercentage(long flowData, int percentage) {
        float x = unpackX(flowData);
        float y = unpackY(flowData);

        // Mechanical Sympathy: FPU Division replaced by reciprocal multiplication
        float scale = percentage * 0.01f;
        float scaledX = x * scale;
        float scaledY = y * scale;

        return packFloats(scaledX, scaledY);
    }

    /**
     * Validates and adjusts a memory pointer to a 4KB page boundary.
     * 
     * <p>Reduces Translation Lookaside Buffer (TLB) misses by ensuring 
     * data reads align with hardware memory pages.
     * 
     * @param dataPointer The raw off-heap memory pointer.
     * @return The 4KB-aligned pointer.
     */
    public static long alignToPage4KB(long dataPointer) {
        // Mechanical Sympathy: Modulo operator replaced by Bitwise AND for power of 2
        // 4096 = 2^12. Mask = 4096 - 1 = 4095.
        long remainder = dataPointer & 4095L;

        if (remainder == 0) {
            return dataPointer;
        }

        return dataPointer + (4096L - remainder);
    }
}
