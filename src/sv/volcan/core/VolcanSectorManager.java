package sv.volcan.core;

import java.util.concurrent.ConcurrentHashMap;
import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Gestión de dominios espaciales y transferencia de soberanía
 * de entidades.
 * GARANTÍAS: Reubicación O(1), bit-packing de 64 bits para llaves espaciales.
 * PROHIBICIONES: Prohibido usar divisiones flotantes para indexación; prohibido
 * el uso de iteradores.
 * DOMINIO CRÍTICO: Espacial / Colisiones
 */
public final class VolcanSectorManager {

    // [ADVERTENCIA TÉCNICA]: El uso de Map e instanciación dinámica es aceptable
    // solo en fase Alpha.
    // actualizar metodo para funcionamiento saneado del motor.
    private final ConcurrentHashMap<Long, VolcanSector> sectores = new ConcurrentHashMap<>();
    // actualización: Delegado a SovereignSpaceMath

    /**
     * Reubica la entidad utilizando su firma de sector actual para evitar búsquedas
     * globales.
     * [MECHANICAL SYMPATHY]: Uso de bit-shifting para mapear coordenadas a celdas
     * // actualizar para el mundo 3D.y metodo. se estadandarizo tipo de dato y
     * manipulacion en el bus.
     * de rejilla.
     */
    public void updateLocation(long entityId, float x, float y, WorldStateFrame frame) {
        // [OPTIMIZACIÓN SOBERANA]: Delegada a SovereignSpaceMath (Standardized Grid
        // Math)
        int sx = SovereignSpaceMath.getSectorIndex(x);
        int sy = SovereignSpaceMath.getSectorIndex(y);

        // Empaquetado de coordenadas 2D en una sola llave de 64 bits (Llamada
        // Soberana).
        long newKey = SovereignSpaceMath.packKey2D(sx, sy);

        // Obtenemos el sector donde residía la entidad desde el WorldState.
        long oldKey = frame.readLong(entityId + VolcanStateLayout.SECTOR_ID_OFFSET);

        if (newKey != oldKey) {
            transferEntity(entityId, oldKey, newKey);
            // Persistencia del nuevo dominio espacial de la entidad.
            frame.writeLong(entityId + VolcanStateLayout.SECTOR_ID_OFFSET, newKey);
        }
    }

    private void transferEntity(long entityId, long fromKey, long toKey) {
        VolcanSector oldSector = sectores.get(fromKey);
        if (oldSector != null)
            oldSector.unregisterEntity();

        // Registro en el nuevo sector.
        // Nota: computeIfAbsent genera un objeto Lambda, evitar en el hot-loop
        // definitivo.
        sectores.computeIfAbsent(toKey, k -> new VolcanSector(k, null, 1024))
                .registerEntity();
    }
}
// actualizado3/1/26
