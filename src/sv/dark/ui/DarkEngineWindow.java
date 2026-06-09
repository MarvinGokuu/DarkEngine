package sv.dark.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.Random;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Capa visual del Dark-Engine — nebulosa interactiva.
 *
 * TECNOLOGÍA: AWT Canvas + BufferStrategy (doble buffer).
 *   java.desktop incluido en GraalVM 25 — cero dependencias externas.
 *
 * FIXES v2:
 *   - Fullscreen via MAXIMIZED_BOTH + setUndecorated (más confiable en Windows que setFullScreenWindow)
 *   - Fuentes escaladas dinámicamente según altura de pantalla (no hardcoded en px)
 *   - Subtítulo centrado con TextLayout.getAdvance() (medición exacta con tracking)
 *   - windowClosing: dispose + System.exit(0) → activa shutdown hook del kernel
 *   - BufferStrategy creado en render thread, después de que canvas tiene dimensiones
 *
 * SIMPATÍA MECÁNICA:
 *   Thread "dark-engine-kernel" [MAX_PRIORITY]  → kernel.start() → pinToCore(1)
 *   Thread "dark-ui-render"     [NORM-1]        → AnimationLoop, aislado
 *   Thread "dark-log-writer"    [MIN, daemon]   → flush ring buffer → log
 *   Comunicación: AtomicInteger (lock-free, cero contención)
 *
 * @author Marvin-Dev
 */
public final class DarkEngineWindow {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final long  FRAME_NS = 1_000_000_000L / 60;
    private static final int   PARTS    = 200;
    private static final float REPEL    = 95f;
    private static final String SUB_STR = "ZERO-OVERHEAD JAVA RUNTIME & MECHANICAL SYMPATHY";

    // ─── Colors (static, immutable) ───────────────────────────────────────────
    private static final Color C_TITLE  = new Color(255, 255, 255, 242);
    private static final Color C_VER    = new Color(255, 255, 255, 128);
    private static final Color C_SUB    = new Color(200,  80,  30, 217);
    private static final Color C_CURSOR = new Color(255, 160,  60, 230);
    private static final Color C_GLOW_O = new Color(255, 120,  30,  30);
    private static final Color C_GLOW_I = new Color(255, 120,  30,  60);

    // ─── AWT ──────────────────────────────────────────────────────────────────
    private final Frame  frame;
    private final Canvas canvas;

    // ─── Render state (render thread only) ───────────────────────────────────
    private volatile boolean running = true;
    private long  tick = 0;
    private float px   = 0, py = 0;

    // ─── Mouse (EDT escribe, render thread lee — volatile) ────────────────────
    private volatile float mx = 0, my = 0;
    private volatile float targetPX = 0, targetPY = 0;

    // ─── Cache por resolución (render thread only, rebuild on resize) ─────────
    private int cachedW = -1, cachedH = -1;
    private RadialGradientPaint gNebula1, gNebula2, gNebula3, gNebula4, gCore;

    // ─── Fuentes escaladas (instance, se crean al conocer H) ─────────────────
    private Font            fTitle, fVer, fSub;
    private AttributedString subAttr;
    private boolean          resourcesReady = false;

    // ─── Partículas — arrays primitivos (zero boxing, cache-friendly SoA) ─────
    private float[]   pX, pY, pVx, pVy, pSize, pAlpha, pSpeed, pAngle, pR, pDrift;
    private boolean[] pWarm;
    private final Random rng = new Random();

    // ─── Geometría reutilizable (evita allocations en hot-path) ──────────────
    private final Ellipse2D.Float ellipse = new Ellipse2D.Float();
    private final Line2D.Float    line    = new Line2D.Float();

    // ─────────────────────────────────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    /** Lanza la ventana en el EDT y retorna inmediatamente. */
    public static void launch() {
        EventQueue.invokeLater(() -> new DarkEngineWindow().start());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTOR (EDT)
    // ─────────────────────────────────────────────────────────────────────────

    private DarkEngineWindow() {
        frame = new Frame("Dark-Engine");
        try {
            java.net.URL iconUrl = DarkEngineWindow.class.getResource("/sv/dark/ui/darkengine_logo.png");
            if (iconUrl != null) {
                frame.setIconImage(Toolkit.getDefaultToolkit().getImage(iconUrl));
            }
        } catch (Exception e) {
            // Silently ignore if icon cannot be loaded
        }
        frame.setUndecorated(false);  // barra de título estándar del OS (minimizar, maximizar, cerrar)
        frame.setBackground(Color.BLACK);
        frame.setIgnoreRepaint(true); // usamos BufferStrategy, no el sistema AWT de pintura

        canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        frame.add(canvas, BorderLayout.CENTER);

        // ── Ventana centrada 900×520 (proporciones del HTML original) ──────
        java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int winW = 900, winH = 520;
        frame.setSize(winW, winH);
        frame.setLocation((screen.width - winW) / 2, (screen.height - winH) / 2);
        frame.setVisible(true);
        frame.validate(); // Fuerza layout pass: canvas obtiene dimensiones reales

        // ── Cursor del sistema: oculto (dibujamos el propio) ──────────────
        BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        canvas.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                blank, new Point(0, 0), "blank"));

        // ── Mouse events (EDT) ────────────────────────────────────────────
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved  (MouseEvent e) { onMouse(e); }
            @Override public void mouseDragged(MouseEvent e) { onMouse(e); }
            private void onMouse(MouseEvent e) {
                mx = e.getX(); my = e.getY();
                int W = canvas.getWidth(), H = canvas.getHeight();
                if (W > 0 && H > 0) {
                    targetPX = (mx / (float) W - 0.5f) * 18f;
                    targetPY = (my / (float) H - 0.5f) * 11f;
                }
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { targetPX = 0; targetPY = 0; }
        });

        // ── FIX: Window close — dispose + exit ───────────────────────────
        // System.exit(0) activa el shutdown hook del kernel (gracefulShutdown).
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false;
                frame.dispose();
                System.exit(0);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // START RENDER THREAD (EDT)
    // ─────────────────────────────────────────────────────────────────────────

    private void start() {
        // El BufferStrategy se crea en el render thread, después de verificar
        // que canvas tiene dimensiones. Esto evita el timing issue de EDT.
        Thread t = new Thread(this::renderLoop, "dark-ui-render");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER LOOP (render thread)
    // ─────────────────────────────────────────────────────────────────────────

    private void renderLoop() {
        // Esperar hasta que canvas tenga dimensiones reales
        while (canvas.getWidth() == 0 || canvas.getHeight() == 0) {
            try { Thread.sleep(10); } catch (InterruptedException e) { return; }
        }

        // Crear BufferStrategy aquí, cuando sabemos que canvas está listo
        canvas.createBufferStrategy(2);
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) return;

        while (running) {
            long t0 = System.nanoTime();
            tick++;

            int W = canvas.getWidth();
            int H = canvas.getHeight();

            // Rebuild en primer frame o en resize
            if (W != cachedW || H != cachedH) {
                buildFonts(H);         // escala fuentes según altura de pantalla
                buildGradients(W, H);  // rebuildea los 5 gradientes de nebulosa
                buildParticles(W, H);  // reinicia las 200 partículas
                cachedW = W; cachedH = H;
                resourcesReady = true;
            }

            // Parallax lerp (render thread only — sin concurrencia)
            px += (targetPX - px) * 0.055f;
            py += (targetPY - py) * 0.055f;

            // Render con BufferStrategy (doble buffer)
            do {
                do {
                    java.awt.Graphics2D g = null;
                    try {
                        g = (java.awt.Graphics2D) bs.getDrawGraphics();
                        renderFrame(g, W, H);
                    } finally {
                        if (g != null) g.dispose();
                    }
                } while (bs.contentsRestored());
                bs.show();
            } while (bs.contentsLost());

            // Fixed-timestep pacing
            long sleep = FRAME_NS - (System.nanoTime() - t0);
            if (sleep > 500_000L) {
                try { Thread.sleep(sleep / 1_000_000L, (int)(sleep % 1_000_000L)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER FRAME DISPATCH
    // ─────────────────────────────────────────────────────────────────────────

    private void renderFrame(java.awt.Graphics2D g, int W, int H) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);

        if (!resourcesReady) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, W, H);
            return;
        }

        if (EngineStateChannel.STATE.get() == EngineStateChannel.STATE_TIER3) {
            drawTier3(g, W, H);
        } else {
            drawScene(g, W, H);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TIER 3: Deep Hibernation — negro total + título centrado
    // ─────────────────────────────────────────────────────────────────────────

    private void drawTier3(java.awt.Graphics2D g, int W, int H) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        drawTitleBlock(g, W / 2, H / 2, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FULL SCENE
    // ─────────────────────────────────────────────────────────────────────────

    private void drawScene(java.awt.Graphics2D g, int W, int H) {
        float cx = W * 0.52f, cy = H * 0.44f;

        // 1. Fondo negro
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);

        // 2. Nebulosa (5 gradientes radiales, SRC_OVER)
        g.setPaint(gNebula1); g.fillRect(0, 0, W, H);
        g.setPaint(gNebula2); g.fillRect(0, 0, W, H);
        g.setPaint(gNebula3); g.fillRect(0, 0, W, H);
        g.setPaint(gNebula4); g.fillRect(0, 0, W, H);
        g.setPaint(gCore);    g.fillRect(0, 0, W, H);

        // 3. Rayos solares
        drawRays(g, cx, cy, W, H);

        // 4. Partículas orbitales
        drawParticles(g, cx, cy);

        // 5. Bloque de texto con parallax
        drawTitleBlock(g,
                (int)(W / 2f + px * 1.3f),
                (int)(H * 0.68f + py * 1.3f),
                true);

        // 6. Cursor dot naranja
        drawCursorDot(g);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RAYOS SOLARES
    // ─────────────────────────────────────────────────────────────────────────

    private void drawRays(java.awt.Graphics2D g, float cx, float cy, int W, int H) {
        float minD = Math.min(W, H);
        for (int i = 0; i < 8; i++) {
            float angle = (float)((i / 8.0) * Math.PI * 2 + tick * 0.0003);
            float len   = (0.14f + (i % 3) * 0.07f) * minD;
            float flick = 0.5f + (float)Math.sin(tick * 0.002 + i * 1.3) * 0.3f;
            float ex = cx + (float)Math.cos(angle) * len;
            float ey = cy + (float)Math.sin(angle) * len;

            g.setPaint(new LinearGradientPaint(cx, cy, ex, ey,
                    new float[]{0f, 1f},
                    new Color[]{
                        new Color(255, 195, 75, (int)(0.16f * flick * 255)),
                        new Color(255,  90,  8, 0)
                    }));
            g.setStroke(new BasicStroke(0.8f + rng.nextFloat() * 0.4f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            line.setLine(cx, cy, ex, ey);
            g.draw(line);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARTÍCULAS ORBITALES
    // ─────────────────────────────────────────────────────────────────────────

    private void drawParticles(java.awt.Graphics2D g, float cx, float cy) {
        float lmx = mx, lmy = my; // snapshot de volatiles: 1 lectura por frame

        for (int i = 0; i < PARTS; i++) {
            // Drift orbital
            pAngle[i] += pDrift[i];

            // Repulsión del cursor
            float dx = lmx - pX[i], dy = lmy - pY[i];
            float d2 = dx * dx + dy * dy;
            if (d2 < REPEL * REPEL && d2 > 0f) {
                float dist  = (float)Math.sqrt(d2);
                float force = (REPEL - dist) / REPEL;
                pVx[i] -= (dx / dist) * force * 1.6f;
                pVy[i] -= (dy / dist) * force * 1.6f;
            }

            // Spring back a órbita
            float tx = cx + (float)Math.cos(pAngle[i]) * pR[i];
            float ty = cy + (float)Math.sin(pAngle[i]) * pR[i];
            pVx[i] += (tx - pX[i]) * pSpeed[i];
            pVy[i] += (ty - pY[i]) * pSpeed[i];
            pVx[i] *= 0.87f; pVy[i] *= 0.87f;
            pX[i]  += pVx[i]; pY[i]  += pVy[i];

            // Flicker + color
            float flick = 0.65f + (float)Math.sin(tick * 0.004f + i) * 0.35f;
            int alpha = Math.max(0, Math.min(255, (int)(pAlpha[i] * flick * 255)));
            if (alpha < 2) continue;

            g.setColor(pWarm[i]
                ? new Color(255, 120 + (int)(rng.nextFloat()*60), 30 + (int)(rng.nextFloat()*30), alpha)
                : new Color(120, 190, 255, alpha));

            float s = pSize[i];
            ellipse.setFrame(pX[i]-s, pY[i]-s, s*2f, s*2f);
            g.fill(ellipse);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE DE TEXTO (título + versión + subtítulo)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dibuja el bloque centrado en (centerX, centerY).
     *
     * FIX: TextLayout.getAdvance() mide el ancho exacto incluyendo tracking,
     *      eliminando el centrado incorrecto de la versión anterior.
     *
     * @param showSubtitle false en Tier 3 (solo Dark-Engine + v0.1)
     */
    private void drawTitleBlock(java.awt.Graphics2D g, int centerX, int centerY, boolean showSubtitle) {

        // ── Medidas de fila 1 ─────────────────────────────────────────────
        g.setFont(fTitle);
        FontMetrics tfm = g.getFontMetrics();
        int titleW = tfm.stringWidth("Dark-Engine");

        g.setFont(fVer);
        FontMetrics vfm = g.getFontMetrics();
        int row1W = titleW + 10 + vfm.stringWidth("v0.1");

        // ── Altura total del bloque ───────────────────────────────────────
        FontMetrics sfm = showSubtitle ? g.getFontMetrics(fSub) : null;
        int blockH = tfm.getHeight() + (showSubtitle ? 8 + sfm.getHeight() : 0);

        // ── Baseline del título ───────────────────────────────────────────
        int row1X          = centerX - row1W / 2;
        int titleBaselineY = centerY - blockH / 2 + tfm.getAscent();

        // ── Fila 1: "Dark-Engine" ─────────────────────────────────────────
        g.setFont(fTitle);
        g.setColor(C_TITLE);
        g.drawString("Dark-Engine", row1X, titleBaselineY);

        // ── Fila 1: "v0.1" (baseline-aligned, margin-bottom 4px) ─────────
        g.setFont(fVer);
        g.setColor(C_VER);
        g.drawString("v0.1", row1X + titleW + 10, titleBaselineY - 4);

        // ── Fila 2: Subtítulo (solo en modo normal) ───────────────────────
        if (showSubtitle && subAttr != null) {
            // TextLayout mide el ancho real incluyendo tracking — centrado exacto
            TextLayout layout = new TextLayout(subAttr.getIterator(), g.getFontRenderContext());
            int subX = centerX - (int)(layout.getAdvance() / 2);
            int subY = titleBaselineY + tfm.getDescent() + 8 + sfm.getAscent();
            g.drawString(subAttr.getIterator(), subX, subY);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURSOR DOT
    // ─────────────────────────────────────────────────────────────────────────

    private void drawCursorDot(java.awt.Graphics2D g) {
        g.setColor(C_GLOW_O);
        ellipse.setFrame(mx-10f, my-10f, 20f, 20f);
        g.fill(ellipse);
        g.setColor(C_GLOW_I);
        ellipse.setFrame(mx-6f, my-6f, 12f, 12f);
        g.fill(ellipse);
        g.setColor(C_CURSOR);
        ellipse.setFrame(mx-2.5f, my-2.5f, 5f, 5f);
        g.fill(ellipse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD: FUENTES (escaladas por altura de pantalla)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * FIX: Fuentes hardcoded en 52/13/11 px eran correctas para el HTML de 520px.
     * En pantalla completa (768-1440px) se veían diminutas.
     *
     * Ahora escalan con H:
     *   H=768  → title≈70px, ver≈14px, sub≈14px
     *   H=1080 → title≈98px, ver≈19px, sub≈16px
     */
    private void buildFonts(int H) {
        int sTitle = Math.max(52, Math.min(120, H / 11));
        int sVer   = Math.max(13, H / 55);
        int sSub   = Math.max(13, H / 65);

        Font candidate = new Font("Segoe UI Light", Font.PLAIN, sTitle);
        fTitle = candidate.getFamily().equalsIgnoreCase("Segoe UI Light")
               ? candidate : new Font("SansSerif", Font.PLAIN, sTitle);
        fVer   = new Font("SansSerif", Font.PLAIN, sVer);
        fSub   = new Font("SansSerif", Font.PLAIN, sSub);

        // Subtítulo con tracking — rebuild cuando cambia fSub
        subAttr = new AttributedString(SUB_STR);
        subAttr.addAttribute(TextAttribute.FONT,       fSub);
        subAttr.addAttribute(TextAttribute.TRACKING,   0.30f);
        subAttr.addAttribute(TextAttribute.FOREGROUND, C_SUB);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD: GRADIENTES DE NEBULOSA (cacheados por resolución)
    // ─────────────────────────────────────────────────────────────────────────

    private void buildGradients(int W, int H) {
        float cx = W * 0.52f, cy = H * 0.44f;

        // Traduce directamente createRadialGradient() del JS original
        // rgba(r,g,b,a) → new Color(r,g,b, (int)(a*255))

        gNebula1 = radial(cx, cy, H*0.40f,
            new float[]{0f,.25f,.6f,1f},
            new Color[]{new Color(255,170, 50, 41), new Color(210, 65, 10, 51),
                        new Color(130, 18,  4, 31), new Color(  0,  0,  0,  0)});

        gNebula2 = radial(cx+W*0.17f, cy-H*0.20f, H*0.26f,
            new float[]{0f,.5f,1f},
            new Color[]{new Color(190, 28,  8, 41), new Color(150, 12,  4, 20),
                        new Color(  0,  0,  0,  0)});

        gNebula3 = radial(cx-W*0.19f, cy+H*0.16f, H*0.28f,
            new float[]{0f,.5f,1f},
            new Color[]{new Color(170, 18,  4, 36), new Color(110,  8,  2, 18),
                        new Color(  0,  0,  0,  0)});

        gNebula4 = radial(cx, cy, H*0.10f,
            new float[]{0f,.18f,.55f,1f},
            new Color[]{new Color(255,245,190,224), new Color(255,190, 70,115),
                        new Color(190, 70,  8, 46), new Color(  0,  0,  0,  0)});

        gCore = radial(cx-8f, cy+6f, H*0.055f,
            new float[]{0f,.4f,1f},
            new Color[]{new Color(150,215,255,166), new Color( 70,150,210, 46),
                        new Color(  0,  0,  0,  0)});
    }

    private static RadialGradientPaint radial(float x, float y, float r, float[] f, Color[] c) {
        return new RadialGradientPaint(x, y, r, f, c, MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD: PARTÍCULAS (arrays primitivos SoA, zero boxing)
    // ─────────────────────────────────────────────────────────────────────────

    private void buildParticles(int W, int H) {
        pX=new float[PARTS]; pY=new float[PARTS];
        pVx=new float[PARTS]; pVy=new float[PARTS];
        pSize=new float[PARTS]; pAlpha=new float[PARTS];
        pSpeed=new float[PARTS]; pAngle=new float[PARTS];
        pR=new float[PARTS]; pDrift=new float[PARTS];
        pWarm=new boolean[PARTS];

        float cx=W*0.52f, cy=H*0.44f, minD=Math.min(W,H);
        for (int i=0; i<PARTS; i++) {
            pAngle[i]=(float)(rng.nextFloat()*Math.PI*2);
            pR[i]=50f+rng.nextFloat()*minD*0.44f;
            pX[i]=cx+(float)Math.cos(pAngle[i])*pR[i];
            pY[i]=cy+(float)Math.sin(pAngle[i])*pR[i];
            pSize[i]=0.5f+rng.nextFloat()*1.6f;
            pAlpha[i]=0.25f+rng.nextFloat()*0.75f;
            pWarm[i]=rng.nextFloat()<0.72f;
            pSpeed[i]=0.014f+rng.nextFloat()*0.022f;
            pDrift[i]=(rng.nextFloat()-0.5f)*0.0003f;
        }
    }
}
