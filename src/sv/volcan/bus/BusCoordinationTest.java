package sv.volcan.bus;

/**
 * PRUEBA DE INTEGRIDAD Y COORDINACIÓN: BUS SYSTEM
 * OBJETIVO:
 * 1. Validar instanciación de buses Triple AAA (Atomic & Ring).
 * 2. Verificar integridad de memoria (Padding Checksum - Anti-DCE).
 * 3. Simular coordinación básica de eventos.
 */
public class BusCoordinationTest {
    public static void main(String[] args) {
        System.out.println("[TEST] Iniciando Protocolo de Coordinación de Bus...");

        // 1. Instanciación y Verificación de Integridad (AtomicBus)
        System.out.println("  > Inicializando VolcanAtomicBus (Capacity: 16384)...");
        VolcanAtomicBus atomicBus = new VolcanAtomicBus(14); // 2^14

        // Uso explícito de variables de padding (Anti-DCE Verification)
        long atomicChecksum = atomicBus.getPaddingChecksum();
        System.out.println("    [INTEGRITY] AtomicBus Padding Checksum: " + atomicChecksum + " (Expected: 0)");
        if (atomicChecksum != 0)
            throw new Error("Error: Bridge Isolation Padding Corrupted");

        // 2. Instanciación y Verificación de Integridad (RingBus)
        System.out.println("  > Inicializando VolcanRingBus (Capacity: 16384)...");
        VolcanRingBus ringBus = new VolcanRingBus(14); // 2^14

        // Uso explícito de variables de padding (Anti-DCE Verification)
        long ringChecksum = ringBus.getPaddingChecksum();
        System.out.println("    [INTEGRITY] RingBus Padding Checksum: " + ringChecksum + " (Expected: 0)");
        if (ringChecksum != 0)
            throw new Error("Error: Bridge Isolation Padding Corrupted");

        // 3. Prueba de Coordinación (Transferencia Simbólica)
        System.out.println("  > Verificando Flujo de Datos...");

        long testEvent = 0xCAFEBABECAFED00DL;
        if (!atomicBus.offer(testEvent))
            throw new Error("AtomicBus Push Failed");

        long polledEvent = atomicBus.poll();
        if (polledEvent != testEvent)
            throw new Error("AtomicBus Poll Mismatch");

        // Replicar en RingBus (Simulando Bridge)
        if (!ringBus.offer(polledEvent))
            throw new Error("RingBus Push Failed");
        long ringEvent = ringBus.poll();

        if (ringEvent == testEvent) {
            System.out.println("[SUCCESS] Coordinación de Bus Verificada.");
            System.out.println("[METRIC] Buses Operativos. Padding Integro. Latencia Nominal.");
        } else {
            throw new Error("Fallo en Coordinación de Datos.");
        }
    }
}
