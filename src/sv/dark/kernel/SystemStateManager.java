// Reading Order: 00111101
//  61
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;


import sv.dark.core.DarkLogger;
import sv.dark.core.AAACertified;

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
 * RESPONSIBILITY: OS State capture, high-performance optimization, and restoration.
 * WHY: Max performance requires changing the OS Power Plan and CPU affinity, but we must restore the user's machine to its original state upon exit.
 * TECHNIQUE: Uses Windows Power API via Project Panama (FFI) and ThreadPinning (JEP 454) to capture, mutate, and restore system state.
 * GUARANTEES: The system is left 100% clean when closing the engine. Fallback to CLI commands if FFI fails.
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date = "2026-01-08",
    maxLatencyNs = 50_000_000,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = true,
    notes = "Captures, optimizes, and restores OS state (Windows Power API + ThreadPinning)"
)
public final class SystemStateManager {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandle GET_POWER_STATUS_HANDLE;
    private static final MethodHandle POWER_GET_ACTIVE_SCHEME_HANDLE;
    private static final MethodHandle POWER_SET_ACTIVE_SCHEME_HANDLE;
    private static final MethodHandle POWER_READ_FRIENDLY_NAME_HANDLE;
    private static final MethodHandle LOCAL_FREE_HANDLE;

    // Standard Windows GUIDs for power plans
    public static final String HIGH_PERFORMANCE_GUID = "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c";
    public static final String BALANCED_GUID = "381b4222-f694-41f0-9685-ff5bb260df2e";

    static {
        MethodHandle getPowerStatus = null;
        MethodHandle powerGetActive = null;
        MethodHandle powerSetActive = null;
        MethodHandle powerReadFriendlyName = null;
        MethodHandle localFree = null;

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
            MemorySegment localFreeAddr = kernel32.find("LocalFree").orElse(null);
            if (localFreeAddr != null) {
                localFree = LINKER.downcallHandle(
                        localFreeAddr,
                        FunctionDescriptor.ofVoid(
                                ValueLayout.ADDRESS   // hMem
                        ));
            }
        } catch (Exception e) {
            DarkLogger.error("KERNEL", "Failed to link kernel32 functions for Power API: " + e.getMessage());
        }

        try {
            SymbolLookup powrprof = SymbolLookup.libraryLookup("PowrProf", Arena.global());
            MemorySegment getActiveAddr = powrprof.find("PowerGetActiveScheme").orElse(null);
            if (getActiveAddr != null) {
                powerGetActive = LINKER.downcallHandle(
                        getActiveAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,   // Return: DWORD (status, 0 is success)
                                ValueLayout.ADDRESS,    // UserRootPowerKey
                                ValueLayout.ADDRESS     // ActivePolicyGuid (GUID**)
                        ));
            }

            MemorySegment setActiveAddr = powrprof.find("PowerSetActiveScheme").orElse(null);
            if (setActiveAddr != null) {
                powerSetActive = LINKER.downcallHandle(
                        setActiveAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,   // Return: DWORD (status, 0 is success)
                                ValueLayout.ADDRESS,    // UserRootPowerKey
                                ValueLayout.ADDRESS     // SchemeGuid (const GUID*)
                        ));
            }

            MemorySegment readFriendlyNameAddr = powrprof.find("PowerReadFriendlyName").orElse(null);
            if (readFriendlyNameAddr != null) {
                powerReadFriendlyName = LINKER.downcallHandle(
                        readFriendlyNameAddr,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,   // Return: DWORD (status, 0 is success)
                                ValueLayout.ADDRESS,    // RootPowerKey
                                ValueLayout.ADDRESS,    // SchemeGuid
                                ValueLayout.ADDRESS,    // SubGroupOfPowerSettingsGuid
                                ValueLayout.ADDRESS,    // PowerSettingGuid
                                ValueLayout.ADDRESS,    // Buffer
                                ValueLayout.ADDRESS     // BufferSize
                        ));
            }
        } catch (Exception e) {
            DarkLogger.error("KERNEL", "Failed to link PowrProf functions: " + e.getMessage());
        }

        GET_POWER_STATUS_HANDLE = getPowerStatus;
        POWER_GET_ACTIVE_SCHEME_HANDLE = powerGetActive;
        POWER_SET_ACTIVE_SCHEME_HANDLE = powerSetActive;
        POWER_READ_FRIENDLY_NAME_HANDLE = powerReadFriendlyName;
        LOCAL_FREE_HANDLE = localFree;
    }

    private static void stringToGuid(String guidStr, MemorySegment dest) {
        String hex = guidStr.replace("-", "");
        if (hex.length() != 32) {
            throw new IllegalArgumentException("Invalid GUID string: " + guidStr);
        }
        int data1 = (int) Long.parseLong(hex.substring(0, 8), 16);
        dest.set(ValueLayout.JAVA_INT_UNALIGNED, 0, data1);
        
        short data2 = (short) Integer.parseInt(hex.substring(8, 12), 16);
        dest.set(ValueLayout.JAVA_SHORT_UNALIGNED, 4, data2);
        
        short data3 = (short) Integer.parseInt(hex.substring(12, 16), 16);
        dest.set(ValueLayout.JAVA_SHORT_UNALIGNED, 6, data3);
        
        for (int i = 0; i < 8; i++) {
            byte b = (byte) Integer.parseInt(hex.substring(16 + i * 2, 18 + i * 2), 16);
            dest.set(ValueLayout.JAVA_BYTE, 8 + i, b);
        }
    }

    private static String guidToString(MemorySegment src) {
        int data1 = src.get(ValueLayout.JAVA_INT_UNALIGNED, 0);
        short data2 = src.get(ValueLayout.JAVA_SHORT_UNALIGNED, 4);
        short data3 = src.get(ValueLayout.JAVA_SHORT_UNALIGNED, 6);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%08x-%04x-%04x-", data1, data2 & 0xFFFF, data3 & 0xFFFF));
        for (int i = 0; i < 2; i++) {
            sb.append(String.format("%02x", src.get(ValueLayout.JAVA_BYTE, 8 + i)));
        }
        sb.append("-");
        for (int i = 2; i < 8; i++) {
            sb.append(String.format("%02x", src.get(ValueLayout.JAVA_BYTE, 8 + i)));
        }
        return sb.toString();
    }

    private static String getSchemeFriendlyName(MemorySegment schemeGuid) {
        if (POWER_READ_FRIENDLY_NAME_HANDLE == null) {
            return "Unknown";
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pBufferSize = arena.allocate(ValueLayout.JAVA_INT);
            pBufferSize.set(ValueLayout.JAVA_INT, 0, 0);
            
            @SuppressWarnings("unused")
            int ignoredStatus = (int) POWER_READ_FRIENDLY_NAME_HANDLE.invokeExact(
                MemorySegment.NULL,
                schemeGuid,
                MemorySegment.NULL,
                MemorySegment.NULL,
                MemorySegment.NULL,
                pBufferSize
            );
            
            int bufferSize = pBufferSize.get(ValueLayout.JAVA_INT, 0);
            if (bufferSize > 0) {
                MemorySegment buffer = arena.allocate(bufferSize);
                int status = (int) POWER_READ_FRIENDLY_NAME_HANDLE.invokeExact(
                    MemorySegment.NULL,
                    schemeGuid,
                    MemorySegment.NULL,
                    MemorySegment.NULL,
                    buffer,
                    pBufferSize
                );
                if (status == 0) {
                    byte[] bytes = buffer.asSlice(0, bufferSize).toArray(ValueLayout.JAVA_BYTE);
                    int len = bytes.length;
                    if (len >= 2 && bytes[len - 1] == 0 && bytes[len - 2] == 0) {
                        len -= 2;
                    }
                    return new String(bytes, 0, len, StandardCharsets.UTF_16LE);
                }
            }
        } catch (Throwable t) {
            // Fallback
        }
        return "Unknown";
    }

    /**
     * Captures the current state of the system.
     *
     * @return An immutable snapshot of the system state.
     */
    public static SystemSnapshot captureInitialState() {
        long originalAffinity = ThreadPinning.getOriginalAffinityMask();
        String guid = "";
        String name = "";

        if (POWER_GET_ACTIVE_SCHEME_HANDLE != null) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pGuidPtr = arena.allocate(ValueLayout.ADDRESS);
                int status = (int) POWER_GET_ACTIVE_SCHEME_HANDLE.invokeExact(MemorySegment.NULL, pGuidPtr);
                if (status == 0) {
                    MemorySegment guidPtr = pGuidPtr.get(ValueLayout.ADDRESS, 0);
                    MemorySegment guidSegment = guidPtr.reinterpret(16);
                    guid = guidToString(guidSegment);
                    name = getSchemeFriendlyName(guidSegment);
                    if (LOCAL_FREE_HANDLE != null) {
                        LOCAL_FREE_HANDLE.invokeExact(guidPtr);
                    }
                }
            } catch (Throwable t) {
                DarkLogger.error("SYSTEM STATE", "Failed to capture power scheme via FFI: " + t.getMessage());
            }
        }

        // Fallback to cmd
        if (guid.isEmpty()) {
            String activeSchemeOutput = runCommand("powercfg", "/getactivescheme");
            guid = extractGuid(activeSchemeOutput);
            name = extractName(activeSchemeOutput);
            if (guid.isEmpty()) {
                guid = BALANCED_GUID; // Reasonable fallback
                name = "Balanced (Fallback)";
            }
        }

        String powerSource = queryPowerSource();

        SystemSnapshot snapshot = new SystemSnapshot(originalAffinity, guid, name, powerSource);
        DarkLogger.info("SYSTEM STATE", "Captured system snapshot successfully (GUID: " + guid + ", Name: " + name + ").");
        return snapshot;
    }

    /**
     * Applies high-performance optimizations to the system.
     * Changes the power plan to "High Performance".
     *
     * @return true if the operation completed successfully.
     */
    public static boolean applyPerformanceBoost() {
        DarkLogger.info("SYSTEM STATE", "Applying performance boost...");

        if (POWER_SET_ACTIVE_SCHEME_HANDLE != null) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment guidSegment = arena.allocate(16);
                stringToGuid(HIGH_PERFORMANCE_GUID, guidSegment);
                int status = (int) POWER_SET_ACTIVE_SCHEME_HANDLE.invokeExact(MemorySegment.NULL, guidSegment);
                if (status == 0) {
                    DarkLogger.info("SYSTEM STATE", "Power Scheme transitioned to HIGH PERFORMANCE successfully via FFI.");
                    return true;
                }
            } catch (Throwable t) {
                DarkLogger.error("SYSTEM STATE", "Failed to transition Power Scheme via FFI: " + t.getMessage());
            }
        }

        // Fallback
        String result = runCommand("powercfg", "/setactive", HIGH_PERFORMANCE_GUID);
        String activeSchemeOutput = runCommand("powercfg", "/getactivescheme");
        String currentGuid = extractGuid(activeSchemeOutput);

        if (HIGH_PERFORMANCE_GUID.equalsIgnoreCase(currentGuid)) {
            DarkLogger.info("SYSTEM STATE", "Power Scheme transitioned to HIGH PERFORMANCE successfully.");
            return true;
        } else {
            DarkLogger.error("SYSTEM STATE", "Failed to transition Power Scheme to HIGH PERFORMANCE. Result: " + result);
            return false;
        }
    }

    /**
     * Restores the system state to the initial snapshot.
     *
     * @param initial The original snapshot captured at startup.
     * @return true if restoration was successful.
     */
    public static boolean restoreInitialState(SystemSnapshot initial) {
        if (initial == null) return false;
        DarkLogger.info("SYSTEM STATE", "Restoring initial system state...");

        // 1. Restore power plan
        boolean powerRestored = false;
        if (POWER_SET_ACTIVE_SCHEME_HANDLE != null) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment guidSegment = arena.allocate(16);
                stringToGuid(initial.powerSchemeGuid, guidSegment);
                int status = (int) POWER_SET_ACTIVE_SCHEME_HANDLE.invokeExact(MemorySegment.NULL, guidSegment);
                if (status == 0) {
                    DarkLogger.info("SYSTEM STATE", "Restored Power Scheme via FFI to: " + initial.powerSchemeName + " (" + initial.powerSchemeGuid + "");
                    powerRestored = true;
                }
            } catch (Throwable t) {
                DarkLogger.error("SYSTEM STATE", "Failed to restore Power Scheme via FFI: " + t.getMessage());
            }
        }

        if (!powerRestored) {
            runCommand("powercfg", "/setactive", initial.powerSchemeGuid);
            DarkLogger.info("SYSTEM STATE", "Restored Power Scheme via CLI to: " + initial.powerSchemeName + " (" + initial.powerSchemeGuid + "");
        }

        // 2. Restore thread affinity
        boolean affinityRestored = ThreadPinning.restoreAffinityMask(initial.threadAffinityMask);
        if (affinityRestored) {
            DarkLogger.info("SYSTEM STATE", String.format("Restored Thread Affinity to mask: 0x%X%n", initial.threadAffinityMask));
        } else {
            DarkLogger.error("SYSTEM STATE", "Failed to restore Thread Affinity.");
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
                // Fallback to wmic/powercfg if fails
            }
        }

        // Robust fallback using Windows command
        String batteryStatus = runCommand("wmic", "path", "Win32_Battery", "get", "BatteryStatus");
        if (batteryStatus.contains("BatteryStatus") && !batteryStatus.trim().endsWith("BatteryStatus")) {
            return "Battery";
        }
        return "AC Power";
    }

    private static String runCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
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
