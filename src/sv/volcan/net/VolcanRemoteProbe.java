package sv.volcan.net;

import com.sun.management.OperatingSystemMXBean;
import java.io.DataOutputStream;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import sv.volcan.bus.VolcanSignalPacker;
import sv.volcan.state.VolcanStateLayout;

/**
 * AUTORIDAD: Volcan (Fase 4.1)
 * RESPONSABILIDAD: Agente de telemetría remota de ultra-baja latencia.
 * GARANTÍAS: Conexión persistente, empaquetado binario de 64 bits,
 * Zero-Allocation.
 * PROHIBICIONES: Prohibido usar JSON/XML para transporte; prohibido crear
 * Strings en el loop de envío.
 * DOMINIO CRÍTICO: Telemetría Distribuidora
 */
public final class VolcanRemoteProbe {

    private static final OperatingSystemMXBean OS_BEAN = (OperatingSystemMXBean) ManagementFactory
            .getOperatingSystemMXBean();

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = 9999;

        System.out.println("[VOLCAN-PROBE] Estableciendo enlace soberano con " + host + ":" + port);

        // Bucle de persistencia de enlace
        while (!Thread.currentThread().isInterrupted()) {

            try (Socket socket = new Socket(host, port);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                // [MECHANICAL SYMPATHY]: Desactivar Nagle para despacho inmediato de señales
                socket.setTcpNoDelay(true);

                System.out.println("[VOLCAN-PROBE] Enlace activo. Transmitiendo...");

                while (true) {
                    // 1. Captura de métricas crudas (Hardware Layer)
                    int cpu = (int) (OS_BEAN.getCpuLoad() * 10000); // 0-100.00%
                    int ram = (int) (OS_BEAN.getFreeMemorySize() / 1048576); // MB

                    // 2. Empaquetado Binario (Soberanía de Datos)
                    // El Packer une el ID de la métrica y el valor en un solo 'long' de 64 bits.
                    out.writeLong(VolcanSignalPacker.pack(VolcanStateLayout.SYS_CPU_LOAD, cpu));
                    out.writeLong(VolcanSignalPacker.pack(VolcanStateLayout.SYS_MEM_FREE, ram));

                    // Forzar el despacho al buffer de red
                    out.flush();

                    // 3. Latido sincronizado (Frecuencia: 1Hz para telemetría de sistema)
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                System.err.println("[VOLCAN-PROBE] Enlace perdido. Reintentando en 5s...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
// actualizado3/1/26