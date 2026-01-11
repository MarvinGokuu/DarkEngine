package sv.volcan.test;

import sv.volcan.kernel.UltraFastBootSequence;
import sv.volcan.kernel.UltraFastBootSequence.BootResult;
import sv.volcan.kernel.KernelControlRegister;
import sv.volcan.memory.SectorMemoryVault;
import sv.volcan.bus.VolcanAtomicBus;

import sv.volcan.core.AAACertified;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Validar que el boot completo ocurra en < 1ms (AAA+
 * Standard).
 * MÉTRICAS: Pass/Fail basado en latencia hard-real-time.
 * 
 * @author Marvin-Dev
 * @version 1.0 (Sovereign Validator)
 * @since 2026-01-11
 */
@AAACertified(date = "2026-01-11", maxLatencyNs = 1_000_000, minThroughput = 1, alignment = 0, lockFree = true, offHeap = true, notes = "Critical Boot Validator (Hard Real-Time)")
public class Test_UltraFastBoot {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  AAA+ CERTIFICATION: ULTRA FAST BOOT (<1ms)");
        System.out.println("═══════════════════════════════════════════════════════");

        try {
            // 1. Setup Infrastructure (Pre-Boot overhead not counted in sequence)
            System.out.println("[TEST] Initializing Hardware Simulation...");

            KernelControlRegister controlRegister = new KernelControlRegister();
            controlRegister.transition(KernelControlRegister.STATE_OFFLINE, KernelControlRegister.STATE_BOOTING);

            SectorMemoryVault memoryVault = new SectorMemoryVault(1024); // 64MB
            VolcanAtomicBus systemBus = new VolcanAtomicBus(1024);
            VolcanAtomicBus inputBus = new VolcanAtomicBus(1024);

            // 2. WARMUP (JIT Compilation)
            System.out.println("[TEST] Warming up JVM (C2 Compiler)...");
            for (int i = 0; i < 10000; i++) {
                runDummyBoot();
            }

            // 3. REAL TEST
            System.gc();
            Thread.sleep(100); // Quiescence

            System.out.println("[TEST] EXECUTING CRITICAL BOOT SEQUENCE...");

            // Execute
            BootResult result = UltraFastBootSequence.execute(
                    controlRegister,
                    memoryVault,
                    systemBus,
                    inputBus);

            // 4. Validate
            UltraFastBootSequence.printBootStats(result);

            if (result.success && result.bootTimeNs < 1_000_000) {
                System.out.println("\n[PASSED] SYSTEM IS AAA+ COMPLIANT");
                System.exit(0);
            } else {
                System.err.println("\n[FAILED] SYSTEM TOO SLOW OR BROKEN");
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Real JIT Warmup: Exercises the actual code paths
    private static void runDummyBoot() {
        // Create throwaway components for warmup
        KernelControlRegister dummyReg = new KernelControlRegister();
        dummyReg.transition(KernelControlRegister.STATE_OFFLINE, KernelControlRegister.STATE_BOOTING);

        // Use a small specialized vault for warmup to avoid OOM
        // (We just need to exercise the code paths, not allocate 2GB)
        SectorMemoryVault dummyVault = new SectorMemoryVault(128);

        VolcanAtomicBus dummyBus = new VolcanAtomicBus(128);

        // Execute the actual method we want to measure
        UltraFastBootSequence.execute(dummyReg, dummyVault, dummyBus);

        // Clean up native memory immediately
        dummyVault.close();
    }
}
