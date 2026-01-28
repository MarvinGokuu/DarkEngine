package sv.volcan.core; // Sincronizado con la ruta física real

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Motor de Paralelismo Work-Stealing (Divide & Conquer).
 * DEPENDENCIAS: java.util.concurrent.ForkJoinPool, MemorySegment
 * MÉTRICAS: 100% Core Utilization, Recursive Task Splitting
 * 
 * Implementación de paralelismo recursivo para procesamiento de entidades.
 * Divide dinámicamente la carga de trabajo entre núcleos usando el algoritmo
 * work-stealing.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class WorkStealingProcessor {

    private final ForkJoinPool pool;

    public WorkStealingProcessor(int parallelism) {
        // [CONFIGURACIÓN]: Modo Async optimizado para flujos de baja latencia.
        // El factory predeterminado es suficiente mientras no se requiera afinidad de
        // núcleo.
        this.pool = new ForkJoinPool(parallelism,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null, true);
    }

    /**
     * Inicia el procesamiento paralelo del frame actual.
     * Divide los sectores entre los hilos disponibles según demanda.
     */
    public void execute(MemorySegment[] sectors, double dt) {
        if (sectors == null || sectors.length == 0)
            return;
        pool.invoke(new SectorTask(sectors, 0, sectors.length, dt));
    }

    /**
     * Tarea recursiva que aplica el protocolo Divide & Conquer.
     * Permite que hilos ociosos "roben" sectores de hilos sobrecargados.
     */
    @SuppressWarnings("serial")
    private static class SectorTask extends RecursiveAction {
        private final MemorySegment[] sectors;
        private final int start, end;
        private final double dt;

        SectorTask(MemorySegment[] sectors, int start, int end, double dt) {
            this.sectors = sectors;
            this.start = start;
            this.end = end;
            this.dt = dt;
        }

        @Override
        protected void compute() {
            int length = end - start;

            // Unidad mínima de trabajo: Un sector completo (Preserva la localidad de
            // caché).
            if (length <= 1) {
                processSector(sectors[start], dt);
                return;
            }

            // División binaria de la carga de sectores.
            int mid = start + length / 2;
            SectorTask left = new SectorTask(sectors, start, mid, dt);
            SectorTask right = new SectorTask(sectors, mid, end, dt);

            // [HITO 4.5]: Bifurcación asíncrona.
            // 'left' se ejecuta localmente mientras 'right' queda disponible para ser
            // robado.
            invokeAll(left, right);
        }

        /**
         * Punto de inyección de lógica de sistemas sobre la memoria nativa.
         * Aquí el CPU opera a velocidad de registro sobre el segmento de memoria.
         */
        private void processSector(MemorySegment sector, double dt) {
            // [SISTEMAS]: Aquí se invocan MovementSystem.process(sector, dt)
            // Se garantiza que este hilo es el único con acceso exclusivo sobre este
            // segmento en
            // este instante.
        }
    }

    public void shutdown() {
        pool.shutdown();
    }
}
// actualizado3/1/26
