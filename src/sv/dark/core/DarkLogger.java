// Reading Order: 00010000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Unified Telemetry Framework (AAA+ Logger).
 * 
 * Replaces direct System.out.println calls across the engine.
 * Ensures absolute terminal silence for nominal operations,
 * routing all metrics and state changes to darkengine.log.
 * 
 * Only ERROR, WARN and FATAL severities are allowed to break terminal silence.
 */
public final class DarkLogger {
    private static final String LOG_DIR = "logs";
    private static final String METRICS_FILE = LOG_DIR + File.separator + "darkengine_metrics.log";
    private static final String ERRORS_FILE = LOG_DIR + File.separator + "darkengine_errors.log";
    
    private static PrintWriter metricsWriter;
    private static PrintWriter errorsWriter;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static int errorCount = 0;

    static {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // Overwrite mode for clean runs
            metricsWriter = new PrintWriter(new FileWriter(METRICS_FILE, false), true);
            errorsWriter = new PrintWriter(new FileWriter(ERRORS_FILE, false), true);
            
            metricsWriter.println("========================================================");
            metricsWriter.println(" DARK ENGINE METRICS SUBSYSTEM INITIALIZED");
            metricsWriter.println("========================================================\n");
            
            errorsWriter.println("========================================================");
            errorsWriter.println(" DARK ENGINE ERROR TRACKING INITIALIZED");
            errorsWriter.println("========================================================\n");
        } catch (IOException e) {
            System.err.println("[DARK LOGGER FATAL] Could not initialize telemetry file.");
        }
    }

    private static String formatMessage(String level, String component, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("[%s] [%-5s] [%-15s] %s", timestamp, level, component, message);
    }

    /**
     * Standard telemetry. Written ONLY to metrics log.
     */
    public static void info(String component, String message) {
        String formatted = formatMessage("INFO", component, message);
        if (metricsWriter != null) metricsWriter.println(formatted);
    }

    /**
     * Diagnostic data. Written ONLY to metrics log.
     */
    public static void debug(String component, String message) {
        String formatted = formatMessage("DEBUG", component, message);
        if (metricsWriter != null) metricsWriter.println(formatted);
    }

    /**
     * Warning events. Written to errors log.
     */
    public static void warning(String component, String message) {
        String formatted = formatMessage("WARN", component, message);
        if (errorsWriter != null) errorsWriter.println(formatted);
        errorCount++;
    }

    /**
     * Critical errors. Written ONLY to errors log to maintain terminal silence.
     */
    public static void error(String component, String message) {
        String formatted = formatMessage("ERROR", component, message);
        if (errorsWriter != null) errorsWriter.println(formatted);
        errorCount++;
    }

    /**
     * Fatal crashes. Dumps stack trace to errors log.
     */
    public static void fatal(String component, String message, Throwable t) {
        String formatted = formatMessage("FATAL", component, message);
        if (errorsWriter != null) {
            errorsWriter.println(formatted);
            if (t != null) {
                t.printStackTrace(errorsWriter);
            }
        }
        errorCount++;
        System.exit(1);
    }
    
    /**
     * Prints final summary to terminal.
     */
    public static void printFinalSummary() {
        if (errorCount > 0) {
            System.err.println("[DARK LOGGER] Execution finished with " + errorCount + " errors/warnings. Check logs/darkengine_errors.log");
        }
    }

    /**
     * Safely flushes and closes all native I/O streams.
     * Prevents metric loss during asynchronous shutdown.
     */
    public static void flushAndClose() {
        if (metricsWriter != null) {
            metricsWriter.flush();
            metricsWriter.close();
            metricsWriter = null;
        }
        if (errorsWriter != null) {
            errorsWriter.flush();
            errorsWriter.close();
            errorsWriter = null;
        }
    }
}
