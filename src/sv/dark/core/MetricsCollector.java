package sv.dark.core;

import sv.dark.core.systems.MovementSystem;
import sv.dark.core.systems.RenderSystem;
import sv.dark.core.systems.PhysicsSystem;
import sv.dark.core.systems.AudioSystem;
import sv.dark.bus.IEventBus;

/**
 * MetricsCollector: Agregación de métricas OFF-CRITICAL-PATH
 * 
 * Responsabilidad:
 * - Recolectar métricas de sistemas independientes.
 * - Agregar sin contención (después de que los sistemas terminen).
 * - Sin impacto en la latencia de frame.
 * 
 * Perspectiva: Kernel Architect Level CEO
 */
public class MetricsCollector {
    
    /**
     * FrameMetrics: Contenedor para métricas agregadas de frame
     */
    public static class FrameMetrics {
        public long frameNumber = 0;
        public long frameTimeNs = 0;
        
        // Contadores por sistema (agregados)
        public int movementProcessed = 0;
        public int renderProcessed = 0;
        public int physicsProcessed = 0;
        public int audioProcessed = 0;
        
        // Latencias
        public long busLatencyNs = 0;
        public long systemsExecutionNs = 0;
        
        // Estadísticas
        public double avgFrameTimeMs = 0;
        public int droppedFrames = 0;
        
        @Override
        public String toString() {
            return String.format(
                "Frame[%d] Time: %.2f ms | Latency: %d ns | " +
                "Movement: %d | Render: %d | Physics: %d | Audio: %d",
                frameNumber,
                frameTimeNs / 1_000_000.0,
                busLatencyNs,
                movementProcessed,
                renderProcessed,
                physicsProcessed,
                audioProcessed
            );
        }
    }
    
    /**
     * aggregateMetrics: Llamada DESPUÉS de que los sistemas terminan
     * 
     * CRÍTICO: Esta función se ejecuta OFF-CRITICAL-PATH.
     * No está en el presupuesto de latencia crítica del frame.
     */
    public static void aggregateMetrics(
        MovementSystem movementSystem,
        RenderSystem renderSystem,
        PhysicsSystem physicsSystem,
        AudioSystem audioSystem,
        IEventBus eventBus,
        FrameMetrics output
    ) {
        // Leer métricas de cada sistema (SIN CONTENCIÓN)
        // Cada sistema ya terminó, no hay race conditions
        if (movementSystem != null) {
            output.movementProcessed = movementSystem.getProcessedCount();
        }
        if (renderSystem != null) {
            output.renderProcessed = renderSystem.getProcessedCount();
        }
        if (physicsSystem != null) {
            output.physicsProcessed = physicsSystem.getProcessedCount();
        }
        if (audioSystem != null) {
            output.audioProcessed = audioSystem.getProcessedCount();
        }
        
        // Agregar estadísticas de bus de forma segura
        output.busLatencyNs = (eventBus != null) ? eventBus.getLastLatencyNs() : 0L;
        
        // Calcular tiempo total del frame e interpolar con EMA (Exponential Moving Average)
        if (output.frameNumber > 0) {
            output.avgFrameTimeMs = 
                output.avgFrameTimeMs * 0.9 + 
                (output.frameTimeNs / 1_000_000.0) * 0.1;
        } else {
            output.avgFrameTimeMs = output.frameTimeNs / 1_000_000.0;
        }
        
        // Detectar frame drops
        if (output.frameTimeNs > 16_666_666) { // >16.67ms
            output.droppedFrames++;
        }
    }
    
    /**
     * Verifica si se deben recopilar métricas en el frame actual (ej. cada 60 frames).
     */
    public static boolean shouldCollectMetrics(long frameNumber) {
        return frameNumber % 60 == 0;
    }
}
