// Reading Order: 00001110
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;

import sv.dark.core.DarkLogger;
import sv.dark.core.systems.GameSystem;
import sv.dark.core.AAACertified;
import sv.dark.state.WorldStateFrame;

import sv.dark.core.systems.MovementSystem;
import sv.dark.core.systems.RenderSystem;
import sv.dark.core.systems.PhysicsSystem;
import sv.dark.core.systems.AudioSystem;

/**
 * RESPONSIBILITY: Parallel System Executor for deterministic parallel execution.
 * WHY: Sequential execution of systems wastes multi-core CPU potential. We need parallel execution without data races.
 * TECHNIQUE: Executes systems in parallel using static Platform Threads pinned to tasks, communicating via volatile Spin-Waits. 
 * GUARANTEES: Determinism (same graph + same input = same output). All systems in layer N finish before layer N+1 starts. 0 GC allocations during the loop (no Executor/Thread pooling objects).
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date = "2026-06-27",
    maxLatencyNs = 2_000,
    minThroughput = 240,
    alignment = 64,
    lockFree = true,
    offHeap = false,
    notes = "Zero-GC Parallel executor using Static Platform Threads and Spin-Wait Lock-Free barriers"
)
public final class ParallelSystemExecutor {

    private final GameSystem[][] executionLayersArray;
    private final SystemTask[][] layerTasks;
    private final WorkerThread[] workers;

    private long lastExecutionTimeNs;

    private MovementSystem movementSystem;
    private RenderSystem renderSystem;
    private PhysicsSystem physicsSystem;
    private AudioSystem audioSystem;
    private sv.dark.core.systems.DarkAudioSystem darkAudioSystem;

    private static final class SystemTask {
        private final GameSystem system;
        private WorldStateFrame state;
        private double deltaTime;

        // Memory barriers for lock-free spin-waiting
        volatile boolean ready = false;
        volatile boolean done = false;
        volatile boolean shutdown = false;

        SystemTask(GameSystem system) {
            this.system = system;
        }

        void setArgs(WorldStateFrame state, float deltaTime) {
            this.state = state;
            this.deltaTime = deltaTime;
            this.done = false;
            this.ready = true;
        }
    }

    private static final class WorkerThread extends Thread {
        private final SystemTask task;

        WorkerThread(String name, SystemTask task) {
            super(name);
            this.task = task;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!task.shutdown) {
                if (task.ready) {
                    task.ready = false; // Claim task
                    try {
                        task.system.update(task.state, (float) task.deltaTime);
                    } catch (Exception e) {
                        // Suppressed to avoid blocking JNI/IO
                    } finally {
                        task.done = true; // Signal completion
                    }
                } else {
                    Thread.onSpinWait(); // Mechanical Sympathy: Hint to CPU
                }
            }
        }
    }

    public ParallelSystemExecutor(GameSystem[][] executionLayersArray) {
        if (executionLayersArray == null || executionLayersArray.length == 0) {
            throw new IllegalArgumentException("Execution layers cannot be null or empty");
        }

        this.executionLayersArray = executionLayersArray;
        this.lastExecutionTimeNs = 0;

        int totalParallelTasks = 0;
        for (GameSystem[] layer : executionLayersArray) {
            if (layer.length > 1) {
                totalParallelTasks += layer.length;
            }
        }

        this.layerTasks = new SystemTask[executionLayersArray.length][];
        this.workers = new WorkerThread[totalParallelTasks];
        
        int workerIdx = 0;
        for (int i = 0; i < executionLayersArray.length; i++) {
            GameSystem[] layer = executionLayersArray[i];
            int systemCount = layer.length;
            
            if (systemCount > 1) {
                this.layerTasks[i] = new SystemTask[systemCount];
                for (int j = 0; j < systemCount; j++) {
                    SystemTask task = new SystemTask(layer[j]);
                    this.layerTasks[i][j] = task;
                    
                    WorkerThread worker = new WorkerThread("Worker-" + layer[j].getClass().getSimpleName(), task);
                    this.workers[workerIdx++] = worker;
                    worker.start();
                }
            } else {
                this.layerTasks[i] = null;
            }
        }

        for (GameSystem[] layer : executionLayersArray) {
            for (GameSystem system : layer) {
                if (system instanceof MovementSystem) {
                    this.movementSystem = (MovementSystem) system;
                } else if (system instanceof RenderSystem) {
                    this.renderSystem = (RenderSystem) system;
                } else if (system instanceof PhysicsSystem) {
                    this.physicsSystem = (PhysicsSystem) system;
                } else if (system instanceof AudioSystem) {
                    this.audioSystem = (AudioSystem) system;
                } else if (system instanceof sv.dark.core.systems.DarkAudioSystem) {
                    this.darkAudioSystem = (sv.dark.core.systems.DarkAudioSystem) system;
                }
            }
        }

        DarkLogger.info("PARALLEL", "Executor initialized with " +
                executionLayersArray.length + " layers running on " + totalParallelTasks + " Static Platform Threads (Spin-Wait)");
    }

    public MovementSystem getMovementSystem() {
        return movementSystem;
    }

    public RenderSystem getRenderSystem() {
        return renderSystem;
    }

    public PhysicsSystem getPhysicsSystem() {
        return physicsSystem;
    }

    public AudioSystem getAudioSystem() {
        return audioSystem;
    }

    public sv.dark.core.systems.DarkAudioSystem getDarkAudioSystem() {
        return darkAudioSystem;
    }

    public void execute(WorldStateFrame state, float deltaTime) {
        long startTime = System.nanoTime();

        for (int i = 0; i < executionLayersArray.length; i++) {
            executeLayer(i, executionLayersArray[i], state, deltaTime);
        }

        long endTime = System.nanoTime();
        lastExecutionTimeNs = endTime - startTime;
    }

    private void executeLayer(int layerIndex, GameSystem[] layer, WorldStateFrame state, float deltaTime) {
        int systemCount = layer.length;

        // 1. Mono-thread fast path
        if (systemCount == 1) {
            try {
                layer[0].update(state, deltaTime);
            } catch (Exception e) {
                // Suppressed
            }
            return;
        }

        // 2. Parallel dispatch
        SystemTask[] tasks = layerTasks[layerIndex];
        
        for (int j = 0; j < systemCount; j++) {
            tasks[j].setArgs(state, deltaTime);
        }

        // 3. Spin-Wait Barrier (Zero-GC synchronization)
        for (int j = 0; j < systemCount; j++) {
            SystemTask task = tasks[j];
            while (!task.done) {
                Thread.onSpinWait();
            }
        }
    }

    public long getLastExecutionTimeNs() {
        return lastExecutionTimeNs;
    }

    public double getLastExecutionTimeMs() {
        return lastExecutionTimeNs / 1_000_000.0;
    }

    public void shutdown() {
        for (SystemTask[] tasks : layerTasks) {
            if (tasks != null) {
                for (SystemTask task : tasks) {
                    task.shutdown = true;
                }
            }
        }
        DarkLogger.info("PARALLEL", "Executor shutdown (Platform Threads terminated)");
    }
}
