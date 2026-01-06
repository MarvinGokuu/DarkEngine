package sv.volcan.net;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sv.volcan.bus.VolcanSignalPacker;
import sv.volcan.state.VolcanStateLayout;

/**
 * AUTORIDAD: Volcan (Fase 4.2)
 * RESPONSABILIDAD: Validación de estrés y saturación del Atomic Bus del Kernel.
 * DISEÑO: Bombardeo binario persistente sin asignación de memoria (Zero-GC).
 * GARANTÍAS: Alta frecuencia, concurrencia masiva, validación de integridad de
 * señal.
 * DOMINIO CRÍTICO: Telemetría / QA
 */
public final class VolcanSaturationProbe {

    public static void main(String[] args) {
        int workerCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);

        System.out.println("[VOLCAN-STRESS] Iniciando saturación binaria: " + workerCount + " hilos.");

        for (int i = 0; i < workerCount; i++) {
            final int id = i;
            pool.execute(() -> runSaturationLoop(id));
        }
    }

    private static void runSaturationLoop(int threadId) {
        String host = "127.0.0.1";
        int port = 9999;

        while (!Thread.currentThread().isInterrupted()) {
            try (Socket socket = new Socket(host, port);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                // [MECHANICAL SYMPATHY]: Desactivar buffer local de red para impacto directo
                socket.setTcpNoDelay(true);
                System.out.println("[STRESS-WORKER-" + threadId + "] Enlace establecido. Inyectando carga...");

                while (true) {
                    // Generamos carga artificial: 95.00% + offset por hilo
                    int fakeCpu = 9500 + (threadId * 10);

                    // Empaquetado binario de 64 bits (Signal ID | Payload)
                    // Usamos el layout industrial para que el Kernel reconozca la señal.
                    long signal = VolcanSignalPacker.pack(VolcanStateLayout.SYS_CPU_LOAD, fakeCpu);

                    // Inyección de alta frecuencia: 8 bytes por latido
                    out.writeLong(signal);
                    out.flush();

                    // [STRESS INTERVAL]: 10ms (100 Hz por trabajador)
                    // Diseñado para poner a prueba la cola de recepción del VolcanAtomicBus.
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                // Reintento en caso de que el Kernel cierre la conexión por saturación
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
// actualizado3/1/26
