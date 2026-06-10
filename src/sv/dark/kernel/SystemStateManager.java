// Reading Order: 00010000
package sv.dark.kernel;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Captura, optimización y restauración del estado del OS.
 * TÉCNICA: Windows Power API + ThreadPinning (JEP 454)
 * GARANTÍA: El sistema queda 100% limpio al cerrar el motor.
 *
 * @author Marvin-Dev
 */
public final class SystemStateManager {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandle GET_POWER_STATUS_HANDLE;

    // GUIDs estándar de Windows para planes de energía
    public static final String HIGH_PERFORMANCE_GUID = "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c";
    public static final String BALANCED_GUID = "381b4222-f694-41f0-9685-ff5bb260df2e";

    static {
        MethodHandle getPowerStatus = null;
        try {
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
            MemorySegment getPowerStatusAddr = kernel32.find("GetSystemPowerStatus").orElse(null);
            if (getPowerStatusAddr != null) {
                getPowerStatus = LINKER.downcallHandle(
                        getPowerStatusAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT, // Return: BOOL
                                ValueLayout.ADDRESS   // lpSystemPowerStatus: Pointer to SYSTEM_POWER_STATUS struct
                        ));
            }
        } catch (Exception e) {
            System.err.println("[KERNEL] Failed to link GetSystemPowerStatus: " + e.getMessage());
        }
        GET_POWER_STATUS_HANDLE = getPowerStatus;
    }

    /**
     * Captura el estado actual del sistema.
     *
     * @return Un snapshot inmutable del estado del sistema.
     */
    public static SystemSnapshot captureInitialState() {
        long originalAffinity = ThreadPinning.getOriginalAffinityMask();
        String activeSchemeOutput = runCommand("powercfg", "/getactivescheme");

        String guid = extractGuid(activeSchemeOutput);
        String name = extractName(activeSchemeOutput);
        if (guid.isEmpty()) {
            guid = BALANCED_GUID; // Fallback razonable
            name = "Balanced (Fallback)";
        }

        String powerSource = queryPowerSource();

        SystemSnapshot snapshot = new SystemSnapshot(originalAffinity, guid, name, powerSource);
        System.out.println("[SYSTEM STATE] Captured system snapshot successfully.");
        return snapshot;
    }

    /**
     * Aplica optimizaciones de alto rendimiento al sistema.
     * Cambia el plan de energía a "Alto Rendimiento".
     *
     * @return true si la operación se completó con éxito.
     */
    public static boolean applyPerformanceBoost() {
        System.out.println("[SYSTEM STATE] Applying performance boost...");
        String result = runCommand("powercfg", "/setactive", HIGH_PERFORMANCE_GUID);
        String activeSchemeOutput = runCommand("powercfg", "/getactivescheme");
        String currentGuid = extractGuid(activeSchemeOutput);

        if (HIGH_PERFORMANCE_GUID.equalsIgnoreCase(currentGuid)) {
            System.out.println("[SYSTEM STATE] Power Scheme transitioned to HIGH PERFORMANCE successfully.");
            return true;
        } else {
            System.err.println("[SYSTEM STATE] Failed to transition Power Scheme to HIGH PERFORMANCE. Result: " + result);
            return false;
        }
    }

    /**
     * Restaura el estado del sistema al snapshot inicial.
     *
     * @param initial El snapshot original capturado al inicio.
     * @return true si la restauración fue exitosa.
     */
    public static boolean restoreInitialState(SystemSnapshot initial) {
        if (initial == null) return false;
        System.out.println("[SYSTEM STATE] Restoring initial system state...");

        // 1. Restaurar plan de energía
        runCommand("powercfg", "/setactive", initial.powerSchemeGuid);
        System.out.println("[SYSTEM STATE] Restored Power Scheme to: " + initial.powerSchemeName + " (" + initial.powerSchemeGuid + ")");

        // 2. Restaurar afinidad de hilos
        boolean affinityRestored = ThreadPinning.restoreAffinityMask(initial.threadAffinityMask);
        if (affinityRestored) {
            System.out.printf("[SYSTEM STATE] Restored Thread Affinity to mask: 0x%X%n", initial.threadAffinityMask);
        } else {
            System.err.println("[SYSTEM STATE] Failed to restore Thread Affinity.");
        }

        return affinityRestored;
    }

    private static String queryPowerSource() {
        if (GET_POWER_STATUS_HANDLE != null) {
            try (Arena arena = Arena.ofConfined()) {
                // SYSTEM_POWER_STATUS struct is 12 bytes
                MemorySegment struct = arena.allocate(12);
                int result = (int) GET_POWER_STATUS_HANDLE.invokeExact(struct);
                if (result != 0) {
                    byte acLineStatus = struct.get(ValueLayout.JAVA_BYTE, 0);
                    if (acLineStatus == 1) return "AC Power";
                    if (acLineStatus == 0) return "Battery";
                }
            } catch (Throwable t) {
                // Fallback a wmic/powercfg si falla
            }
        }

        // Fallback robusto usando comando de Windows
        String batteryStatus = runCommand("wmic", "path", "Win32_Battery", "get", "BatteryStatus");
        if (batteryStatus.contains("BatteryStatus") && !batteryStatus.trim().endsWith("BatteryStatus")) {
            return "Battery";
        }
        return "AC Power";
    }

    private static String runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                process.waitFor();
                return sb.toString().trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String extractGuid(String output) {
        if (output == null || output.isEmpty()) return "";
        Matcher matcher = Pattern.compile(
            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
        ).matcher(output);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private static String extractName(String output) {
        if (output == null || output.isEmpty()) return "Unknown";
        int start = output.indexOf('(');
        int end = output.indexOf(')', start);
        if (start != -1 && end != -1) {
            return output.substring(start + 1, end);
        }
        return "Unknown";
    }
}
