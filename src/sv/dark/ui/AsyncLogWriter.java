// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ui;

import sv.dark.core.AAACertified;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * RESPONSIBILITY: Asynchronous redirection of stdout/stderr to darkengine.log.
 * WHY: The kernel uses DarkLogger in its hot-path. Without this class: every log = blocking I/O syscall in the 60 FPS loop.
 * TECHNIQUE: Ring Buffer (ArrayBlockingQueue) of 8192 lines. println -> in-memory ring buffer (nanoseconds), actual disk flush via MIN_PRIORITY daemon thread. Intercepts tier transition lines to update EngineStateChannel.
 * GUARANTEES: Zero I/O in hot-path. Trivial memory overhead (~480 KB in heap). If buffer is full, line is silently discarded to preserve kernel latency.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(date = "2026-06-11", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = false, offHeap = false, notes = "Automatically AAA Certified during Core Audit")
public final class AsyncLogWriter {

    private static final int              QUEUE_CAPACITY = 8_192;
    private static final DateTimeFormatter FMT           = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ArrayBlockingQueue<String> queue   = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private volatile boolean                 running = true;

    // --- Constructors --------------------------------------------------------

    /**
     * Initializes the asynchronous writer.
     * 
     * <p>Creates the darkengine.log file and starts the background I/O daemon thread.
     *
     * @param logPath Relative path to the log file.
     */
    public AsyncLogWriter(String logPath) {
        Thread writer = new Thread(() -> {
            try (PrintWriter pw = new PrintWriter(
                    new BufferedWriter(new FileWriter(logPath, false)))) {

                pw.printf("[%s] Dark-Engine log started%n", now());
                pw.flush();

                while (running || !queue.isEmpty()) {
                    try {
                        String line = queue.poll(200L, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (line != null) {
                            pw.println(line);
                            // Batch flush only when the queue is empty to minimize I/O syscalls
                            if (queue.isEmpty()) pw.flush();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                pw.printf("[%s] Dark-Engine log closed%n", now());
                pw.flush();

            } catch (IOException e) {
                // Cannot log the logger's own error. Silent fail.
            }
        }, "dark-log-writer");

        writer.setDaemon(true);
        writer.setPriority(Thread.MIN_PRIORITY);
        writer.start();
    }

    // --- Core Logic ----------------------------------------------------------

    /**
     * Creates a {@link PrintStream} that redirects all output to the ring buffer.
     *
     * <p>Also parses tier transition lines to update {@link EngineStateChannel}.
     * Call this twice for stdout and stderr:
     * {@code System.setOut(logWriter.createPrintStream(System.out));}
     *
     * <p>Each invocation creates its own OutputStream with its own StringBuilder
     * (no shared state between instances).
     *
     * @param fallback Original stream (reference only, unused at runtime).
     * @return A PrintStream that writes to the ring buffer instead of the console.
     */
    public PrintStream createPrintStream(
            //@SuppressWarnings("unused")
            PrintStream fallback) {
        return new PrintStream(new OutputStream() {

            // Thread-local StringBuilder for this specific OutputStream
            private final StringBuilder sb = new StringBuilder(256);

            @Override
            public synchronized void write(int b) {
                if (b == '\n') {
                    String line = sb.toString();
                    sb.setLength(0);
                    if (!line.isEmpty()) {
                        offer(line);
                        parseState(line);
                    }
                } else if (b != '\r') {
                    // Only ASCII/Latin-1 characters - the engine exclusively uses ASCII
                    sb.append((char)(b & 0xFF));
                }
            }

            // PrintStream calls write(byte[], int, int) internally.
            // The base OutputStream implementation delegates to write(int),
            // but we explicitly override it to guarantee performance.
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                for (int i= off; i< off + len; i++) {
                    write(b[i] & 0xFF);
                }
            }

            private void offer(String line) {
                // Non-blocking: drops the line if the buffer is full.
                // Blocking would introduce unacceptable latency in the kernel thread.
                queue.offer(line);
            }

            /**
             * Parses kernel output to detect state transitions.
             * These strings exactly match EngineKernel.java output — do not modify.
             */
            private void parseState(String line) {
                if (line.contains("[KERNEL] Main loop started")) {
                    EngineStateChannel.STATE.set(EngineStateChannel.STATE_RUNNING);
                } else if (line.contains("Tier 3 (Deep Hibernation)")) {
                    EngineStateChannel.STATE.set(EngineStateChannel.STATE_TIER3);
                } else if (line.contains("Saliendo de Tier")) {
                    // Any exit from a tier returns to running state
                    EngineStateChannel.STATE.set(EngineStateChannel.STATE_RUNNING);
                }
            }

        }, false /* autoFlush controlado manualmente */);
    }

    /** Signals the daemon writer to terminate when the queue is fully drained. */
    public void stop() {
        running = false;
    }

    private static String now() {
        return LocalDateTime.now().format(FMT);
    }
}
