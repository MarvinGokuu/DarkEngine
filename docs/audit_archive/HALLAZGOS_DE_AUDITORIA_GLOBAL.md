# 🌐 HALLAZGOS DE AUDITORÍA GLOBAL (100% COMPLETADO)

**Estado de Auditoría**: COMPLETADO 🟢
**Metodología**: Escaneo Línea por Línea, ordenado estrictamente por `Reading Order` para rastreo de dependencias.

--- 

### [Reading Order: 00000000] Archivo: `sv/dark/kernel/BaselineValidator.java`
- **Línea 80**: Bloqueo I/O (System.out) -> `System.out.println("════════════════════════════════════════...`
- **Línea 81**: Zero-GC (String Concat) -> `System.out.println("BASELINE SNAPSHOT: " + label);`
- **Línea 81**: Bloqueo I/O (System.out) -> `System.out.println("BASELINE SNAPSHOT: " + label);`
- **Línea 82**: Bloqueo I/O (System.out) -> `System.out.println("════════════════════════════════════════...`
- **Línea 83**: Bloqueo I/O (System.out) -> `System.out.println(String.format("  Heap Used:      %,d byte...`
- **Línea 83**: Aritmetica Modulo (Potencial) -> `System.out.println(String.format("  Heap Used:      %,d byte...`
- **Línea 84**: Bloqueo I/O (System.out) -> `System.out.printf("  Heap Committed: %,d bytes (%.2f MB)%n",...`
- **Línea 84**: Aritmetica Modulo (Potencial) -> `System.out.printf("  Heap Committed: %,d bytes (%.2f MB)%n",...`
- **Línea 86**: Bloqueo I/O (System.out) -> `System.out.println(String.format("  Heap Max:       %,d byte...`
- **Línea 86**: Aritmetica Modulo (Potencial) -> `System.out.println(String.format("  Heap Max:       %,d byte...`
- **Línea 87**: Bloqueo I/O (System.out) -> `System.out.printf("  Non-Heap Used:  %,d bytes (%.2f MB)%n",...`
- **Línea 87**: Aritmetica Modulo (Potencial) -> `System.out.printf("  Non-Heap Used:  %,d bytes (%.2f MB)%n",...`
- **Línea 89**: Bloqueo I/O (System.out) -> `System.out.println(String.format("  Thread Count:   %d%n", t...`
- **Línea 89**: Aritmetica Modulo (Potencial) -> `System.out.println(String.format("  Thread Count:   %d%n", t...`
- **Línea 90**: Bloqueo I/O (System.out) -> `System.out.println(String.format("  Timestamp:      %,d ns%n...`
- **Línea 90**: Aritmetica Modulo (Potencial) -> `System.out.println(String.format("  Timestamp:      %,d ns%n...`
- **Línea 91**: Bloqueo I/O (System.out) -> `System.out.println("════════════════════════════════════════...`
- **Línea 156**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] Reclaiming deep memory (Triple ...`
- **Línea 161**: Bloqueo I/O (System.out) -> `System.out.println("DONE.");`
- **Línea 175**: Zero-GC (New Object) -> `return new MemorySnapshot("State A (Pre-Boot)");`
- **Línea 184**: Zero-GC (New Object) -> `return new MemorySnapshot("State B (During Execution)");`
- **Línea 196**: Zero-GC (New Object) -> `return new MemorySnapshot("State C (Post-Shutdown)");`

### [Reading Order: 00000001] Archivo: `sv/dark/state/DarkStateLayout.java`
- **Línea 40**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("DarkStateLayout is a static utilit...`

### [Reading Order: 00000010] Archivo: `sv/dark/bus/DarkSignalCommands.java`
- **Línea 32**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("DarkSignalCommands is a static uti...`

### [Reading Order: 00000101] Archivo: `sv/dark/bus/DarkSignalPacker.java`
- **Línea 35**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("DarkSignalPacker is a static utili...`
- **Línea 101**: FPU Overhead (Float/Double) -> `public static long packFloats(float x, float y) {`
- **Línea 113**: FPU Overhead (Float/Double) -> `public static float unpackX(long packed) {`
- **Línea 123**: FPU Overhead (Float/Double) -> `public static float unpackY(long packed) {`
- **Línea 257**: FPU Overhead (Float/Double) -> `float x1 = unpackX(orbit1);`
- **Línea 258**: FPU Overhead (Float/Double) -> `float y1 = unpackY(orbit1);`
- **Línea 259**: FPU Overhead (Float/Double) -> `float x2 = unpackX(orbit2);`
- **Línea 260**: FPU Overhead (Float/Double) -> `float y2 = unpackY(orbit2);`
- **Línea 262**: FPU Overhead (Float/Double) -> `float dx = x1 - x2;`
- **Línea 263**: FPU Overhead (Float/Double) -> `float dy = y1 - y2;`
- **Línea 276**: FPU Overhead (Float/Double) -> `float x = unpackX(flowData);`
- **Línea 277**: FPU Overhead (Float/Double) -> `float y = unpackY(flowData);`
- **Línea 280**: FPU Overhead (Float/Double) -> `float scale = percentage * 0.01f;`
- **Línea 281**: FPU Overhead (Float/Double) -> `float scaledX = x * scale;`
- **Línea 282**: FPU Overhead (Float/Double) -> `float scaledY = y * scale;`

### [Reading Order: 00000110] Archivo: `sv/dark/bus/DarkAtomicBus.java`
- **Línea 137**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("Critical failure in Dark Atomic Bus: Could ...`
- **Línea 160**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("DarkAtomicBus: Memory signature corrupted -...`
- **Línea 479**: Bloqueo I/O (System.out) -> `System.out.println("[ATOMIC BUS] Injecting Tombstone Event.....`
- **Línea 494**: Bloqueo I/O (System.out) -> `System.out.println("[ATOMIC BUS] Clearing buffer...");`
- **Línea 497**: Bloqueo I/O (System.out) -> `System.out.println("[ATOMIC BUS] Validating memory integrity...`
- **Línea 502**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("DarkAtomicBus: Shutdown failed - Pending ev...`
- **Línea 506**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("DarkAtomicBus: Memory signature corrupted d...`
- **Línea 509**: Bloqueo I/O (System.out) -> `System.out.println("[ATOMIC BUS] Shutdown completed - 100% I...`
- **Línea 509**: Aritmetica Modulo (Potencial) -> `System.out.println("[ATOMIC BUS] Shutdown completed - 100% I...`

### [Reading Order: 00000111] Archivo: `sv/dark/bus/DarkRingBus.java`
- **Línea 115**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("Critical failure in Dark Ring Bus: Could no...`
- **Línea 139**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("DarkRingBus: Padding corruption detected at...`

### [Reading Order: 00001000] Archivo: `sv/dark/bus/DarkEventLane.java`
- **Línea 207**: FPU Overhead (Float/Double) -> `public double getAcceptanceRate() {`
- **Línea 210**: FPU Overhead (Float/Double) -> `return (double) totalAccepted / offered;`
- **Línea 218**: FPU Overhead (Float/Double) -> `public double getDropRate() {`
- **Línea 221**: FPU Overhead (Float/Double) -> `return (double) totalDropped / offered;`
- **Línea 260**: Aritmetica Modulo (Potencial) -> `"[LANE: %s] Type=%s | Size=%d/%d | Offered=%d | Accepted=%d ...`

### [Reading Order: 00001000] Archivo: `sv/dark/bus/DarkSignalDispatcher.java`
- **Línea 34**: Zero-GC (New Object) -> `this.bus = new DarkAtomicBus(BUS_SIZE_POWER);`
- **Línea 183**: FPU Overhead (Float/Double) -> `public boolean dispatchVector2D(float x, float y) {`

### [Reading Order: 00001001] Archivo: `sv/dark/bus/DarkEventDispatcher.java`
- **Línea 61**: Zero-GC (New Object) -> `DarkEventDispatcher dispatcher = new DarkEventDispatcher();`
- **Línea 66**: Zero-GC (New Object) -> `new DarkRingBus(busSize),`
- **Línea 72**: Zero-GC (New Object) -> `new DarkRingBus(busSize),`
- **Línea 78**: Zero-GC (New Object) -> `new DarkRingBus(busSize),`
- **Línea 84**: Zero-GC (New Object) -> `new DarkRingBus(busSize),`
- **Línea 90**: Zero-GC (New Object) -> `new DarkRingBus(busSize),`
- **Línea 96**: Zero-GC (New Object) -> `new DarkRingBus(busSize),`
- **Línea 114**: Zero-GC (New Object) -> `DarkEventLane lane = new DarkEventLane(type.name(), type, bu...`
- **Línea 230**: Bloqueo I/O (System.out) -> `System.out.println("════════════════════════════════════════...`
- **Línea 231**: Bloqueo I/O (System.out) -> `System.out.println("  DARK EVENT DISPATCHER - STATUS REPORT"...`
- **Línea 232**: Bloqueo I/O (System.out) -> `System.out.println("════════════════════════════════════════...`
- **Línea 237**: Bloqueo I/O (System.out) -> `System.out.println(lane.getStatusReport());`
- **Línea 241**: Bloqueo I/O (System.out) -> `System.out.println("════════════════════════════════════════...`
- **Línea 285**: Bloqueo I/O (System.out) -> `System.out.println("[EVENT DISPATCHER] Initiating shutdown f...`
- **Línea 289**: Bloqueo I/O (System.out) -> `System.err.printf("[EVENT DISPATCHER] WARNING: %d pending ev...`
- **Línea 289**: Aritmetica Modulo (Potencial) -> `System.err.printf("[EVENT DISPATCHER] WARNING: %d pending ev...`
- **Línea 312**: Bloqueo I/O (System.out) -> `System.out.println("[EVENT DISPATCHER] Shutdown completed");`

### [Reading Order: 00001001] Archivo: `sv/dark/kernel/KernelControlRegister.java`
- **Línea 75**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("CRITICAL: Failed to link KernelControlRegis...`
- **Línea 82**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("KernelControlRegister: Padding Corruption D...`

### [Reading Order: 00001010] Archivo: `sv/dark/kernel/SystemRegistry.java`
- **Línea 13**: AWT/Swing (Renderizado Lento) -> `import java.awt.Graphics2D;`
- **Línea 86**: Zero-GC (String Concat) -> `DarkLogger.info("REGISTRY", "Registered game system: " + sys...`
- **Línea 98**: Zero-GC (String Concat) -> `DarkLogger.info("REGISTRY", "Registered render system: " + s...`
- **Línea 114**: FPU Overhead (Float/Double) -> `public void executeGameSystems(WorldStateFrame state, double...`
- **Línea 174**: FPU Overhead (Float/Double) -> `public double getLastExecutionTimeMs() {`
- **Línea 206**: Zero-GC (New Object) -> `dependencyGraph = new SystemDependencyGraph();`
- **Línea 220**: Zero-GC (New Object) -> `parallelExecutor = new ParallelSystemExecutor(dependencyGrap...`
- **Línea 223**: Zero-GC (String Concat) -> `DarkLogger.info("REGISTRY", "Parallel execution ready (" + d...`
- **Línea 225**: Zero-GC (String Concat) -> `DarkLogger.error("REGISTRY", "Failed to build dependency gra...`
- **Línea 243**: Zero-GC (String Concat) -> `DarkLogger.info("REGISTRY", "Parallel mode: " + (enabled ? "...`

### [Reading Order: 00001010] Archivo: `sv/dark/state/DarkStateVault.java`
- **Línea 91**: Aritmetica Modulo (Potencial) -> `if (slotIndex % 2 != 0) {`
- **Línea 92**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException(`
- **Línea 93**: Zero-GC (String Concat) -> `"slotIndex must be even to read long (8 bytes). " +`
- **Línea 94**: Zero-GC (String Concat) -> `"Got slotIndex=" + slotIndex + ". " +`
- **Línea 117**: Aritmetica Modulo (Potencial) -> `if (slotIndex % 2 != 0) {`
- **Línea 118**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException(`
- **Línea 119**: Zero-GC (String Concat) -> `"slotIndex must be even to write long (8 bytes). " +`
- **Línea 120**: Zero-GC (String Concat) -> `"Got slotIndex=" + slotIndex + ". " +`

### [Reading Order: 00001011] Archivo: `sv/dark/kernel/EngineKernel.java`
- **Línea 90**: Zero-GC (New Object) -> `this.systemRegistry = new SystemRegistry();`
- **Línea 91**: Zero-GC (New Object) -> `this.timeKeeper = new TimeKeeper();`
- **Línea 98**: Zero-GC (New Object) -> `this.stateVault = new DarkStateVault(stateArena, DarkStateLa...`
- **Línea 108**: Zero-GC (New Object) -> `this.controlRegister = new KernelControlRegister();`
- **Línea 119**: Zero-GC (New Object) -> `this.currentState = new WorldStateFrame(frameArena, stateVau...`
- **Línea 123**: Zero-GC (New Object) -> `this.adminMetricsBus = new DarkAtomicBus(1024);`
- **Línea 144**: Zero-GC (New Object) -> `this.shutdownHook = new Thread(() -> {`
- **Línea 145**: Bloqueo I/O (System.out) -> `System.out.println(">>> INITIATING GRACEFUL SHUTDOWN SEQUENC...`
- **Línea 147**: Bloqueo I/O (System.out) -> `System.out.println(">>> DARK ENGINE OFFLINE. GRAPHICS RELEAS...`
- **Línea 160**: Zero-GC (New Object) -> `this(DarkEventDispatcher.createDefault(14), new SectorMemory...`
- **Línea 182**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] STARTUP SEQUENCE START");`
- **Línea 189**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] INTEGRITY CHECK PASSED");`
- **Línea 194**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] EXECUTING JIT WARM-UP...");`
- **Línea 198**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] INITIALIZING NATIVE OS WINDOW (...`
- **Línea 204**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] EXECUTING BOOT SEQUENCE...");`
- **Línea 214**: Zero-GC (String Concat) -> `System.err.println("[KERNEL PANIC] BOOT FAILED: " + bootResu...`
- **Línea 214**: Bloqueo I/O (System.out) -> `System.err.println("[KERNEL PANIC] BOOT FAILED: " + bootResu...`
- **Línea 357**: Aritmetica Modulo (Potencial) -> `if (totalFrames % 60 == 0) {`
- **Línea 392**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] Loop terminated");`
- **Línea 447**: Zero-GC (String Concat) -> `System.out.println("[KERNEL] Pause State: " + this.paused);`
- **Línea 447**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] Pause State: " + this.paused);`
- **Línea 452**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] Rollback / Time Travel Executed...`
- **Línea 480**: FPU Overhead (Float/Double) -> `double deltaTime = timeKeeper.getDeltaTime();`
- **Línea 506**: Bloqueo I/O (System.out) -> `System.err.println("[KERNEL PANIC] Entity count corrupted!")...`
- **Línea 513**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] SHUTDOWN SEQUENCE");`
- **Línea 547**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] Shutdown already in progress, i...`
- **Línea 553**: Zero-GC (New Object) -> `Thread terminator = new Thread(() -> {`
- **Línea 564**: Zero-GC (String Concat) -> `System.err.println("[KERNEL] Error stopping Control Plane: "...`
- **Línea 564**: Bloqueo I/O (System.out) -> `System.err.println("[KERNEL] Error stopping Control Plane: "...`
- **Línea 567**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 568**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] GRACEFUL SHUTDOWN SEQUENCE");`
- **Línea 569**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 574**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 1/6] Stopping main loop...");`
- **Línea 583**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 1/6] Main loop stopped [OK]");`
- **Línea 588**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 2/6] Closing Event Dispatcher...")...`
- **Línea 591**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 2/6] Event Dispatcher closed [OK]"...`
- **Línea 593**: Zero-GC (String Concat) -> `System.err.println("[STEP 2/6] Error closing Event Dispatche...`
- **Línea 593**: Bloqueo I/O (System.out) -> `System.err.println("[STEP 2/6] Error closing Event Dispatche...`
- **Línea 599**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 3/6] Closing Admin Metrics Bus..."...`
- **Línea 602**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 3/6] Admin Metrics Bus closed [OK]...`
- **Línea 604**: Zero-GC (String Concat) -> `System.err.println("[STEP 3/6] Error closing Admin Metrics B...`
- **Línea 604**: Bloqueo I/O (System.out) -> `System.err.println("[STEP 3/6] Error closing Admin Metrics B...`
- **Línea 610**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 4/6] Closing Frame Arena...");`
- **Línea 613**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 4/6] Frame Arena closed [OK]");`
- **Línea 615**: Zero-GC (String Concat) -> `System.err.println("[STEP 4/6] Error closing Frame Arena: " ...`
- **Línea 615**: Bloqueo I/O (System.out) -> `System.err.println("[STEP 4/6] Error closing Frame Arena: " ...`
- **Línea 621**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 5/6] Closing State Vault...");`
- **Línea 624**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 5/6] State Vault Arena closed [OK]...`
- **Línea 626**: Zero-GC (String Concat) -> `System.err.println("[STEP 5/6] Error closing State Vault: " ...`
- **Línea 626**: Bloqueo I/O (System.out) -> `System.err.println("[STEP 5/6] Error closing State Vault: " ...`
- **Línea 632**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 6/7] Closing Sector Vault...");`
- **Línea 635**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 6/7] Sector Vault closed [OK]");`
- **Línea 637**: Zero-GC (String Concat) -> `System.err.println("[STEP 6/7] Error closing Sector Vault: "...`
- **Línea 637**: Bloqueo I/O (System.out) -> `System.err.println("[STEP 6/7] Error closing Sector Vault: "...`
- **Línea 643**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 7/7] Terminating FFI Native Graphi...`
- **Línea 646**: Bloqueo I/O (System.out) -> `System.out.println("[STEP 7/7] Native Graphics terminated [O...`
- **Línea 648**: Zero-GC (String Concat) -> `System.err.println("[STEP 7/7] Error terminating GLFW: " + e...`
- **Línea 648**: Bloqueo I/O (System.out) -> `System.err.println("[STEP 7/7] Error terminating GLFW: " + e...`
- **Línea 651**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 652**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] GRACEFUL SHUTDOWN COMPLETED");`
- **Línea 653**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 666**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] EXECUTING LOW-LEVEL SHUTDOWN (H...`
- **Línea 670**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] HALT bypassed for test executio...`
- **Línea 676**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] Shutdown Hook removed (CLEANUP"...`
- **Línea 680**: Bloqueo I/O (System.out) -> `System.err.println("[KERNEL] Warning: Could not remove Shutd...`

### [Reading Order: 00001100] Archivo: `sv/dark/kernel/TimeKeeper.java`
- **Línea 121**: FPU Overhead (Float/Double) -> `public double getDeltaTime() {`
- **Línea 153**: Aritmetica Modulo (Potencial) -> `bufferIndex = (bufferIndex + 1) % BUFFER_SIZE;`
- **Línea 265**: Zero-GC (String Concat) -> `DarkLogger.info("TIME", "Governor shifted to: " + fps + " FP...`
- **Línea 277**: FPU Overhead (Float/Double) -> `public double getLastFrameTimeMs() {`
- **Línea 288**: Aritmetica Modulo (Potencial) -> `DarkLogger.info("TIME", String.format("Mode: %s | FPS Target...`
- **Línea 295**: Zero-GC (String Concat) -> `DarkLogger.warning("TIME", "⚠️ WARNING: Frame exceeded budge...`

### [Reading Order: 00001100] Archivo: `sv/dark/kernel/UltraFastBootSequence.java`
- **Línea 64**: Zero-GC (New Object) -> `return new BootResult(true, bootTimeNs, null);`
- **Línea 68**: Zero-GC (New Object) -> `return new BootResult(false, bootTimeNs, error);`
- **Línea 74**: Aritmetica Modulo (Potencial) -> `return String.format("BOOT SUCCESS [%,d ns = %.3f ms]",`
- **Línea 77**: Aritmetica Modulo (Potencial) -> `return String.format("BOOT FAILURE [%s] [%,d ns]",`
- **Línea 111**: Zero-GC (New Object) -> `KernelControlRegister testRegister = new KernelControlRegist...`
- **Línea 112**: Zero-GC (New Object) -> `SectorMemoryVault testVault = new SectorMemoryVault(1);`
- **Línea 113**: Zero-GC (New Object) -> `DarkAtomicBus testBus = new DarkAtomicBus(10);`
- **Línea 145**: Zero-GC (String Concat) -> `DarkLogger.info("WARM-UP", "Total time: " + warmUpTimeMs + "...`
- **Línea 146**: Zero-GC (String Concat) -> `DarkLogger.info("WARM-UP", "VarHandle Latency: " + latencyNs...`
- **Línea 149**: Zero-GC (String Concat) -> `DarkLogger.warning("WARM-UP", "High latency: " + latencyNs +...`
- **Línea 205**: Zero-GC (String Concat) -> `"Memory signature corrupted in bus " + i);`
- **Línea 248**: Zero-GC (String Concat) -> `"Exception during boot: " + e.getMessage());`
- **Línea 272**: Zero-GC (String Concat) -> `"Invalid initial state: " + controlRegister.getState());`
- **Línea 302**: Zero-GC (String Concat) -> `"Exception: " + e.getMessage());`
- **Línea 329**: Zero-GC (String Concat) -> `DarkLogger.info("BOOT", "  Status: " + (result.success ? "SU...`
- **Línea 330**: Zero-GC (String Concat) -> `DarkLogger.info("BOOT", "  Time:   " + String.format("%.3f m...`
- **Línea 330**: Aritmetica Modulo (Potencial) -> `DarkLogger.info("BOOT", "  Time:   " + String.format("%.3f m...`
- **Línea 340**: Zero-GC (String Concat) -> `DarkLogger.error("BOOT", "  Error:  " + result.errorMessage)...`

### [Reading Order: 00001101] Archivo: `sv/dark/kernel/SystemDependencyGraph.java`
- **Línea 73**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException("System cannot be null");`
- **Línea 116**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalStateException("Dependency not found: " + d...`
- **Línea 116**: Zero-GC (String Concat) -> `throw new IllegalStateException("Dependency not found: " + d...`
- **Línea 140**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalStateException(`
- **Línea 141**: Zero-GC (String Concat) -> `"Circular dependency detected! Remaining systems: " +`
- **Línea 171**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalStateException("Graph must be validated bef...`
- **Línea 183**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalStateException("Graph must be validated fir...`
- **Línea 193**: Bloqueo I/O (System.out) -> `System.out.println("[GRAPH] Not validated yet");`
- **Línea 197**: Zero-GC (String Concat) -> `System.out.println("[GRAPH] Execution Layers: " + executionL...`
- **Línea 197**: Bloqueo I/O (System.out) -> `System.out.println("[GRAPH] Execution Layers: " + executionL...`
- **Línea 200**: Zero-GC (String Concat) -> `System.out.println("[GRAPH] Layer " + i+ ": ");`
- **Línea 200**: Bloqueo I/O (System.out) -> `System.out.println("[GRAPH] Layer " + i+ ": ");`
- **Línea 202**: Zero-GC (String Concat) -> `System.out.print(system.getName() + " ");`
- **Línea 202**: Bloqueo I/O (System.out) -> `System.out.print(system.getName() + " ");`
- **Línea 204**: Bloqueo I/O (System.out) -> `System.out.println(" ");`

### [Reading Order: 00001110] Archivo: `sv/dark/kernel/ParallelSystemExecutor.java`
- **Línea 71**: FPU Overhead (Float/Double) -> `private double deltaTime;`
- **Línea 78**: FPU Overhead (Float/Double) -> `void setArgs(WorldStateFrame state, double deltaTime) {`
- **Línea 102**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException("Execution layers cannot ...`
- **Línea 121**: Zero-GC (New Object) -> `this.layerPhasers[i] = new Phaser(systemCount + 1);`
- **Línea 124**: Zero-GC (New Object) -> `this.layerTasks[i][j] = new SystemTask(layer.get(j), this.la...`
- **Línea 147**: Zero-GC (String Concat) -> `System.out.println("[PARALLEL] Executor initialized with " +`
- **Línea 147**: Bloqueo I/O (System.out) -> `System.out.println("[PARALLEL] Executor initialized with " +`
- **Línea 148**: Zero-GC (String Concat) -> `executionLayers.size() + " layers running on Java Virtual Th...`
- **Línea 173**: FPU Overhead (Float/Double) -> `public void execute(WorldStateFrame state, double deltaTime)...`
- **Línea 193**: FPU Overhead (Float/Double) -> `private void executeLayer(int layerIndex, List<GameSystem> l...`
- **Línea 234**: FPU Overhead (Float/Double) -> `public double getLastExecutionTimeMs() {`
- **Línea 243**: Bloqueo I/O (System.out) -> `System.out.println("[PARALLEL] Executor shutdown");`

### [Reading Order: 00001111] Archivo: `sv/dark/kernel/ThreadPinning.java`
- **Línea 131**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("ThreadPinning is a static utility ...`
- **Línea 158**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to pin thread (Windows). ...`
- **Línea 162**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Critical error during CPU Pinnin...`
- **Línea 172**: Aritmetica Modulo (Potencial) -> `int bitOffset = coreId % 64;`
- **Línea 180**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to pin thread (Linux/POSI...`
- **Línea 184**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Critical error during CPU Pinnin...`
- **Línea 214**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to query original thread ...`
- **Línea 225**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to query original thread ...`
- **Línea 247**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to restore thread affinit...`
- **Línea 258**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to restore thread affinit...`

### [Reading Order: 00010000] Archivo: `sv/dark/core/DarkLogger.java`
- **Línea 24**: Zero-GC (String Concat) -> `private static final String METRICS_FILE = LOG_DIR + File.se...`
- **Línea 25**: Zero-GC (String Concat) -> `private static final String ERRORS_FILE = LOG_DIR + File.sep...`
- **Línea 34**: Zero-GC (New Object) -> `File dir = new File(LOG_DIR);`
- **Línea 39**: Zero-GC (New Object) -> `metricsWriter = new PrintWriter(new FileWriter(METRICS_FILE,...`
- **Línea 40**: Zero-GC (New Object) -> `errorsWriter = new PrintWriter(new FileWriter(ERRORS_FILE, f...`
- **Línea 50**: Bloqueo I/O (System.out) -> `System.err.println("[DARK LOGGER FATAL] Could not initialize...`
- **Línea 56**: Aritmetica Modulo (Potencial) -> `return String.format("[%s] [%-5s] [%-15s] %s", timestamp, le...`
- **Línea 111**: Zero-GC (String Concat) -> `System.err.println("[DARK LOGGER] Execution finished with " ...`
- **Línea 111**: Bloqueo I/O (System.out) -> `System.err.println("[DARK LOGGER] Execution finished with " ...`

### [Reading Order: 00010000] Archivo: `sv/dark/kernel/SystemStateManager.java`
- **Línea 82**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to link kernel32 function...`
- **Línea 124**: Zero-GC (String Concat) -> `DarkLogger.error("KERNEL", "Failed to link PowrProf function...`
- **Línea 137**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException("Invalid GUID string: " +...`
- **Línea 137**: Zero-GC (String Concat) -> `throw new IllegalArgumentException("Invalid GUID string: " +...`
- **Línea 159**: Zero-GC (New Object) -> `StringBuilder sb = new StringBuilder();`
- **Línea 160**: Aritmetica Modulo (Potencial) -> `sb.append(String.format("%08x-%04x-%04x-", data1, data2 & 0x...`
- **Línea 162**: Aritmetica Modulo (Potencial) -> `sb.append(String.format("%02x", src.get(ValueLayout.JAVA_BYT...`
- **Línea 166**: Aritmetica Modulo (Potencial) -> `sb.append(String.format("%02x", src.get(ValueLayout.JAVA_BYT...`
- **Línea 206**: Zero-GC (New Object) -> `return new String(bytes, 0, len, StandardCharsets.UTF_16LE);`
- **Línea 239**: Zero-GC (String Concat) -> `DarkLogger.error("SYSTEM STATE", "Failed to capture power sc...`
- **Línea 256**: Zero-GC (New Object) -> `SystemSnapshot snapshot = new SystemSnapshot(originalAffinit...`
- **Línea 257**: Zero-GC (String Concat) -> `DarkLogger.info("SYSTEM STATE", "Captured system snapshot su...`
- **Línea 280**: Zero-GC (String Concat) -> `DarkLogger.error("SYSTEM STATE", "Failed to transition Power...`
- **Línea 293**: Zero-GC (String Concat) -> `DarkLogger.error("SYSTEM STATE", "Failed to transition Power...`
- **Línea 316**: Zero-GC (String Concat) -> `DarkLogger.info("SYSTEM STATE", "Restored Power Scheme via F...`
- **Línea 320**: Zero-GC (String Concat) -> `DarkLogger.error("SYSTEM STATE", "Failed to restore Power Sc...`
- **Línea 326**: Zero-GC (String Concat) -> `DarkLogger.info("SYSTEM STATE", "Restored Power Scheme via C...`
- **Línea 332**: Aritmetica Modulo (Potencial) -> `DarkLogger.info("SYSTEM STATE", String.format("Restored Thre...`
- **Línea 366**: Zero-GC (New Object) -> `ProcessBuilder pb = new ProcessBuilder(command);`
- **Línea 369**: Zero-GC (New Object) -> `try (BufferedReader reader = new BufferedReader(`
- **Línea 370**: Zero-GC (New Object) -> `new InputStreamReader(process.getInputStream(), StandardChar...`
- **Línea 371**: Zero-GC (New Object) -> `StringBuilder sb = new StringBuilder();`

### [Reading Order: 00010001] Archivo: `sv/dark/kernel/MetricsPacker.java`
- **Línea 40**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("MetricsPacker is a static utility ...`

### [Reading Order: 00010010] Archivo: `sv/dark/core/DarkSector.java`
- **Línea 34**: Zero-GC (New Object) -> `this.activeCount = new AtomicInteger(0);`

### [Reading Order: 00010010] Archivo: `sv/dark/kernel/SystemSnapshot.java`
- **Línea 48**: Aritmetica Modulo (Potencial) -> `return String.format("SNAPSHOT | ThreadMask: 0x%X | PowerGui...`

### [Reading Order: 00010011] Archivo: `sv/dark/core/DarkSectorManager.java`
- **Línea 37**: FPU Overhead (Float/Double) -> `public void updateLocation(long entityId, float x, float y, ...`
- **Línea 78**: Zero-GC (New Object) -> `DarkSector freshSector = new DarkSector(toKey, null, 1024);`

### [Reading Order: 00010100] Archivo: `sv/dark/core/DarkNativeConsole.java`
- **Línea 6**: AWT/Swing (Renderizado Lento) -> `import java.awt.*;`
- **Línea 24**: Zero-GC (New Object) -> `private final StringBuilder inputBuffer = new StringBuilder(...`
- **Línea 30**: Zero-GC (New Object) -> `private static final Font CONSOLE_FONT = new Font("Monospace...`
- **Línea 31**: Zero-GC (New Object) -> `private static final BasicStroke BASE_STROKE = new BasicStro...`
- **Línea 32**: Zero-GC (New Object) -> `private static final Color BG_OVERLAY = new Color(0, 0, 0, 2...`
- **Línea 34**: FPU Overhead (Float/Double) -> `private float flashIntensity = 0.0f;`
- **Línea 121**: Aritmetica Modulo (Potencial) -> `boolean showCursor = (tick % 60 < 30);`

### [Reading Order: 00010101] Archivo: `sv/dark/core/DarkSystemProbe.java`
- **Línea 57**: Zero-GC (String Concat) -> `DarkLogger.info("Boot", "OS: " + System.getProperty("os.name...`
- **Línea 58**: Zero-GC (String Concat) -> `DarkLogger.info("Boot", "JVM: " + System.getProperty("java.v...`

### [Reading Order: 00010110] Archivo: `sv/dark/net/DarkMetricsServer.java`
- **Línea 42**: Zero-GC (New Object) -> `this.server = HttpServer.create(new InetSocketAddress(port),...`
- **Línea 45**: Zero-GC (New Object) -> `server.createContext("/", new RootHandler());`
- **Línea 46**: Zero-GC (New Object) -> `server.createContext("/metrics", new MetricsHandler());`
- **Línea 47**: Zero-GC (New Object) -> `server.createContext("/api/state", new StateHandler(vault));`
- **Línea 48**: Zero-GC (New Object) -> `server.createContext("/editor", new EditorHandler());`
- **Línea 56**: Zero-GC (String Concat) -> `DarkLogger.info("METRICS GATEWAY", "Listening on port " + se...`

### [Reading Order: 00011000] Archivo: `sv/dark/admin/AdminController.java`
- **Línea 88**: Zero-GC (New Object) -> `Thread adminConsumer = new Thread(() -> runAdminLoop(kernel)...`
- **Línea 93**: Zero-GC (String Concat) -> `DarkLogger.error("Admin", "Failed to start Control Plane: " ...`
- **Línea 116**: Zero-GC (New Object) -> `StringBuilder jsonBuilder = new StringBuilder(2048);`
- **Línea 131**: Aritmetica Modulo (Potencial) -> `DarkLogger.info("METRICS", String.format("Frame: %d | Time: ...`

### [Reading Order: 00011000] Archivo: `sv/dark/bus/BusBenchmarkTest.java`
- **Línea 46**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 47**: Bloqueo I/O (System.out) -> `System.out.println("  DARK ENGINE - AAA+ BUS BENCHMARK & THR...`
- **Línea 48**: Bloqueo I/O (System.out) -> `System.out.println("  Target Latency: < 150ns  |  Target Thr...`
- **Línea 49**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 56**: Bloqueo I/O (System.out) -> `System.out.println("\n[PHASE 1] LATENCY TEST (Atomic Operati...`
- **Línea 58**: Zero-GC (New Object) -> `DarkAtomicBus bus = new DarkAtomicBus(12);`
- **Línea 61**: Bloqueo I/O (System.out) -> `System.out.print("Warmup... ");`
- **Línea 67**: Bloqueo I/O (System.out) -> `System.out.println("Done.");`
- **Línea 70**: Zero-GC (String Concat) -> `System.out.print("Measuring " + ITERATIONS + " ops... ");`
- **Línea 70**: Bloqueo I/O (System.out) -> `System.out.print("Measuring " + ITERATIONS + " ops... ");`
- **Línea 83**: Bloqueo I/O (System.out) -> `System.out.println("Done.");`
- **Línea 90**: FPU Overhead (Float/Double) -> `double avg = (double) totalNs / ITERATIONS;`
- **Línea 92**: Zero-GC (String Concat) -> `System.out.println("    -> Average Latency: " + String.forma...`
- **Línea 92**: Bloqueo I/O (System.out) -> `System.out.println("    -> Average Latency: " + String.forma...`
- **Línea 92**: Aritmetica Modulo (Potencial) -> `System.out.println("    -> Average Latency: " + String.forma...`
- **Línea 93**: Zero-GC (String Concat) -> `System.out.println("    -> P50: " + p50 + " ns");`
- **Línea 93**: Bloqueo I/O (System.out) -> `System.out.println("    -> P50: " + p50 + " ns");`
- **Línea 94**: Zero-GC (String Concat) -> `System.out.println("    -> P95: " + p95 + " ns");`
- **Línea 94**: Bloqueo I/O (System.out) -> `System.out.println("    -> P95: " + p95 + " ns");`
- **Línea 95**: Zero-GC (String Concat) -> `System.out.println("    -> P99: " + p99 + " ns");`
- **Línea 95**: Bloqueo I/O (System.out) -> `System.out.println("    -> P99: " + p99 + " ns");`
- **Línea 98**: Bloqueo I/O (System.out) -> `System.out.println("    [OK] AAA+ Certified (<150ns)");`
- **Línea 100**: Bloqueo I/O (System.out) -> `System.out.println("    [WARNING] Latency optimization requi...`
- **Línea 105**: Bloqueo I/O (System.out) -> `System.out.println("\n[PHASE 2] THROUGHPUT TEST (Batch Proce...`
- **Línea 107**: Zero-GC (New Object) -> `DarkAtomicBus bus = new DarkAtomicBus(16); // Large buffer f...`
- **Línea 126**: FPU Overhead (Float/Double) -> `double seconds = (endTime - startTime) / 1_000_000_000.0;`
- **Línea 127**: FPU Overhead (Float/Double) -> `double throughput = (ops) / seconds;`
- **Línea 129**: Zero-GC (String Concat) -> `System.out.println("    -> Total Time: " + String.format("%....`
- **Línea 129**: Bloqueo I/O (System.out) -> `System.out.println("    -> Total Time: " + String.format("%....`
- **Línea 129**: Aritmetica Modulo (Potencial) -> `System.out.println("    -> Total Time: " + String.format("%....`
- **Línea 130**: Zero-GC (String Concat) -> `System.out.println("    -> Total Ops: " + ops);`
- **Línea 130**: Bloqueo I/O (System.out) -> `System.out.println("    -> Total Ops: " + ops);`
- **Línea 131**: Zero-GC (String Concat) -> `System.out.println("    -> Throughput: " + String.format("%,...`
- **Línea 131**: Bloqueo I/O (System.out) -> `System.out.println("    -> Throughput: " + String.format("%,...`
- **Línea 131**: Aritmetica Modulo (Potencial) -> `System.out.println("    -> Throughput: " + String.format("%,...`
- **Línea 135**: Bloqueo I/O (System.out) -> `System.out.println("    [OK] AAA+ Certified (>10M/s)");`
- **Línea 137**: Bloqueo I/O (System.out) -> `System.out.println("    [WARNING] Throughput optimization re...`

### [Reading Order: 00011000] Archivo: `sv/dark/bus/BusCoordinationTest.java`
- **Línea 32**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Iniciando Protocolo de Coordinaci...`
- **Línea 35**: Bloqueo I/O (System.out) -> `System.out.println("  > Inicializando DarkAtomicBus (Capacit...`
- **Línea 36**: Zero-GC (New Object) -> `DarkAtomicBus atomicBus = new DarkAtomicBus(14); // 2^14`
- **Línea 40**: Zero-GC (String Concat) -> `System.out.println("    [INTEGRITY] AtomicBus Padding Checks...`
- **Línea 40**: Bloqueo I/O (System.out) -> `System.out.println("    [INTEGRITY] AtomicBus Padding Checks...`
- **Línea 42**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("Error: Bridge Isolation Padding Corrupted")...`
- **Línea 45**: Bloqueo I/O (System.out) -> `System.out.println("  > Inicializando DarkRingBus (Capacity:...`
- **Línea 46**: Zero-GC (New Object) -> `DarkRingBus ringBus = new DarkRingBus(14); // 2^14`
- **Línea 50**: Zero-GC (String Concat) -> `System.out.println("    [INTEGRITY] RingBus Padding Checksum...`
- **Línea 50**: Bloqueo I/O (System.out) -> `System.out.println("    [INTEGRITY] RingBus Padding Checksum...`
- **Línea 52**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("Error: Bridge Isolation Padding Corrupted")...`
- **Línea 55**: Bloqueo I/O (System.out) -> `System.out.println("  > Verificando Flujo de Datos...");`
- **Línea 59**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("AtomicBus Push Failed");`
- **Línea 63**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("AtomicBus Poll Mismatch");`
- **Línea 67**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("RingBus Push Failed");`
- **Línea 71**: Bloqueo I/O (System.out) -> `System.out.println("[SUCCESS] Coordinacion de Bus Verificada...`
- **Línea 72**: Bloqueo I/O (System.out) -> `System.out.println("[METRIC] Buses Operativos. Padding Integ...`
- **Línea 74**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("Fallo en Coordinacion de Datos.");`

### [Reading Order: 00011000] Archivo: `sv/dark/bus/BusHardwareTest.java`
- **Línea 43**: Bloqueo I/O (System.out) -> `System.out.println("[KERNEL] Iniciando Escaneo Nominal de Ch...`
- **Línea 46**: Zero-GC (New Object) -> `DarkAtomicBus atomicBus = new DarkAtomicBus(14);`
- **Línea 47**: Bloqueo I/O (System.out) -> `System.out.println(" > Analizando DarkAtomicBus...");`
- **Línea 70**: Zero-GC (String Concat) -> `System.out.println("   [INFO] AtomicBus Padding Checksum: " ...`
- **Línea 70**: Bloqueo I/O (System.out) -> `System.out.println("   [INFO] AtomicBus Padding Checksum: " ...`
- **Línea 71**: Bloqueo I/O (System.out) -> `System.out.println("   [PASS] AtomicBus Padding Variables Ac...`
- **Línea 74**: Zero-GC (New Object) -> `DarkRingBus ringBus = new DarkRingBus(14);`
- **Línea 75**: Bloqueo I/O (System.out) -> `System.out.println(" > Analizando DarkRingBus...");`
- **Línea 93**: Zero-GC (String Concat) -> `System.out.println("   [INFO] RingBus Padding Checksum: " + ...`
- **Línea 93**: Bloqueo I/O (System.out) -> `System.out.println("   [INFO] RingBus Padding Checksum: " + ...`
- **Línea 94**: Bloqueo I/O (System.out) -> `System.out.println("   [PASS] RingBus Padding Variables Acce...`
- **Línea 100**: Bloqueo I/O (System.out) -> `System.out.println("[SUCCESS] Integridad de Hardware y Datos...`
- **Línea 102**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("DATA MISMATCH: Signal loss detected in atom...`

### [Reading Order: 00011000] Archivo: `sv/dark/bus/BusMultiThreadStressTest.java`
- **Línea 37**: FPU Overhead (Float/Double) -> `private static double spscSeconds;`
- **Línea 38**: FPU Overhead (Float/Double) -> `private static double spscThroughput;`
- **Línea 39**: FPU Overhead (Float/Double) -> `private static double spscLatency;`
- **Línea 42**: FPU Overhead (Float/Double) -> `private static double mpscSeconds;`
- **Línea 43**: FPU Overhead (Float/Double) -> `private static double mpscThroughput;`
- **Línea 44**: FPU Overhead (Float/Double) -> `private static double mpscLatency;`
- **Línea 48**: Bloqueo I/O (System.out) -> `System.out.print("[TEST] Running Multi-threaded Cache Conten...`
- **Línea 55**: Zero-GC (String Concat) -> `System.err.println("FAILED: " + e.getMessage());`
- **Línea 55**: Bloqueo I/O (System.out) -> `System.err.println("FAILED: " + e.getMessage());`
- **Línea 60**: Bloqueo I/O (System.out) -> `System.out.println("DONE.");`
- **Línea 61**: Bloqueo I/O (System.out) -> `System.out.println("\n======================================...`
- **Línea 62**: Bloqueo I/O (System.out) -> `System.out.println("                  MULTI-THREADED BUS BEN...`
- **Línea 63**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 65**: Bloqueo I/O (System.out) -> `System.out.printf(" %-25s | %-12s | %-15s | %-10s%n", "TEST ...`
- **Línea 65**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-25s | %-12s | %-15s | %-10s%n", "TEST ...`
- **Línea 66**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 68**: Bloqueo I/O (System.out) -> `System.out.printf(" %-25s | %-12.4f | %,11.0f ops | %5.2f ns...`
- **Línea 68**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-25s | %-12.4f | %,11.0f ops | %5.2f ns...`
- **Línea 70**: Bloqueo I/O (System.out) -> `System.out.printf(" %-25s | %-12.4f | %,11.0f ops | %5.2f ns...`
- **Línea 70**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-25s | %-12.4f | %,11.0f ops | %5.2f ns...`
- **Línea 73**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 74**: Bloqueo I/O (System.out) -> `System.out.printf(" SPSC LATENCY STATUS: %s%n", spscOk ? "[O...`
- **Línea 74**: Aritmetica Modulo (Potencial) -> `System.out.printf(" SPSC LATENCY STATUS: %s%n", spscOk ? "[O...`
- **Línea 75**: Bloqueo I/O (System.out) -> `System.out.printf(" MPSC LATENCY STATUS: %s%n", mpscOk ? "[O...`
- **Línea 75**: Aritmetica Modulo (Potencial) -> `System.out.printf(" MPSC LATENCY STATUS: %s%n", mpscOk ? "[O...`
- **Línea 76**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 80**: Zero-GC (New Object) -> `DarkRingBus bus = new DarkRingBus(18); // 262,144 elements`
- **Línea 81**: Zero-GC (New Object) -> `CountDownLatch startLatch = new CountDownLatch(1);`
- **Línea 82**: Zero-GC (New Object) -> `CountDownLatch endLatch = new CountDownLatch(2);`
- **Línea 84**: Zero-GC (New Object) -> `Thread consumer = new Thread(() -> {`
- **Línea 98**: Zero-GC (New Object) -> `Thread producer = new Thread(() -> {`
- **Línea 125**: Zero-GC (New Object) -> `DarkAtomicBus bus = new DarkAtomicBus(18);`
- **Línea 128**: Zero-GC (New Object) -> `CountDownLatch startLatch = new CountDownLatch(1);`
- **Línea 129**: Zero-GC (New Object) -> `CountDownLatch endLatch = new CountDownLatch(3);`
- **Línea 131**: Zero-GC (New Object) -> `Thread consumer = new Thread(() -> {`
- **Línea 145**: Zero-GC (New Object) -> `Thread producer1 = new Thread(() -> {`
- **Línea 155**: Zero-GC (New Object) -> `Thread producer2 = new Thread(() -> {`

### [Reading Order: 00011000] Archivo: `sv/dark/config/DarkEngineConfig.java`
- **Línea 49**: Zero-GC (String Concat) -> `private static final String CONFIG_FILE = "config/dark-" + P...`
- **Línea 65**: FPU Overhead (Float/Double) -> `public static final double METRICS_SAMPLING;`
- **Línea 114**: Zero-GC (New Object) -> `Properties props = new Properties();`
- **Línea 118**: Zero-GC (New Object) -> `try (InputStream input = new FileInputStream(CONFIG_FILE)) {`
- **Línea 120**: Zero-GC (String Concat) -> `DarkLogger.info("Config", "Loaded profile: " + PROFILE + " f...`
- **Línea 123**: Zero-GC (String Concat) -> `DarkLogger.error("Config", "Failed to load " + CONFIG_FILE +...`
- **Línea 196**: Zero-GC (String Concat) -> `DarkLogger.info("Config", "Profile: " + PROFILE +`
- **Línea 197**: Zero-GC (String Concat) -> `" | Logging: " + (LOGGING_ENABLED ? "ENABLED" : "DISABLED") ...`
- **Línea 198**: Zero-GC (String Concat) -> `" | Metrics: " + (METRICS_SAMPLING * 100) + "%" +`
- **Línea 198**: Aritmetica Modulo (Potencial) -> `" | Metrics: " + (METRICS_SAMPLING * 100) + "%" +`
- **Línea 199**: Zero-GC (String Concat) -> `" | Validation: " + (VALIDATION_ENABLED ? "ENABLED" : "DISAB...`
- **Línea 200**: Zero-GC (String Concat) -> `" | Bus: " + BUS_CAPACITY +`
- **Línea 201**: Zero-GC (String Concat) -> `" | Pinning: " + (KERNEL_THREAD_PINNING ? "Core " + KERNEL_T...`
- **Línea 227**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("Cannot instantiate DarkEngineConfi...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/DarkAssetManager.java`
- **Línea 81**: Zero-GC (String Concat) -> `DarkLogger.info("Assets", "Boveda activa: " + vaultBase);`

### [Reading Order: 00011000] Archivo: `sv/dark/core/DarkDataAccelerator.java`
- **Línea 69**: Bloqueo I/O (System.out) -> `System.out.println();`
- **Línea 70**: Zero-GC (String Concat) -> `DarkLogger.info("TEST", "Hardware: Vector Bit Size: " + SPEC...`
- **Línea 74**: Zero-GC (String Concat) -> `DarkLogger.info("TEST", "Allocating " + mbAllocated + " MB O...`
- **Línea 92**: FPU Overhead (Float/Double) -> `double seconds = durationNs / 1_000_000_000.0;`
- **Línea 93**: FPU Overhead (Float/Double) -> `double gbProcessed = (dataSize * 4) / (1024.0 * 1024.0 * 102...`
- **Línea 94**: FPU Overhead (Float/Double) -> `double throughput = gbProcessed / seconds;`
- **Línea 96**: Zero-GC (String Concat) -> `DarkLogger.info("TEST", "-> Checksum: " + result);`
- **Línea 97**: Aritmetica Modulo (Potencial) -> `DarkLogger.info("TEST", String.format("-> Time: %.5f s", sec...`
- **Línea 98**: Aritmetica Modulo (Potencial) -> `DarkLogger.info("TEST", String.format("-> Throughput: %.2f G...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/DarkDisplayBridge.java`
- **Línea 8**: AWT/Swing (Renderizado Lento) -> `import java.awt.*;`
- **Línea 9**: AWT/Swing (Renderizado Lento) -> `import java.awt.image.BufferStrategy;`
- **Línea 44**: Zero-GC (New Object) -> `private static final Color BG_COLOR = new Color(5, 5, 10);`
- **Línea 45**: Zero-GC (New Object) -> `private static final Color SCANLINE_COLOR = new Color(0, 0, ...`
- **Línea 54**: Zero-GC (New Object) -> `this.hints = new RenderingHints(RenderingHints.KEY_ANTIALIAS...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/DarkHardwareProbe.java`
- **Línea 82**: Aritmetica Modulo (Potencial) -> `"Hardware Verified: %d GB RAM | %d Cores | Profile: %s",`

### [Reading Order: 00011000] Archivo: `sv/dark/core/DarkParticleSystem.java`
- **Línea 50**: Zero-GC (New Object) -> `private final Random RNG = new Random(0xCAFEBABE); // Fixed ...`
- **Línea 75**: FPU Overhead (Float/Double) -> `public void update(double dt) {`
- **Línea 76**: FPU Overhead (Float/Double) -> `float deltaTime = (float) dt;`
- **Línea 81**: FPU Overhead (Float/Double) -> `float y = particleData.get(ValueLayout.JAVA_FLOAT, base + 4)...`
- **Línea 82**: FPU Overhead (Float/Double) -> `float speed = particleData.get(ValueLayout.JAVA_FLOAT, base ...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/DarkTimeControlUnit.java`
- **Línea 75**: Aritmetica Modulo (Potencial) -> `writeIndex = (writeIndex + 1) % maxFrames;`
- **Línea 84**: Aritmetica Modulo (Potencial) -> `writeIndex = (writeIndex - 1 + maxFrames) % maxFrames;`

### [Reading Order: 00011000] Archivo: `sv/dark/core/EventBytePacker.java`
- **Línea 64**: FPU Overhead (Float/Double) -> `public static void pack(MemorySegment segment, long index, i...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/ExecutionValidator.java`
- **Línea 58**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new Error("[PANIC] INTEGRITY_VIOLATION");`

### [Reading Order: 00011000] Archivo: `sv/dark/core/MetricsCollector.java`
- **Línea 60**: FPU Overhead (Float/Double) -> `public double avgFrameTimeMs = 0;`
- **Línea 66**: Zero-GC (String Concat) -> `"Frame[%d] Time: %.2f ms | Latency: %d ns | " +`
- **Línea 66**: Aritmetica Modulo (Potencial) -> `"Frame[%d] Time: %.2f ms | Latency: %d ns | " +`
- **Línea 67**: Aritmetica Modulo (Potencial) -> `"Movement: %d | Render: %d | Physics: %d | Audio: %d",`

### [Reading Order: 00011000] Archivo: `sv/dark/core/SectorMap.java`
- **Línea 25**: FPU Overhead (Float/Double) -> `private static final float LOAD_FACTOR = 0.7f;`
- **Línea 66**: Zero-GC (New Object) -> `private final StampedLock lock = new StampedLock();`
- **Línea 194**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException("Invalid Key: Reserved Se...`
- **Línea 196**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException("Value cannot be null");`

### [Reading Order: 00011000] Archivo: `sv/dark/core/SpaceMath.java`
- **Línea 52**: FPU Overhead (Float/Double) -> `public static int getSectorIndex(float worldCoord) {`

### [Reading Order: 00011000] Archivo: `sv/dark/core/WorkStealingProcessor.java`
- **Línea 30**: Zero-GC (New Object) -> `this.pool = new ForkJoinPool(parallelism,`
- **Línea 39**: FPU Overhead (Float/Double) -> `public void execute(MemorySegment[] sectors, double dt) {`
- **Línea 45**: Zero-GC (New Object) -> `this.rootTask = new SectorTask(sectors, 0, sectors.length);`
- **Línea 61**: FPU Overhead (Float/Double) -> `private double dt;`
- **Línea 74**: Zero-GC (New Object) -> `this.left = new SectorTask(sectors, start, mid);`
- **Línea 75**: Zero-GC (New Object) -> `this.right = new SectorTask(sectors, mid, end);`
- **Línea 82**: FPU Overhead (Float/Double) -> `void prepare(MemorySegment[] sectors, double dt) {`
- **Línea 112**: FPU Overhead (Float/Double) -> `private void processSector(MemorySegment sector, double dt) ...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/AudioSystem.java`
- **Línea 37**: Zero-GC (New Object) -> `private final AtomicInteger processedCount = new AtomicInteg...`
- **Línea 48**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/CreditsLogic.java`
- **Línea 46**: FPU Overhead (Float/Double) -> `public static void update(WorldStateFrame state, double delt...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/DarkAudioLinker.java`
- **Línea 32**: Zero-GC (New Object) -> `File oalDll = new File("lib/soft_oal.dll");`
- **Línea 35**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new RuntimeException("Missing lib/soft_oal.dll");`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/DarkAudioSystem.java`
- **Línea 55**: Zero-GC (String Concat) -> `DarkLogger.warning("AUDIO", "OpenAL Initialization Exception...`
- **Línea 60**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`
- **Línea 65**: FPU Overhead (Float/Double) -> `float px = vault.readInt(DarkStateLayout.PLAYER_X) / 1000f; ...`
- **Línea 66**: FPU Overhead (Float/Double) -> `float py = vault.readInt(DarkStateLayout.PLAYER_Y) / 1000f;`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/DarkEntityController.java`
- **Línea 57**: Aritmetica Modulo (Potencial) -> `int nx = (tick * 13) % 800;`
- **Línea 58**: Aritmetica Modulo (Potencial) -> `int ny = (tick * 7) % 600;`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/DarkGraphicsLinker.java`
- **Línea 32**: Zero-GC (New Object) -> `File glfwDll = new File("lib/glfw3.dll");`
- **Línea 35**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new RuntimeException("Missing lib/glfw3.dll");`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/DarkInputSystem.java`
- **Línea 49**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`
- **Línea 58**: FPU Overhead (Float/Double) -> `double mx = mouseXPtr.get(ValueLayout.JAVA_DOUBLE, 0);`
- **Línea 59**: FPU Overhead (Float/Double) -> `double my = mouseYPtr.get(ValueLayout.JAVA_DOUBLE, 0);`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/DarkRenderSystem.java`
- **Línea 8**: AWT/Swing (Renderizado Lento) -> `import java.awt.Graphics2D;`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/DarkTheme.java`
- **Línea 8**: AWT/Swing (Renderizado Lento) -> `import java.awt.Color;`
- **Línea 9**: AWT/Swing (Renderizado Lento) -> `import java.awt.Graphics2D;`
- **Línea 10**: AWT/Swing (Renderizado Lento) -> `import java.awt.BasicStroke;`
- **Línea 39**: Zero-GC (New Object) -> `public static final Color MINT_NEON = new Color(0, 255, 163)...`
- **Línea 40**: Zero-GC (New Object) -> `public static final Color BACKGROUND = new Color(10, 10, 15)...`
- **Línea 41**: Zero-GC (New Object) -> `public static final Color PANEL_GLASS = new Color(30, 30, 45...`
- **Línea 42**: Zero-GC (New Object) -> `public static final Color ALERT_CRITICAL = new Color(220, 0,...`
- **Línea 43**: Zero-GC (New Object) -> `public static final Color ALERT_HEALING = new Color(0, 180, ...`
- **Línea 46**: Zero-GC (New Object) -> `private static final BasicStroke DEFAULT_STROKE = new BasicS...`
- **Línea 74**: Zero-GC (New Object) -> `g2d.setColor(new Color(255, 255, 255, 30));`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/GameSystem.java`
- **Línea 153**: FPU Overhead (Float/Double) -> `void update(WorldStateFrame state, double deltaTime);`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/MovementSystem.java`
- **Línea 45**: Zero-GC (New Object) -> `private final AtomicInteger processedCount = new AtomicInteg...`
- **Línea 64**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`
- **Línea 74**: FPU Overhead (Float/Double) -> `double x = segment.get(ValueLayout.JAVA_DOUBLE, currentBase ...`
- **Línea 75**: FPU Overhead (Float/Double) -> `double vx = segment.get(ValueLayout.JAVA_DOUBLE, currentBase...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/PhysicsSystem.java`
- **Línea 42**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`
- **Línea 55**: FPU Overhead (Float/Double) -> `public void simulateSIMD(MemorySegment segment, int entityCo...`
- **Línea 97**: FPU Overhead (Float/Double) -> `float px = segment.get(java.lang.foreign.ValueLayout.JAVA_FL...`
- **Línea 98**: FPU Overhead (Float/Double) -> `float py = segment.get(java.lang.foreign.ValueLayout.JAVA_FL...`
- **Línea 99**: FPU Overhead (Float/Double) -> `float vx = segment.get(java.lang.foreign.ValueLayout.JAVA_FL...`
- **Línea 100**: FPU Overhead (Float/Double) -> `float vy = segment.get(java.lang.foreign.ValueLayout.JAVA_FL...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/PlayerSystem.java`
- **Línea 42**: FPU Overhead (Float/Double) -> `private static final double BASE_VELOCITY = 300.0; // Pixels...`
- **Línea 51**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`
- **Línea 57**: FPU Overhead (Float/Double) -> `double currentX = state.readDouble(ADDR_POS_X);`
- **Línea 58**: FPU Overhead (Float/Double) -> `double currentY = state.readDouble(ADDR_POS_Y);`
- **Línea 60**: FPU Overhead (Float/Double) -> `double moveStep = BASE_VELOCITY * deltaTime;`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/RenderSystem.java`
- **Línea 37**: Zero-GC (New Object) -> `private final AtomicInteger processedCount = new AtomicInteg...`
- **Línea 48**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`

### [Reading Order: 00011000] Archivo: `sv/dark/core/systems/SpriteSystem.java`
- **Línea 11**: AWT/Swing (Renderizado Lento) -> `import java.awt.Graphics2D;`
- **Línea 59**: FPU Overhead (Float/Double) -> `double x = state.readDouble(base + EntityLayout.X_OFFSET);`
- **Línea 60**: FPU Overhead (Float/Double) -> `double y = state.readDouble(base + EntityLayout.Y_OFFSET);`
- **Línea 61**: FPU Overhead (Float/Double) -> `double glow = state.readDouble(base + EntityLayout.GLOW_ALPH...`
- **Línea 72**: FPU Overhead (Float/Double) -> `private void drawFromAtlas(Graphics2D g2d, double x, double ...`

### [Reading Order: 00011000] Archivo: `sv/dark/kernel/CleanupValidator.java`
- **Línea 45**: Bloqueo I/O (System.out) -> `System.out.println("\n--------------------------------------...`
- **Línea 46**: Bloqueo I/O (System.out) -> `System.out.println("OS CLEANUP AUDIT: INITIAL vs POST-SHUTDO...`
- **Línea 47**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 52**: Bloqueo I/O (System.out) -> `System.err.println("  [ERROR] AUDIT CRITICAL: One or both sn...`
- **Línea 53**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 59**: Bloqueo I/O (System.out) -> `System.out.println("  [OK] Thread Affinity: RESTORED OK");`
- **Línea 61**: Bloqueo I/O (System.out) -> `System.err.printf("  [ERROR] THREAD AFFINITY RESIDUAL DETECT...`
- **Línea 61**: Aritmetica Modulo (Potencial) -> `System.err.printf("  [ERROR] THREAD AFFINITY RESIDUAL DETECT...`
- **Línea 68**: Zero-GC (String Concat) -> `System.out.println("  [OK] Power Scheme: RESTORED OK (" + in...`
- **Línea 68**: Bloqueo I/O (System.out) -> `System.out.println("  [OK] Power Scheme: RESTORED OK (" + in...`
- **Línea 70**: Bloqueo I/O (System.out) -> `System.err.printf("  [ERROR] POWER SCHEME RESIDUAL DETECTED:...`
- **Línea 70**: Aritmetica Modulo (Potencial) -> `System.err.printf("  [ERROR] POWER SCHEME RESIDUAL DETECTED:...`
- **Línea 75**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 77**: Bloqueo I/O (System.out) -> `System.out.println("[OK] SYSTEM RESTORE VALIDATION PASSED: 1...`
- **Línea 77**: Aritmetica Modulo (Potencial) -> `System.out.println("[OK] SYSTEM RESTORE VALIDATION PASSED: 1...`
- **Línea 79**: Bloqueo I/O (System.out) -> `System.out.println("[ERROR] SYSTEM RESTORE VALIDATION FAILED...`
- **Línea 81**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`

### [Reading Order: 00011000] Archivo: `sv/dark/memory/SectorMemoryVault.java`
- **Línea 89**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new IllegalArgumentException("sectorCount debe ser > 0...`
- **Línea 101**: Aritmetica Modulo (Potencial) -> `if (address % PAGE_SIZE != 0) {`
- **Línea 102**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("Memory not 4KB aligned: " + addres...`
- **Línea 102**: Zero-GC (String Concat) -> `throw new AssertionError("Memory not 4KB aligned: " + addres...`
- **Línea 227**: Aritmetica Modulo (Potencial) -> `return (segment.address() % PAGE_SIZE) == 0;`

### [Reading Order: 00011000] Archivo: `sv/dark/net/DarkSaturationProbe.java`
- **Línea 49**: Zero-GC (New Object) -> `try (Socket socket = new Socket(host, port);`
- **Línea 50**: Zero-GC (New Object) -> `DataOutputStream out = new DataOutputStream(socket.getOutput...`

### [Reading Order: 00011000] Archivo: `sv/dark/net/DarkTelemetryStream.java`
- **Línea 39**: Zero-GC (New Object) -> `private static final AtomicLong cursor = new AtomicLong(0);`

### [Reading Order: 00011000] Archivo: `sv/dark/state/DarkEngineMaster.java`
- **Línea 54**: Zero-GC (New Object) -> `SectorMemoryVault memoryVault = new SectorMemoryVault(1024);`
- **Línea 61**: Zero-GC (New Object) -> `EngineKernel kernel = new EngineKernel(dispatcher, memoryVau...`
- **Línea 97**: Zero-GC (New Object) -> `registry.registerGameSystem(new DarkInputSystem(memoryVault)...`
- **Línea 98**: Zero-GC (New Object) -> `registry.registerGameSystem(new DarkAudioSystem(memoryVault)...`

### [Reading Order: 00011000] Archivo: `sv/dark/state/WorldStateFrame.java`
- **Línea 91**: FPU Overhead (Float/Double) -> `public double readDouble(long offset) {`
- **Línea 99**: FPU Overhead (Float/Double) -> `public void writeDouble(long offset, double value) {`
- **Línea 107**: FPU Overhead (Float/Double) -> `public float readFloat(long offset) {`
- **Línea 123**: FPU Overhead (Float/Double) -> `public void writeFloat(long offset, float value) {`

### [Reading Order: 00011000] Archivo: `sv/dark/test/BusBenchmarkTest.java`
- **Línea 38**: Zero-GC (New Object) -> `private static final DarkAtomicBus bus = new DarkAtomicBus(1...`
- **Línea 45**: Bloqueo I/O (System.out) -> `System.out.println("=== AAA+ DarkEngine Benchmark ===");`
- **Línea 52**: FPU Overhead (Float/Double) -> `double latency = runLatencyTest();`
- **Línea 58**: Bloqueo I/O (System.out) -> `System.out.println("=== Certification Complete ===");`
- **Línea 59**: Zero-GC (String Concat) -> `System.out.println("AAA+ Status: " + (certified ? "CERTIFIED...`
- **Línea 59**: Bloqueo I/O (System.out) -> `System.out.println("AAA+ Status: " + (certified ? "CERTIFIED...`
- **Línea 66**: Zero-GC (String Concat) -> `System.out.print("JIT Warmup (" + WARMUP_ITERATIONS + " ops)...`
- **Línea 66**: Bloqueo I/O (System.out) -> `System.out.print("JIT Warmup (" + WARMUP_ITERATIONS + " ops)...`
- **Línea 73**: Zero-GC (String Concat) -> `System.out.println("Done in " + ((end - start) / 1_000_000) ...`
- **Línea 73**: Bloqueo I/O (System.out) -> `System.out.println("Done in " + ((end - start) / 1_000_000) ...`
- **Línea 76**: FPU Overhead (Float/Double) -> `private static double runLatencyTest() {`
- **Línea 77**: Bloqueo I/O (System.out) -> `System.out.println("\n[Phase 1] Atomic Latency Test (offer/p...`
- **Línea 94**: FPU Overhead (Float/Double) -> `double avgLatency = (double) totalTime / MEASURE_ITERATIONS;`
- **Línea 95**: Bloqueo I/O (System.out) -> `System.out.printf("  > Average Latency: %.2f ns/op\n", avgLa...`
- **Línea 95**: Aritmetica Modulo (Potencial) -> `System.out.printf("  > Average Latency: %.2f ns/op\n", avgLa...`
- **Línea 98**: Bloqueo I/O (System.out) -> `System.out.println("  > Status: PASSED (AAA+ Certified)");`
- **Línea 100**: Bloqueo I/O (System.out) -> `System.out.println("  > Status: FAILED (Optimization Require...`
- **Línea 107**: Bloqueo I/O (System.out) -> `System.out.println("\n[Phase 2] High-Velocity Throughput Tes...`
- **Línea 123**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new RuntimeException("Bus Saturation: Expected " + BAT...`
- **Línea 123**: Zero-GC (String Concat) -> `throw new RuntimeException("Bus Saturation: Expected " + BAT...`
- **Línea 134**: FPU Overhead (Float/Double) -> `double durationSec = durationNs / 1_000_000_000.0;`
- **Línea 137**: Bloqueo I/O (System.out) -> `System.out.printf("  > Throughput: %,d ops/sec\n", opsPerSec...`
- **Línea 137**: Aritmetica Modulo (Potencial) -> `System.out.printf("  > Throughput: %,d ops/sec\n", opsPerSec...`
- **Línea 140**: Bloqueo I/O (System.out) -> `System.out.println("  > Status: PASSED (AAA+ Certified)");`
- **Línea 142**: Bloqueo I/O (System.out) -> `System.out.println("  > Status: FAILED");`

### [Reading Order: 00011000] Archivo: `sv/dark/test/DependencyGraphPerformanceTest.java`
- **Línea 41**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 42**: Bloqueo I/O (System.out) -> `System.out.println("  AAA+ CERTIFICATION: DEPENDENCY GRAPH P...`
- **Línea 43**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 46**: Zero-GC (New Object) -> `SystemDependencyGraph graph = new SystemDependencyGraph();`
- **Línea 58**: Zero-GC (New Object) -> `GameSystem dummySystem = new GameSystem() {`
- **Línea 59**: FPU Overhead (Float/Double) -> `@Override public void update(WorldStateFrame state, double d...`
- **Línea 75**: Zero-GC (String Concat) -> `System.out.println("[TEST] systemsByName initial table capac...`
- **Línea 75**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] systemsByName initial table capac...`
- **Línea 76**: Zero-GC (String Concat) -> `System.out.println("[TEST] dependencies initial table capaci...`
- **Línea 76**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] dependencies initial table capaci...`
- **Línea 79**: Bloqueo I/O (System.out) -> `System.out.println("\n[PASSED] SYSTEM DEPENDENCY GRAPH MAPS ...`
- **Línea 82**: Bloqueo I/O (System.out) -> `System.err.println("\n[FAILED] SYSTEM DEPENDENCY GRAPH MAPS ...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/GovernorTest.java`
- **Línea 37**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 38**: Bloqueo I/O (System.out) -> `System.out.println("TEST: DARK GOVERNOR (DYNAMIC FPS SCALING...`
- **Línea 39**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 41**: Zero-GC (New Object) -> `TimeKeeper timeKeeper = new TimeKeeper();`
- **Línea 43**: Bloqueo I/O (System.out) -> `System.out.println("\n[PHASE 1] Warmup (Simulating light loa...`
- **Línea 47**: Bloqueo I/O (System.out) -> `System.out.println("\n[PHASE 2] Stress Test (Simulating 'Cyb...`
- **Línea 52**: Bloqueo I/O (System.out) -> `System.out.println("\n[PHASE 3] Recovery (Returning to calm)...`
- **Línea 56**: Bloqueo I/O (System.out) -> `System.out.println("\n[PHASE 4] TNT OVERLOAD (Simulating mas...`
- **Línea 61**: Bloqueo I/O (System.out) -> `System.out.println("\n======================================...`
- **Línea 62**: Bloqueo I/O (System.out) -> `System.out.println("TEST COMPLETE");`
- **Línea 63**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 80**: Aritmetica Modulo (Potencial) -> `if (i % 30 == 0) {`

### [Reading Order: 00011000] Archivo: `sv/dark/test/GracefulShutdownTest.java`
- **Línea 42**: Bloqueo I/O (System.out) -> `System.out.print("[TEST] Running Graceful Shutdown & Baselin...`
- **Línea 46**: Zero-GC (New Object) -> `Thread engineThread = new Thread(() -> {`
- **Línea 49**: Zero-GC (New Object) -> `SectorMemoryVault vault = new SectorMemoryVault(1024);`
- **Línea 50**: Zero-GC (New Object) -> `EngineKernel kernel = new EngineKernel(dispatcher, vault);`
- **Línea 53**: Zero-GC (String Concat) -> `System.err.println("Engine error: " + e.getMessage());`
- **Línea 53**: Bloqueo I/O (System.out) -> `System.err.println("Engine error: " + e.getMessage());`
- **Línea 74**: Bloqueo I/O (System.out) -> `System.out.println("DONE.");`
- **Línea 75**: Bloqueo I/O (System.out) -> `System.out.println("\n======================================...`
- **Línea 76**: Bloqueo I/O (System.out) -> `System.out.println("               GRACEFUL SHUTDOWN MEMORY ...`
- **Línea 77**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 78**: Bloqueo I/O (System.out) -> `System.out.printf(" %-20s | %-12s | %-12s | %-10s%n", "STATE...`
- **Línea 78**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-20s | %-12s | %-12s | %-10s%n", "STATE...`
- **Línea 79**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 80**: Bloqueo I/O (System.out) -> `System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "A...`
- **Línea 80**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "A...`
- **Línea 81**: Bloqueo I/O (System.out) -> `System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "B...`
- **Línea 81**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "B...`
- **Línea 82**: Bloqueo I/O (System.out) -> `System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "C...`
- **Línea 82**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-20s | %-12.2f | %-12.2f | %-10d%n", "C...`
- **Línea 83**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 87**: Bloqueo I/O (System.out) -> `System.out.printf(" ENGINE HEAP IMPACT   : %,d bytes (%.2f M...`
- **Línea 87**: Aritmetica Modulo (Potencial) -> `System.out.printf(" ENGINE HEAP IMPACT   : %,d bytes (%.2f M...`
- **Línea 88**: Bloqueo I/O (System.out) -> `System.out.printf(" ENGINE NON-HEAP PULL : %,d bytes (%.2f M...`
- **Línea 88**: Aritmetica Modulo (Potencial) -> `System.out.printf(" ENGINE NON-HEAP PULL : %,d bytes (%.2f M...`
- **Línea 89**: Bloqueo I/O (System.out) -> `System.out.printf(" ENGINE THREAD COUNT  : %d threads%n", st...`
- **Línea 89**: Aritmetica Modulo (Potencial) -> `System.out.printf(" ENGINE THREAD COUNT  : %d threads%n", st...`
- **Línea 90**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 91**: Bloqueo I/O (System.out) -> `System.out.printf(" LEAK STATUS: %s%n", passed ? "[OK] NO LE...`
- **Línea 91**: Aritmetica Modulo (Potencial) -> `System.out.printf(" LEAK STATUS: %s%n", passed ? "[OK] NO LE...`
- **Línea 92**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 102**: Aritmetica Modulo (Potencial) -> `return String.format("Heap=%.2fMB, NonHeap=%.2fMB, Threads=%...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/MetricsAggregationTest.java`
- **Línea 41**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 42**: Bloqueo I/O (System.out) -> `System.out.println("TEST: METRICS AGGREGATION & ZERO FALSE S...`
- **Línea 43**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 50**: Bloqueo I/O (System.out) -> `System.out.println("\n[PASSED] METRICS AGGREGATION IS AAA+ C...`
- **Línea 53**: Bloqueo I/O (System.out) -> `System.err.println("\n[FAILED] TEST SUITE ENCOUNTERED ERRORS...`
- **Línea 60**: Bloqueo I/O (System.out) -> `System.out.println("\n[RUNNING] testMetricsAggregationNoCont...`
- **Línea 63**: Zero-GC (New Object) -> `MovementSystem movement = new MovementSystem();`
- **Línea 64**: Zero-GC (New Object) -> `RenderSystem render = new RenderSystem();`
- **Línea 65**: Zero-GC (New Object) -> `PhysicsSystem physics = new PhysicsSystem();`
- **Línea 66**: Zero-GC (New Object) -> `AudioSystem audio = new AudioSystem();`
- **Línea 81**: Bloqueo I/O (System.out) -> `System.out.println("[PASS] No contention validation successf...`
- **Línea 85**: Bloqueo I/O (System.out) -> `System.out.println("\n[RUNNING] testMetricsAggregationPerfor...`
- **Línea 88**: Zero-GC (New Object) -> `MovementSystem warmMovement = new MovementSystem();`
- **Línea 89**: Zero-GC (New Object) -> `RenderSystem warmRender = new RenderSystem();`
- **Línea 90**: Zero-GC (New Object) -> `PhysicsSystem warmPhysics = new PhysicsSystem();`
- **Línea 91**: Zero-GC (New Object) -> `AudioSystem warmAudio = new AudioSystem();`
- **Línea 101**: Zero-GC (New Object) -> `MovementSystem movement = new MovementSystem();`
- **Línea 102**: Zero-GC (New Object) -> `RenderSystem render = new RenderSystem();`
- **Línea 103**: Zero-GC (New Object) -> `PhysicsSystem physics = new PhysicsSystem();`
- **Línea 104**: Zero-GC (New Object) -> `AudioSystem audio = new AudioSystem();`
- **Línea 106**: Zero-GC (New Object) -> `Thread t1 = new Thread(() -> {`
- **Línea 111**: Zero-GC (New Object) -> `Thread t2 = new Thread(() -> {`
- **Línea 116**: Zero-GC (New Object) -> `Thread t3 = new Thread(() -> {`
- **Línea 121**: Zero-GC (New Object) -> `Thread t4 = new Thread(() -> {`
- **Línea 148**: Zero-GC (String Concat) -> `System.out.println("Throughput: " + (opsPerSec / 1_000_000) ...`
- **Línea 148**: Bloqueo I/O (System.out) -> `System.out.println("Throughput: " + (opsPerSec / 1_000_000) ...`
- **Línea 152**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new RuntimeException("Performance too low: " + opsPerS...`
- **Línea 152**: Zero-GC (String Concat) -> `throw new RuntimeException("Performance too low: " + opsPerS...`
- **Línea 154**: Bloqueo I/O (System.out) -> `System.out.println("[PASS] Performance test exceeded 10M ops...`
- **Línea 158**: Bloqueo I/O (System.out) -> `System.out.println("\n[RUNNING] testZeroFalseSharing...");`
- **Línea 160**: Zero-GC (New Object) -> `MovementSystem movement = new MovementSystem();`
- **Línea 161**: Zero-GC (New Object) -> `RenderSystem render = new RenderSystem();`
- **Línea 167**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new RuntimeException("Memory address collision: instan...`
- **Línea 169**: Bloqueo I/O (System.out) -> `System.out.println("[PASS] Memory layout isolation certified...`
- **Línea 174**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new RuntimeException(msg + " - Expected: " + expected ...`
- **Línea 174**: Zero-GC (String Concat) -> `throw new RuntimeException(msg + " - Expected: " + expected ...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/ParticleSystemDeterminismTest.java`
- **Línea 42**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 43**: Bloqueo I/O (System.out) -> `System.out.println("  AAA+ CERTIFICATION: PARTICLE SYSTEM DE...`
- **Línea 44**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 48**: Zero-GC (New Object) -> `Random expectedRNG = new Random(0xCAFEBABE);`
- **Línea 49**: FPU Overhead (Float/Double) -> `float expectedX = expectedRNG.nextFloat() * 1280;`
- **Línea 50**: FPU Overhead (Float/Double) -> `float expectedY = expectedRNG.nextFloat() * 720;`
- **Línea 51**: FPU Overhead (Float/Double) -> `float expectedSpeed = expectedRNG.nextFloat() * 2;`
- **Línea 54**: Zero-GC (New Object) -> `DarkParticleSystem system = new DarkParticleSystem(arena);`
- **Línea 62**: FPU Overhead (Float/Double) -> `float actualX = particleData.get(ValueLayout.JAVA_FLOAT, 0L)...`
- **Línea 63**: FPU Overhead (Float/Double) -> `float actualY = particleData.get(ValueLayout.JAVA_FLOAT, 4L)...`
- **Línea 64**: FPU Overhead (Float/Double) -> `float actualSpeed = particleData.get(ValueLayout.JAVA_FLOAT,...`
- **Línea 66**: Bloqueo I/O (System.out) -> `System.out.printf("[TEST] Expected Particle 0: X=%.6f, Y=%.6...`
- **Línea 66**: Aritmetica Modulo (Potencial) -> `System.out.printf("[TEST] Expected Particle 0: X=%.6f, Y=%.6...`
- **Línea 67**: Bloqueo I/O (System.out) -> `System.out.printf("[TEST] Actual Particle 0:   X=%.6f, Y=%.6...`
- **Línea 67**: Aritmetica Modulo (Potencial) -> `System.out.printf("[TEST] Actual Particle 0:   X=%.6f, Y=%.6...`
- **Línea 72**: Bloqueo I/O (System.out) -> `System.out.println("\n[PASSED] PARTICLE SYSTEM INITIALIZATIO...`
- **Línea 75**: Bloqueo I/O (System.out) -> `System.err.println("\n[FAILED] PARTICLE SYSTEM INITIALIZATIO...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/SimdPhysicsDemoTest.java`
- **Línea 15**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 16**: Bloqueo I/O (System.out) -> `System.out.println("  DARK ENGINE - AAA+ SIMD PHYSICS BENCHM...`
- **Línea 17**: Bloqueo I/O (System.out) -> `System.out.println("  Target Throughput: < 2.0 ms per 1,000,...`
- **Línea 18**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 23**: Zero-GC (New Object) -> `SectorMemoryVault vault = new SectorMemoryVault(256);`
- **Línea 24**: Zero-GC (New Object) -> `PhysicsSystem physics = new PhysicsSystem();`
- **Línea 27**: Bloqueo I/O (System.out) -> `System.out.print("Warming up JIT... ");`
- **Línea 31**: Bloqueo I/O (System.out) -> `System.out.println("Done.");`
- **Línea 34**: Bloqueo I/O (System.out) -> `System.out.print("Measuring SIMD Physics Execution... ");`
- **Línea 40**: FPU Overhead (Float/Double) -> `double ms = (endNs - startNs) / 1_000_000.0;`
- **Línea 42**: Bloqueo I/O (System.out) -> `System.out.println("Done.");`
- **Línea 43**: Bloqueo I/O (System.out) -> `System.out.println();`
- **Línea 44**: Zero-GC (String Concat) -> `System.out.println("    -> Entities Processed: " + entities)...`
- **Línea 44**: Bloqueo I/O (System.out) -> `System.out.println("    -> Entities Processed: " + entities)...`
- **Línea 45**: Bloqueo I/O (System.out) -> `System.out.println(String.format("    -> Execution Time: %.4...`
- **Línea 45**: Aritmetica Modulo (Potencial) -> `System.out.println(String.format("    -> Execution Time: %.4...`
- **Línea 48**: Bloqueo I/O (System.out) -> `System.out.println("    [OK] AAA+ Certified (<2.0ms)");`
- **Línea 50**: Bloqueo I/O (System.out) -> `System.out.println("    [WARNING] Physics throughput below A...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/SystemDependencyTest.java`
- **Línea 42**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/SystemExecutionTest.java`
- **Línea 42**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/SystemParallelismTest.java`
- **Línea 41**: FPU Overhead (Float/Double) -> `public void update(WorldStateFrame state, double deltaTime) ...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/SystemRegistryCapacityTest.java`
- **Línea 39**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 40**: Bloqueo I/O (System.out) -> `System.out.println("  AAA+ CERTIFICATION: SYSTEM REGISTRY PR...`
- **Línea 41**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 44**: Zero-GC (New Object) -> `SystemRegistry registry = new SystemRegistry();`
- **Línea 66**: Zero-GC (String Concat) -> `System.out.println("[TEST] gameSystems collection initial ca...`
- **Línea 66**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] gameSystems collection initial ca...`
- **Línea 67**: Zero-GC (String Concat) -> `System.out.println("[TEST] renderSystems collection initial ...`
- **Línea 67**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] renderSystems collection initial ...`
- **Línea 70**: Bloqueo I/O (System.out) -> `System.out.println("\n[PASSED] SYSTEM REGISTRY COLLECTIONS A...`
- **Línea 73**: Bloqueo I/O (System.out) -> `System.err.println("\n[FAILED] SYSTEM REGISTRY COLLECTIONS A...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/SystemStateManagerTest.java`
- **Línea 39**: Bloqueo I/O (System.out) -> `System.out.println("====================...`
- **Línea 40**: Bloqueo I/O (System.out) -> `System.out.println("TEST: SYSTEM STATE MANAGER & CLEANUP VAL...`
- **Línea 41**: Bloqueo I/O (System.out) -> `System.out.println("====================...`
- **Línea 44**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 1: Capturing initial system ...`
- **Línea 46**: Bloqueo I/O (System.out) -> `System.out.println(initial.formatTelemetryData());`
- **Línea 49**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("FAILED: Power Scheme GUID cannot b...`
- **Línea 52**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("FAILED: Power Source cannot be nul...`
- **Línea 54**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 1 passed.\n");`
- **Línea 57**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 2: Applying performance boos...`
- **Línea 60**: Bloqueo I/O (System.out) -> `System.err.println("[TEST WARNING] Failed to apply performan...`
- **Línea 62**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 2 passed.");`
- **Línea 64**: Bloqueo I/O (System.out) -> `System.out.println();`
- **Línea 67**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 3: Restoring original OS set...`
- **Línea 70**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("FAILED: Thread affinity or Power P...`
- **Línea 72**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 3 passed.\n");`
- **Línea 75**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 4: Capturing final snapshot ...`
- **Línea 79**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new AssertionError("FAILED: CleanupValidator flagged r...`
- **Línea 81**: Bloqueo I/O (System.out) -> `System.out.println("[TEST] Step 4 passed.\n");`
- **Línea 83**: Bloqueo I/O (System.out) -> `System.out.println("====================...`
- **Línea 84**: Bloqueo I/O (System.out) -> `System.out.println("SYSTEM STATE MANAGER TEST: PASSED [OK]")...`
- **Línea 85**: Bloqueo I/O (System.out) -> `System.out.println("====================...`

### [Reading Order: 00011000] Archivo: `sv/dark/test/UltraFastBootTest.java`
- **Línea 27**: Bloqueo I/O (System.out) -> `System.out.print("[TEST] Running Ultra Fast Boot Sequence Va...`
- **Línea 30**: Zero-GC (New Object) -> `KernelControlRegister controlRegister = new KernelControlReg...`
- **Línea 33**: Zero-GC (New Object) -> `SectorMemoryVault memoryVault = new SectorMemoryVault(1024);`
- **Línea 34**: Zero-GC (New Object) -> `DarkAtomicBus systemBus = new DarkAtomicBus(1024);`
- **Línea 35**: Zero-GC (New Object) -> `DarkAtomicBus inputBus = new DarkAtomicBus(1024);`
- **Línea 50**: Bloqueo I/O (System.out) -> `System.out.println("DONE.");`
- **Línea 52**: Bloqueo I/O (System.out) -> `System.out.println("\n======================================...`
- **Línea 53**: Bloqueo I/O (System.out) -> `System.out.println("                   ULTRA FAST BOOT PROTO...`
- **Línea 54**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 55**: Bloqueo I/O (System.out) -> `System.out.printf(" %-20s | %-20s%n", "METRIC", "VALUE");`
- **Línea 55**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-20s | %-20s%n", "METRIC", "VALUE");`
- **Línea 56**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 57**: Bloqueo I/O (System.out) -> `System.out.printf(" %-20s | %-20.4f ms%n", "Execution Time",...`
- **Línea 57**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-20s | %-20.4f ms%n", "Execution Time",...`
- **Línea 58**: Bloqueo I/O (System.out) -> `System.out.printf(" %-20s | %-20s%n", "Target Standard", "< ...`
- **Línea 58**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-20s | %-20s%n", "Target Standard", "< ...`
- **Línea 59**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 62**: Bloqueo I/O (System.out) -> `System.out.println(" BOOT STATUS: [OK] AAA+ COMPLIANT");`
- **Línea 63**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 66**: Bloqueo I/O (System.out) -> `System.out.println(" BOOT STATUS: [FAILED] SYSTEM TOO SLOW O...`
- **Línea 67**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 72**: Zero-GC (String Concat) -> `System.err.println("FAILED: " + e.getMessage());`
- **Línea 72**: Bloqueo I/O (System.out) -> `System.err.println("FAILED: " + e.getMessage());`
- **Línea 80**: Zero-GC (New Object) -> `KernelControlRegister dummyReg = new KernelControlRegister()...`
- **Línea 85**: Zero-GC (New Object) -> `SectorMemoryVault dummyVault = new SectorMemoryVault(128);`
- **Línea 87**: Zero-GC (New Object) -> `DarkAtomicBus dummyBus = new DarkAtomicBus(128);`

### [Reading Order: 00011000] Archivo: `sv/dark/ui/EngineStateChannel.java`
- **Línea 45**: Zero-GC (New Object) -> `public static final AtomicInteger STATE = new AtomicInteger(...`

### [Reading Order: 00011000] Archivo: `sv/dark/validation/BusSymmetryValidator.java`
- **Línea 77**: Zero-GC (New Object) -> `return new ValidationResult(true, null, head, tail, capacity...`
- **Línea 81**: Zero-GC (New Object) -> `return new ValidationResult(false, error, head, tail, capaci...`
- **Línea 87**: Aritmetica Modulo (Potencial) -> `return String.format("VALID [head=%d, tail=%d, capacity=%d]"...`
- **Línea 90**: Aritmetica Modulo (Potencial) -> `return String.format("INVALID [%s] [head=%d, tail=%d, capaci...`
- **Línea 241**: Zero-GC (String Concat) -> `DarkLogger.error("BUS VALIDATION", "Bus " + i+ ": " + result...`
- **Línea 255**: Zero-GC (String Concat) -> `DarkLogger.error("BUS VALIDATION", "RingBus " + i+ ": " + re...`

### [Reading Order: 00100000] Archivo: `sv/dark/scene/DarkTransformSoA.java`
- **Línea 45**: FPU Overhead (Float/Double) -> `long bytesRequired32 = capacity * 4L; // 4 bytes por float`
- **Línea 46**: FPU Overhead (Float/Double) -> `long bytesRequired64 = capacity * 8L; // 8 bytes por double`
- **Línea 58**: Zero-GC (String Concat) -> `DarkLogger.info("ECS", "SoA Allocator: " + capacity + " enti...`
- **Línea 68**: FPU Overhead (Float/Double) -> `public void setEntity(int entityId, double globalPx, double ...`
- **Línea 77**: FPU Overhead (Float/Double) -> `posX.set(ValueLayout.JAVA_FLOAT, offset32, (float) globalPx)...`
- **Línea 78**: FPU Overhead (Float/Double) -> `posY.set(ValueLayout.JAVA_FLOAT, offset32, (float) globalPy)...`

### [Reading Order: 00100001] Archivo: `sv/dark/scene/DarkKinematicsSystem.java`
- **Línea 35**: FPU Overhead (Float/Double) -> `public static void update(DarkTransformSoA soa, float dt, do...`
- **Línea 75**: FPU Overhead (Float/Double) -> `double px = soa.globalPosX.get(ValueLayout.JAVA_DOUBLE, offs...`
- **Línea 76**: FPU Overhead (Float/Double) -> `float vx = soa.velX.get(ValueLayout.JAVA_FLOAT, offset32);`
- **Línea 77**: FPU Overhead (Float/Double) -> `double newPx = px + (vx * dt);`
- **Línea 79**: FPU Overhead (Float/Double) -> `soa.posX.set(ValueLayout.JAVA_FLOAT, offset32, (float)(newPx...`
- **Línea 81**: FPU Overhead (Float/Double) -> `double py = soa.globalPosY.get(ValueLayout.JAVA_DOUBLE, offs...`
- **Línea 82**: FPU Overhead (Float/Double) -> `float vy = soa.velY.get(ValueLayout.JAVA_FLOAT, offset32);`
- **Línea 83**: FPU Overhead (Float/Double) -> `double newPy = py + (vy * dt);`
- **Línea 85**: FPU Overhead (Float/Double) -> `soa.posY.set(ValueLayout.JAVA_FLOAT, offset32, (float)(newPy...`

### [Reading Order: 01100000] Archivo: `sv/dark/test/SystemSIMDKinematicsTest.java`
- **Línea 15**: Zero-GC (String Concat) -> `DarkLogger.info("TEST", "[16/16] Running SIMD Kinematics Thr...`
- **Línea 17**: Zero-GC (New Object) -> `DarkTransformSoA soa = new DarkTransformSoA(ENTITY_COUNT);`
- **Línea 34**: FPU Overhead (Float/Double) -> `double durationMs = (end - start) / 1_000_000.0;`
- **Línea 36**: Aritmetica Modulo (Potencial) -> `DarkLogger.info("TEST", String.format("SIMD Kinematics proce...`
- **Línea 41**: Zero-GC (Exception Throw - Tolerable en Fatal) -> `throw new RuntimeException("Kinematics latency exceeded 4.0 ...`
- **Línea 41**: Zero-GC (String Concat) -> `throw new RuntimeException("Kinematics latency exceeded 4.0 ...`

### [Reading Order: NO_ORDER] Archivo: `sv/dark/test/PowerSavingTest.java`
- **Línea 40**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 41**: Bloqueo I/O (System.out) -> `System.out.println("TEST: POWER SAVING IDLE SCALING (3 TIERS...`
- **Línea 42**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 46**: Zero-GC (New Object) -> `SectorMemoryVault vault = new SectorMemoryVault(1024);`
- **Línea 47**: Zero-GC (New Object) -> `EngineKernel kernel = new EngineKernel(dispatcher, vault);`
- **Línea 50**: Bloqueo I/O (System.out) -> `System.out.println("Engine started - Watch the Windows Resou...`
- **Línea 51**: Bloqueo I/O (System.out) -> `System.out.println("\nTo open Resource Monitor:");`
- **Línea 52**: Bloqueo I/O (System.out) -> `System.out.println("  1. Press Win+R");`
- **Línea 53**: Bloqueo I/O (System.out) -> `System.out.println("  2. Type: perfmon /res");`
- **Línea 54**: Bloqueo I/O (System.out) -> `System.out.println("  3. Go to the 'CPU' and 'Memory' tabs\n...`
- **Línea 56**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 57**: Bloqueo I/O (System.out) -> `System.out.println("EXPECTED SCALING:");`
- **Línea 58**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 59**: Bloqueo I/O (System.out) -> `System.out.println("  Tier 0 (Active):          CPU ~100% on...`
- **Línea 59**: Aritmetica Modulo (Potencial) -> `System.out.println("  Tier 0 (Active):          CPU ~100% on...`
- **Línea 60**: Bloqueo I/O (System.out) -> `System.out.println("  Tier 1 (Spin Wait):       CPU ~50-70% ...`
- **Línea 60**: Aritmetica Modulo (Potencial) -> `System.out.println("  Tier 1 (Spin Wait):       CPU ~50-70% ...`
- **Línea 61**: Bloqueo I/O (System.out) -> `System.out.println("  Tier 2 (Light Sleep):     CPU ~5-10% a...`
- **Línea 61**: Aritmetica Modulo (Potencial) -> `System.out.println("  Tier 2 (Light Sleep):     CPU ~5-10% a...`
- **Línea 62**: Bloqueo I/O (System.out) -> `System.out.println("  Tier 3 (Deep Hibernation): CPU ~0-1% a...`
- **Línea 62**: Aritmetica Modulo (Potencial) -> `System.out.println("  Tier 3 (Deep Hibernation): CPU ~0-1% a...`
- **Línea 63**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 65**: Bloqueo I/O (System.out) -> `System.out.println("[INFO] The engine has NO events, so it w...`
- **Línea 66**: Bloqueo I/O (System.out) -> `System.out.println("[INFO] Watch how the CPU consumption dro...`
- **Línea 68**: Bloqueo I/O (System.out) -> `System.out.println("Press Ctrl+C to terminate (will execute ...`
- **Línea 71**: Zero-GC (New Object) -> `Thread timer = new Thread(() -> {`
- **Línea 74**: Bloqueo I/O (System.out) -> `System.out.println("\n[AUTOMATED TEST] 2 seconds elapsed. Tr...`

### [Reading Order: NO_ORDER] Archivo: `sv/dark/test/SummaryGenerator.java`
- **Línea 22**: Zero-GC (New Object) -> `try (BufferedReader br = new BufferedReader(new FileReader(l...`
- **Línea 37**: Zero-GC (String Concat) -> `bootSequence = val + " ms";`
- **Línea 57**: Zero-GC (String Concat) -> `System.err.println("[SummaryGenerator] Error reading " + log...`
- **Línea 57**: Bloqueo I/O (System.out) -> `System.err.println("[SummaryGenerator] Error reading " + log...`
- **Línea 60**: Bloqueo I/O (System.out) -> `System.out.println("\n======================================...`
- **Línea 61**: Bloqueo I/O (System.out) -> `System.out.println("                   AAA+ DEVELOPMENT METR...`
- **Línea 62**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`
- **Línea 63**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "METRIC", "VALUE");`
- **Línea 63**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "METRIC", "VALUE");`
- **Línea 64**: Bloqueo I/O (System.out) -> `System.out.println("----------------------------------------...`
- **Línea 65**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "SIMD Data Accelerator...`
- **Línea 65**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "SIMD Data Accelerator...`
- **Línea 66**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "Atomic Bus Latency", ...`
- **Línea 66**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "Atomic Bus Latency", ...`
- **Línea 67**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "Event Throughput", ev...`
- **Línea 67**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "Event Throughput", ev...`
- **Línea 68**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "Boot Sequence Time", ...`
- **Línea 68**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "Boot Sequence Time", ...`
- **Línea 69**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "OS Cleanup / Memory S...`
- **Línea 69**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "OS Cleanup / Memory S...`
- **Línea 70**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "Engine Power Governor...`
- **Línea 70**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "Engine Power Governor...`
- **Línea 71**: Zero-GC (String Concat) -> `System.out.printf(" %-30s | %-30s\n", "AAA+ Tests Passed", t...`
- **Línea 71**: Bloqueo I/O (System.out) -> `System.out.printf(" %-30s | %-30s\n", "AAA+ Tests Passed", t...`
- **Línea 71**: Aritmetica Modulo (Potencial) -> `System.out.printf(" %-30s | %-30s\n", "AAA+ Tests Passed", t...`
- **Línea 72**: Bloqueo I/O (System.out) -> `System.out.println("========================================...`

