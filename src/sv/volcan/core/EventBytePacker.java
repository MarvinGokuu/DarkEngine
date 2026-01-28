package sv.volcan.core; // Sincronizado con la ruta src/sv/volcan/core/

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Serialización/Deserialización binaria de alta velocidad
 * (Marshalling).
 * DEPENDENCIAS: MemorySegment, VarHandle, MemoryLayout
 * MÉTRICAS: Zero-Copy, Single-Instruction Encodings
 * 
 * Utilidad de bajo nivel para empaquetar datos de entidades en segmentos de
 * memoria.
 * Utiliza VarHandles estáticos para acceso directo a memoria (Off-Heap).
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class EventBytePacker {

    // [HARD ENGINEERING]: Estructura de datos inmutable para el mapeo de memoria.
    public static final MemoryLayout ENTITY_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("timestamp"), // 0-7 (Sincronía Temporal)
            ValueLayout.JAVA_DOUBLE.withName("posX"), // 8-15
            ValueLayout.JAVA_DOUBLE.withName("posY"), // 16-23
            ValueLayout.JAVA_INT.withName("id"), // 24-27
            ValueLayout.JAVA_FLOAT.withName("vel") // 28-31
    ).withName("EntityVector");

    private static final VarHandle TS_HANDLE = ENTITY_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("timestamp"));
    private static final VarHandle X_HANDLE = ENTITY_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("posX"));
    private static final VarHandle Y_HANDLE = ENTITY_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("posY"));
    private static final VarHandle ID_HANDLE = ENTITY_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("id"));

    public static final long STRIDE = ENTITY_LAYOUT.byteSize(); // 32 bytes exactos (Zero-Padding)

    private EventBytePacker() {
    } // Sellado: Utilidad de empaquetado puro.

    /**
     * Empaquetado quirúrgico. El JIT transforma esto en instrucciones MOV de 64
     * bits.
     * [HITO 1.1]: Acceso Off-Heap determinista.
     */
    public static void pack(MemorySegment segment, long index, int id, double x, double y, long ts) {
        // [MECHANICAL SYMPATHY]: Cálculo de offset base sin saltos de página.
        long offset = index * STRIDE;

        // Escritura atómica vía VarHandle (Garantía de visibilidad)
        TS_HANDLE.set(segment, offset, ts);
        X_HANDLE.set(segment, offset, x);
        Y_HANDLE.set(segment, offset, y);
        ID_HANDLE.set(segment, offset, id);
    }
}
// actualizado3/1/26