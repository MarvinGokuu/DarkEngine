package sv.dark.kernel;

/**
 * TECHNICAL SPECIFICATION
 *
 * CONTEXT:
 * - immutable snapshot structure capturing the hardware and OS-level execution state.
 *
 * MEMORY SEMANTICS:
 * - Thread-confined heap properties. Snapshot variables are final and immutable.
 */
public final class SystemSnapshot {

    public final long threadAffinityMask;
    public final String powerSchemeGuid;
    public final String powerSchemeName;
    public final String powerSource;
    public final long timestamp;

    public SystemSnapshot(long threadAffinityMask, String powerSchemeGuid, String powerSchemeName, String powerSource) {
        this.threadAffinityMask = threadAffinityMask;
        this.powerSchemeGuid = powerSchemeGuid;
        this.powerSchemeName = powerSchemeName;
        this.powerSource = powerSource;
        this.timestamp = System.nanoTime();
    }

    public void print() {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("SYSTEM STATE SNAPSHOT");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.printf("  Thread Affinity Mask: 0x%X%n", threadAffinityMask);
        System.out.printf("  Power Scheme GUID:    %s%n", powerSchemeGuid);
        System.out.printf("  Power Scheme Name:    %s%n", powerSchemeName);
        System.out.printf("  Power Source:         %s%n", powerSource);
        System.out.printf("  Timestamp:            %,d ns%n", timestamp);
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
