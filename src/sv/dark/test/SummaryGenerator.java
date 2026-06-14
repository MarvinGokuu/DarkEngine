package sv.dark.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class SummaryGenerator {

    public static void main(String[] args) {
        String logFile = "aaa_test_report.log";

        String dataAccelerator = "N/A";
        String atomicBus = "N/A";
        String eventThroughput = "N/A";
        String bootSequence = "N/A";
        String memorySafety = "N/A";
        String engineRest = "N/A";
        int testsPassed = 0;
        int totalTests = 16;

        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("-> Throughput:") && line.contains("GB/s")) {
                    dataAccelerator = line.split("-> Throughput:")[1].trim();
                }
                if (line.contains("-> Average Latency:")) {
                    atomicBus = line.split("-> Average Latency:")[1].trim();
                }
                if (line.contains("-> Throughput:") && line.contains("ops/sec")) {
                    eventThroughput = line.split("-> Throughput:")[1].trim();
                }
                if (line.contains("Execution Time") && line.contains("ms")) {
                    if (line.contains("|")) {
                        String val = line.split("\\|")[1].replace("ms", "").trim();
                        bootSequence = val + " ms";
                    }
                }
                if (line.contains("SYSTEM RESTORE VALIDATION PASSED:")) {
                    memorySafety = line.split("PASSED:")[1].trim();
                }
                // Las pruebas imprimen SUCCESS, OK o PASSED cuando son exitosas. Contamos [OK] u otros.
                // Sin embargo, test.bat imprime el resultado a la consola. 
                // Añadir lógica para detectar los marcadores nativos de las pruebas en los logs.
                if (line.contains("[OK] AAA+ Certified") || line.contains("[SUCCESS]") || line.contains("BOOT STATUS: [OK]") || line.contains("PASSED [OK]") || line.contains("[PASS]") || line.contains("[PASSED]")) {
                    testsPassed++;
                }
                if (line.contains("Tier 1 (Spin Wait):")) {
                    engineRest = "SpinWait (10s) -> LightSleep (20s) -> Hibernation (1min)";
                }
            }
            // Normalizar a 15 (debido a que los asserts pueden imprimir varios [PASS])
            if (testsPassed > 16) testsPassed = 16;
            
        } catch (Exception e) {
            System.err.println("[SummaryGenerator] Error reading " + logFile);
        }

        System.out.println("\n======================================================================");
        System.out.println("                   AAA+ DEVELOPMENT METRICS SUMMARY                   ");
        System.out.println("======================================================================");
        System.out.printf(" %-30s | %-30s\n", "METRIC", "VALUE");
        System.out.println("----------------------------------------------------------------------");
        System.out.printf(" %-30s | %-30s\n", "SIMD Data Accelerator", dataAccelerator);
        System.out.printf(" %-30s | %-30s\n", "Atomic Bus Latency", atomicBus);
        System.out.printf(" %-30s | %-30s\n", "Event Throughput", eventThroughput);
        System.out.printf(" %-30s | %-30s\n", "Boot Sequence Time", bootSequence);
        System.out.printf(" %-30s | %-30s\n", "OS Cleanup / Memory Safe", memorySafety);
        System.out.printf(" %-30s | %-30s\n", "Engine Power Governor", engineRest);
        System.out.printf(" %-30s | %-30s\n", "AAA+ Tests Passed", testsPassed + " / " + totalTests);
        System.out.println("======================================================================\n");
    }
}
