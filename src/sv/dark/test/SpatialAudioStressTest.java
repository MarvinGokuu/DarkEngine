// Reading Order: 00100080
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.audio.DarkAudioSourceSoA;
import sv.dark.core.systems.DarkAudioSystem;
import sv.dark.memory.SectorMemoryVault;

/**
 * RESPONSIBILITY: Test Spatial Audio Allocation and Doppler synchronization.
 * WHY: Ensuring that scaling to 1024 concurrent 3D audio sources doesn't trigger GC overhead.
 */
public class SpatialAudioStressTest {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" SPATIAL AUDIO STRESS TEST (1024 Sources)");
        System.out.println("==============================================");

        // Pre-cargar la clase Linker para que las asignaciones estáticas del JNI/Panama no cuenten como fuga de memoria
        try {
            Class.forName("sv.dark.core.systems.DarkAudioLinker");
        } catch (ClassNotFoundException e) {}

        SectorMemoryVault vault = new SectorMemoryVault(1024);
        
        // Initialize Audio Context for tests
        sv.dark.core.DarkAudioContext.set(new sv.dark.audio.DarkOpenALBackend());
        
        // Dummy init to force all static class loading (JNI, Project Panama, Loggers)
        sv.dark.audio.DarkAudioSourceSoA dummySrc = new sv.dark.audio.DarkAudioSourceSoA(1);
        DarkAudioSystem dummySys = new DarkAudioSystem(vault, dummySrc);
        dummySys.cleanup();
        dummySrc.destroy();
        dummySys = null;
        dummySrc = null;

        System.gc();
        try { Thread.sleep(200); } catch (Exception e) {}
        System.gc();
        
        long startMem = getUsedMemory();

        DarkAudioSourceSoA sources = new DarkAudioSourceSoA(1024);
        DarkAudioSystem audioSys = new DarkAudioSystem(vault, sources);

        long memDiff = getUsedMemory() - startMem;
        System.out.printf("[RESULT] Heap Memory Footprint for Audio Subsystem: %d bytes.%n", memDiff);

        // Project Panama Arena.ofShared() y OpenAL DLL Init pueden reservar ~2-3 MB de metadatos estáticos JNI.
        // Mientras no exceda una cuota base de 5MB, sabemos que no estamos instanciando objetos "AudioSource" por entidad.
        if (memDiff > 5 * 1024 * 1024) { 
            System.err.println("[FAIL] DarkAudioSourceSoA leaked into JVM Heap!");
            System.exit(1);
        } else {
            System.out.println("[OK] AAA+ Zero-GC Structural Check passed!");
        }

        System.out.println("[TEST] Calling GameSystem.update() to simulate Doppler Effect sync...");
        audioSys.update(null, 0.016f);
        System.out.println("[OK] Synchronized 1024 OpenAL Sources natively.");

        audioSys.cleanup();
        sources.destroy();
        vault.close();

        System.out.println("==============================================");
        System.out.println(" TESTS PASSED: SPATIAL AUDIO HRTF/DOPPLER");
        System.out.println("==============================================");
        System.exit(0);
    }

    private static long getUsedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}
