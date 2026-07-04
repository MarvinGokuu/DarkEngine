// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.state;

import sv.dark.admin.AdminController;
import sv.dark.bus.DarkEventDispatcher;
import sv.dark.config.DarkEngineConfig;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.kernel.EngineKernel;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.ui.DarkEngineWindow;
import sv.dark.core.systems.DarkInputSystem;
import sv.dark.core.systems.DarkAudioSystem;

/**
 * RESPONSIBILITY: Global Runtime Orchestration and Entry Point.
 * WHY: The engine requires a strict chronological bootstrap sequence to initialize native memory, the event bus, the visual window, and the background kernel thread safely.
 * TECHNIQUE: Single entry point (Main). Redirects I/O to async loggers, initializes off-heap memory vaults, configures multi-lane architecture, and pins the Kernel to a MAX_PRIORITY background thread.
 * GUARANTEES: Clean and predictable bootstrap sequence, isolating the kernel hot-path from AWT/OS thread interruptions.
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(date = "2026-01-08", maxLatencyNs = 200_000_000, notes = "Engine Bootstrapper - Infrastructure Orchestrator")
public final class DarkEngineMaster {

    public static void main(String[] args) throws java.io.IOException {

        // -------------------------------------------------------------------------
        // (STEP 0 Eliminado: No se intercepta stdout/stderr. Se delega todo a DarkLogger)
        // -------------------------------------------------------------------------

        // -------------------------------------------------------------------------
        // STEP 1: (REMOVED) Visual Window is now initialized inside EngineKernel.start()
        // -------------------------------------------------------------------------

        // -------------------------------------------------------------------------
        // STEP 2: Original Init (unchanged)
        // -------------------------------------------------------------------------
        // Force DarkEngineConfig class loading to trigger static block
        // initialization.
        // The static block prints the configuration banner and loads all config
        // settings.
        // Variable appears unused but its purpose is the side effect of class loading.
        @SuppressWarnings("unused")
        String profile = DarkEngineConfig.getProfile();

        DarkLogger.info("ENGINE", "DarkEngine v2.0");
        DarkLogger.info("ENGINE", "=================");

        // [NEURONA_048 STEP 1] SECTOR MEMORY VAULT (Off-Heap Memory)
        SectorMemoryVault memoryVault = new SectorMemoryVault(1024);

        // [NEURONA_048 STEP 2] EVENT DISPATCHER (Multi-Lane Bus)
        DarkEventDispatcher dispatcher = DarkEventDispatcher.createDefault(14);

        // [NEURONA_048 STEP 3] MAIN KERNEL (Central Processor)
        DarkLogger.info("ENGINE", "Starting kernel...");
        EngineKernel kernel = new EngineKernel(dispatcher, memoryVault);

        // [NEURONA_048 STEP 4] ADMIN CONTROLLER (Control Plane)
        // Start the Control Plane (HTTP Server + Admin Consumer)
        // This DOES NOT block the hot-path, it runs in separate threads
        AdminController.startControlPlane(kernel, memoryVault);

        // [NEURONA_048 STEP 4.5] INITIALIZE NATIVE CONTEXTS
        try {
            sv.dark.core.DarkPlatformContext.get();
        } catch (IllegalStateException e) {
            sv.dark.core.DarkPlatformContext.set(new sv.dark.platform.DarkGLFWBackend());
        }
        try {
            sv.dark.core.DarkAudioContext.get();
        } catch (IllegalStateException e) {
            sv.dark.core.DarkAudioContext.set(new sv.dark.audio.DarkOpenALBackend());
        }
        try {
            sv.dark.core.DarkUIContext.get();
        } catch (IllegalStateException e) {
            sv.dark.core.DarkUIContext.set(new sv.dark.ui.DarkImGuiBackend());
        }

        // [NEURONA_048 STEP 5] CONFIGURE SYSTEMS
        configureSystems(kernel, memoryVault);

        // -------------------------------------------------------------------------
        // STEP 3: Execute Kernel on Main Thread (MAX_PRIORITY)
        // -------------------------------------------------------------------------
        // By running the Kernel on the Main Thread, we guarantee that GLFW operates
        // directly on the primary OS context (required by macOS and Wayland).
        // The EngineKernel will internally apply Spatial Slicing (Pinning to Core 1)
        // and take absolute control of the OS without yielding.
        Thread.currentThread().setName("dark-engine-kernel");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        try {
            kernel.start();
        } catch (Throwable t) {
            DarkLogger.fatal("MASTER", "Fatal Kernel Error", t);
        }
    }

    private static void configureSystems(EngineKernel kernel, SectorMemoryVault memoryVault) {
        DarkLogger.info("ENGINE", "Configuring User Systems...");
        var registry = kernel.getSystemRegistry();

        // Register Test Systems for Parallel Execution Validation
        registry.registerGameSystem(new sv.dark.test.SystemExecutionTest());
        registry.registerGameSystem(new sv.dark.test.SystemDependencyTest());
        registry.registerGameSystem(new sv.dark.test.SystemParallelismTest());
        
        // Register Core Phase 2 Systems
        registry.registerGameSystem(new DarkInputSystem(memoryVault));
        
        sv.dark.audio.DarkAudioSourceSoA audioSources = new sv.dark.audio.DarkAudioSourceSoA(1024);
        registry.registerGameSystem(new DarkAudioSystem(memoryVault, audioSources));

        // [ECS PHASE 30] Register High-Level Scene Kinematics (Runs in parallel via Graph)
        registry.registerGameSystem(new sv.dark.ecs.SceneKinematicsSystem(kernel.getScene()));
        
        // 5. Physics Broadphase Culling (Data-Oriented Spatial Hash)
        sv.dark.physics.BroadphaseSystem broadphase = new sv.dark.physics.BroadphaseSystem(kernel.getScene());
        registry.registerGameSystem(broadphase);

        // 6. Physics Narrowphase Solver (Circle/AABB Fast-Paths + Rigidbody Dynamics)
        sv.dark.physics.DarkColliderSoA colliderMemory = new sv.dark.physics.DarkColliderSoA(kernel.getScene().getSoA().getCapacity());
        registry.registerGameSystem(new sv.dark.physics.NarrowphaseSystem(kernel.getScene(), broadphase.getGrid(), colliderMemory));

        // 7. GPU Particle System (Phase 32)
        sv.dark.vfx.DarkParticleEmitterSoA emitterMemory = new sv.dark.vfx.DarkParticleEmitterSoA(512); // Soporta hasta 512 Spawners (que emiten N millones de particulas en GPU)
        registry.registerGameSystem(new sv.dark.vfx.GPUParticleSystem(emitterMemory));

        // 8. Skeletal Animation System (Phase 32.2)
        // Soporta 10,000 entidades con esqueletos de 64 huesos cada uno (Total VRAM: ~40 MB para matrices)
        sv.dark.vfx.animation.DarkSkeletonSoA skeletonMemory = new sv.dark.vfx.animation.DarkSkeletonSoA(10000, 64);
        registry.registerGameSystem(new sv.dark.vfx.animation.SkeletalAnimationSystem(skeletonMemory));

        // 9. Networking & State Replication (Phase 33)
        // Cliente UDP en el puerto 27020, con un buffer RX/TX de 64KB (Zero-Allocation)
        sv.dark.net.DarkNetworkClient netClient = new sv.dark.net.DarkNetworkClient(27020, 65536);
        // Simulando una conexion entrante
        netClient.connect(new java.net.InetSocketAddress("127.0.0.1", 27016));
        sv.dark.net.DarkNetworkClient[] netClients = new sv.dark.net.DarkNetworkClient[]{ netClient };
        registry.registerGameSystem(new sv.dark.net.NetworkReplicationSystem(netClients, kernel.getScene()));

        // [FASE 4] Activa el DAG Mode: dispatch elástico sin barreras de layer.
        // Cada sistema se despacha individualmente en cuanto sus dependencias atómicas
        // se satisfacen — 100% CPU core utilization, sin idle time entre layers.
        registry.buildDependencyGraph();
        registry.enableDAGMode();
    }
}
