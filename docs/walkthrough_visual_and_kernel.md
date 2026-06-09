# Walkthrough - Engine Visual Launch & Kernel Optimizations

We have integrated standard OS window controls and applied five core performance and reliability optimizations to the DarkEngine kernel.

---

## 1. Visual Layer Enhancements

### Centered Windowed Mode (900×520)
We configured [DarkEngineWindow.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/ui/DarkEngineWindow.java) to launch centered on the user's monitor with default dimensions of 900×520 (matching the design specifications of the HTML mockup).

### Standard OS Decorations
We set `frame.setUndecorated(false)` to enable the standard operating system window frame, enabling:
* **Minimize / Maximize** buttons natively.
* The standard **Close (X)** button which safely stops execution.

---

## 2. Core Kernel Optimizations

### 1. Fixed CPU Busy-Spin Loop (100% Core Load)
* **File**: [AdminController.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/admin/AdminController.java)
* **Change**: Corrected the empty queue check in the admin metrics consumer from `metric != 0` to `metric != -1L`. This prevents the thread from busy-spinning at 100% CPU on empty streams.

### 2. Resolved HttpServer Port and Thread Leaks
* **Files**: [AdminController.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/admin/AdminController.java) and [EngineKernel.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/kernel/EngineKernel.java)
* **Change**: Stored the metrics server instance in a static variable inside `AdminController` and implemented a `stopControlPlane()` method, which is invoked by the kernel's shutdown hook on shutdown. This releases port `8080` cleanly.

### 3. Deadlock Protection in `BLOCK` Backpressure
* **Files**: [DarkEventLane.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/bus/DarkEventLane.java) and [DarkRingBus.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/bus/DarkRingBus.java)
* **Change**: 
  * Added a thread interruption check to the spin-loop in `DarkEventLane.java`.
  * Added a lifecycle `closed` flag in `DarkRingBus.java` that throws `IllegalStateException` on `offer` when closed on shutdown, matching the behavior of `DarkAtomicBus`.

### 4. Absolute Testing Determinism
* **File**: [DarkParticleSystem.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/core/DarkParticleSystem.java)
* **Change**: Moved the seeded `Random` generator from a static class field to an instance field (`private final Random RNG`). This guarantees that multiple engine instances run within the same JVM start with identical, reproducible particle layouts.

### 5. Stutter Protection (Frame Slip Control)
* **File**: [TimeKeeper.java](file:///c:/Users/theca/Documents/GitHub/DarkEngine/src/sv/dark/kernel/TimeKeeper.java)
* **Change**: Added drift threshold checking to reset the frame timeline baseline if execution slips by more than 2 frames. This eliminates the post-lag high-speed catching up burst.
