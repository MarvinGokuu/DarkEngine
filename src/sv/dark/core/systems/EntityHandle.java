package sv.dark.core.systems;

/**
 * Valhalla-Ready Entity Handle.
 * In Java 26+, this will be marked as a `value class`.
 * It provides an OOP facade over a pure DOD array index, ensuring Zero-GC
 * when Valhalla flattens this in memory.
 */
public record EntityHandle(int id) {
    // A handle only contains the index to the parallel arrays (SoA).
    // It has no logic or state of its own.
}
