package sv.volcan.bus;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Clasificación de eventos por dominio funcional.
 * GARANTÍAS: Separación de preocupaciones, permite lanes especializados.
 * DOMINIO CRÍTICO: Arquitectura / Organización
 * 
 * PATRÓN: Type-Safe Enum
 * CONCEPTO: Domain Segregation
 * ROL: Event Classification
 * 
 * @author MarvinDev
 * @version 2.0
 * @since 2026-01-04
 */
public enum VolcanEventType {

    /**
     * Eventos de entrada del usuario.
     * Ejemplos: Teclado, mouse, gamepad.
     */
    INPUT(0x1000),

    /**
     * Eventos de red.
     * Ejemplos: Paquetes recibidos, sincronización de estado.
     */
    NETWORK(0x2000),

    /**
     * Eventos de sistema.
     * Ejemplos: Spawn de entidades, cambios de estado del motor.
     */
    SYSTEM(0x3000),

    /**
     * Eventos de audio.
     * Ejemplos: Reproducir sonido, cambiar volumen.
     */
    AUDIO(0x4000),

    /**
     * Eventos de física.
     * Ejemplos: Colisiones, aplicar fuerzas.
     */
    PHYSICS(0x5000),

    /**
     * Eventos de renderizado.
     * Ejemplos: Cambiar shader, actualizar textura.
     */
    RENDER(0x6000);

    private final int baseId;

    VolcanEventType(int baseId) {
        this.baseId = baseId;
    }

    /**
     * Retorna el ID base del tipo de evento.
     * Usado para empaquetar eventos con tipo incluido.
     * 
     * @return ID base (16 bits superiores del command ID)
     */
    public int getBaseId() {
        return baseId;
    }

    /**
     * Extrae el tipo de evento desde un command ID.
     * 
     * @param commandId ID del comando (32 bits)
     * @return Tipo de evento correspondiente
     */
    public static VolcanEventType fromCommandId(int commandId) {
        int typeId = commandId & 0xF000;
        for (VolcanEventType type : values()) {
            if (type.baseId == typeId) {
                return type;
            }
        }
        return SYSTEM; // Default
    }
}
