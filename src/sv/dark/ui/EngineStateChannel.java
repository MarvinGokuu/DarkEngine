package sv.dark.ui;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Canal de estado lock-free entre kernel y capa visual.
 * TÉCNICA: AtomicInteger — cero locks, cero contención, cero allocations.
 *
 * El kernel ESCRIBE el estado via AsyncLogWriter (intercepta stdout).
 * La capa visual SOLO LEE — nunca bloquea el kernel thread.
 *
 * Estados:
 *   STATE_BOOT    (0) — Kernel arrancando, ventana muestra splash
 *   STATE_RUNNING (1) — [KERNEL] Main loop started — ventana activa
 *   STATE_TIER3   (2) — Deep Hibernation — pantalla negra con título centrado
 *
 * @author Marvin-Dev
 */
public final class EngineStateChannel {

    public static final int STATE_BOOT    = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_TIER3   = 2;

    /**
     * Estado actual del engine. Escrito por AsyncLogWriter (al parsear stdout),
     * leído por DarkEngineWindow. AtomicInteger garantiza visibilidad entre threads
     * sin locks ni barreras de memoria explícitas.
     */
    public static final AtomicInteger STATE = new AtomicInteger(STATE_BOOT);

    private EngineStateChannel() {}
}
