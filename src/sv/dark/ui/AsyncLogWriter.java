package sv.dark.ui;

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
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Redirección async de stdout/stderr → darkengine.log.
 *
 * SIMPATÍA MECÁNICA:
 *   - El kernel escribe System.out.println() en su hot-path.
 *   - Sin esta clase: cada println = syscall bloqueante de I/O en el loop de 60 FPS.
 *   - Con esta clase: println → ring buffer en memoria (nanosegundos), zero I/O en hot-path.
 *   - Flush real a disco: daemon thread de prioridad MIN_PRIORITY, nunca en kernel thread.
 *
 * RING BUFFER: ArrayBlockingQueue de 8192 líneas.
 *   - Si se llena: línea descartada silenciosamente (kernel latency preserved).
 *   - Capacity: 8192 líneas × ~60 chars avg = ~480 KB en heap. Trivial.
 *
 * PARSEO DE ESTADO: intercepta las líneas de tier transition para actualizar
 *   EngineStateChannel sin que el kernel lo sepa ni le cueste nada.
 *
 * @author Marvin-Dev
 */
public final class AsyncLogWriter {

    private static final int              QUEUE_CAPACITY = 8_192;
    private static final DateTimeFormatter FMT           = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ArrayBlockingQueue<String> queue   = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private volatile boolean                 running = true;

    /**
     * Inicia el writer. Crea el archivo darkengine.log y lanza el daemon thread de escritura.
     *
     * @param logPath ruta del archivo de log (relativa al directorio de trabajo)
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
                            // Batch flush: solo cuando la queue quedó vacía para minimizar I/O
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
                // No se puede loguear el error del logger. Silent fail.
            }
        }, "dark-log-writer");

        writer.setDaemon(true);
        writer.setPriority(Thread.MIN_PRIORITY);
        writer.start();
    }

    /**
     * Crea un PrintStream que redirige toda la salida al ring buffer.
     * También parsea las líneas de transición de tier para actualizar EngineStateChannel.
     *
     * Llamar dos veces para stdout y stderr:
     *   System.setOut(logWriter.createPrintStream(System.out));
     *   System.setErr(logWriter.createPrintStream(System.err));
     *
     * Cada llamada crea su propio OutputStream con su propio StringBuilder
     * (sin estado compartido entre las dos instancias).
     *
     * @param fallback stream original (solo para referencia, no se usa en runtime)
     * @return PrintStream que escribe al ring buffer en lugar de a la consola
     */
    public PrintStream createPrintStream(
            //@SuppressWarnings("unused")
            PrintStream fallback) {
        return new PrintStream(new OutputStream() {

            // StringBuilder local a este OutputStream — no compartido
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
                    // Solo caracteres ASCII/Latin-1 — el engine usa exclusivamente ASCII
                    sb.append((char)(b & 0xFF));
                }
            }

            // PrintStream llama write(byte[], int, int) internamente.
            // La implementación base de OutputStream delega a write(int),
            // pero la sobreescribimos explícitamente para garantizar el comportamiento.
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                for (int i = off; i < off + len; i++) {
                    write(b[i] & 0xFF);
                }
            }

            private void offer(String line) {
                // Non-blocking: descarta la línea si el buffer está lleno.
                // La alternativa (block) introduciría latencia en el kernel thread.
                queue.offer(line);
            }

            /**
             * Parsea el output del kernel para detectar transiciones de estado.
             * Estas strings son exactamente lo que imprime EngineKernel.java — no modificar.
             */
            private void parseState(String line) {
                if (line.contains("[KERNEL] Main loop started")) {
                    EngineStateChannel.STATE.set(EngineStateChannel.STATE_RUNNING);
                } else if (line.contains("Tier 3 (Deep Hibernation)")) {
                    EngineStateChannel.STATE.set(EngineStateChannel.STATE_TIER3);
                } else if (line.contains("Saliendo de Tier")) {
                    // Cualquier salida de tier → volver a running
                    EngineStateChannel.STATE.set(EngineStateChannel.STATE_RUNNING);
                }
            }

        }, false /* autoFlush controlado manualmente */);
    }

    /** Señaliza al daemon writer que termine cuando el queue se vacíe. */
    public void stop() {
        running = false;
    }

    private static String now() {
        return LocalDateTime.now().format(FMT);
    }
}
