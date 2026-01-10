package sv.volcan.core;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Telemetría Visual Externa (Memory Mapped File).
 * DEPENDENCIAS: java.nio.MappedByteBuffer, FileChannel
 * MÉTRICAS: Zero-Overhead, IPC (Inter-Process Communication)
 * 
 * Proyecta el estado interno del frame a un archivo mapeado en memoria.
 * Permite que herramientas externas visualicen el estado en tiempo real sin
 * impactar el rendimiento.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanVisualObserver {

    private static final int LAYOUT_SIZE = 1024; // Espacio para 256 registros int (1KB)
    private ByteBuffer mappedBuffer;

    public VolcanVisualObserver() {
        try {
            // [HITO 1.1]: Creación del puente de telemetría mapeado directamente a la RAM
            Path path = Path.of("volcan_live.bin");
            try (FileChannel fc = FileChannel.open(path,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE)) {

                // Mapeo directo: El SO sincroniza este buffer con el archivo de forma
                // asíncrona.
                this.mappedBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, LAYOUT_SIZE);
            }
        } catch (Exception e) {
            // [PANIC]: Fallo crítico de observación. La soberanía del motor se mantiene,
            // pero la visibilidad externa se desactiva para proteger la ejecución.
        }
    }

    /**
     * Proyecta registros críticos al buffer mapeado.
     * Una herramienta externa (Dashboard/Sonda) leerá este archivo .bin
     * instantáneamente.
     * [MECHANICAL SYMPATHY]: Copia de bytes crudos sin overhead de serialización.
     */
    public void projectState(WorldStateFrame frame) {
        if (mappedBuffer == null)
            return;

        // 1. Latido de telemetría (Timestamp para cálculo de latencia externa)
        mappedBuffer.putLong(0, System.nanoTime());

        // 2. Proyección de Registros de Estado (Basado en el WorldStateLayout)
        // Ejemplo: Proyectamos la posición del Actor Soberano
        mappedBuffer.putInt(8, frame.readInt(400L)); // Registro 400L: PlayerX
        mappedBuffer.putInt(12, frame.readInt(404L)); // Registro 404L: PlayerY

        // [AUDITORÍA]: El archivo volcan_live.bin refleja el presente sin haber creado
        // un solo objeto String.
    }
}
// actualizado3/1/26