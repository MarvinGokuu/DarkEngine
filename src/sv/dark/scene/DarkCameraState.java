// Reading Order: 01101101
//  109
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import sv.dark.core.AAACertified;
import sv.dark.math.DarkMath;

/**
 * RESPONSIBILITY: Centralized Zero-Allocation Camera State Registry for the render pipeline.
 *
 * WHY THIS EXISTS:
 * The deferred render pipeline requires the camera view/proj matrices to be available
 * to multiple systems (DarkGeometrySystem, DarkClusteredSystem, DarkShadowSystem,
 * DarkDeferredLightingSystem) without passing them through method parameters each frame.
 * This follows the same proven pattern as DarkDeferredLightingSystem.setEnvironment().
 *
 * TECHNIQUE:
 * - Static pre-allocated float arrays (no heap per frame).
 * - Updated once per frame from the Kernel before phaseRender().
 * - Read by all render systems during the same frame.
 *
 * THREAD CONFINEMENT:
 * Updated and read exclusively from the Render Thread (main thread).
 * NOT thread-safe by design. Accessing from any other thread is a contract violation.
 *
 * GUARANTEES:
 * - Zero heap allocations per frame.
 * - Consistent view/proj matrices across all render passes within a single frame.
 * - Default matrices produce a valid (identity + perspective) view if never updated.
 *
 * @author Marvin Alexander Flores Canales
 * @since 4.3.0
 */
@AAACertified(
    date          = "2026-06-27",
    maxLatencyNs  = 0,
    minThroughput = 0,
    alignment     = 16,
    lockFree      = true,
    offHeap       = false,
    notes         = "Zero-Alloc camera state registry. Render Thread ONLY."
)
public final class DarkCameraState {

    // -------------------------------------------------------------------------
    // CAMERA MATRICES (Pre-allocated static float arrays — Zero-GC)
    // -------------------------------------------------------------------------

    /**
     * Current camera view matrix (row-major, float[16]).
     * Set once per frame before phaseRender(). Read by all render passes.
     */
    public static final float[] VIEW_MATRIX = new float[16];

    /**
     * Current camera projection matrix (row-major, float[16]).
     * Set once per frame before phaseRender(). Read by all render passes.
     */
    public static final float[] PROJ_MATRIX = new float[16];

    /**
     * Camera position in world space (float[3]: x, y, z).
     * Consumed by DarkDeferredLightingSystem for specular highlights.
     */
    public static final float[] CAMERA_POS = {0.0f, 5.0f, 10.0f};

    /**
     * Camera field-of-view in radians. Used by CSM cascade calculation.
     */
    public static float FOV_Y = (float) Math.toRadians(60.0f);

    /**
     * Camera aspect ratio (width / height). Default 1280/720.
     */
    public static float ASPECT = 1280.0f / 720.0f;

    /**
     * Camera near plane distance.
     */
    public static float Z_NEAR = 0.1f;

    /**
     * Camera far plane distance.
     */
    public static float Z_FAR = 1000.0f;

    // -------------------------------------------------------------------------
    // INITIALIZATION
    // -------------------------------------------------------------------------

    static {
        // Initialize with a stable default so the render pipeline works
        // even before a proper camera system updates the matrices.
        // Default: identity view + perspective proj (60° FOV, 16:9, 0.1 near, 1000 far)
        DarkMath.identity(VIEW_MATRIX);
        // Translate camera back 10 units so the scene origin is visible
        VIEW_MATRIX[14] = -10.0f;

        DarkMath.perspective(PROJ_MATRIX,
            (float) Math.toRadians(60.0f),
            1280.0f / 720.0f,
            0.1f,
            1000.0f
        );
    }

    // -------------------------------------------------------------------------
    // UPDATE API (called by ECS/Kernel once per frame)
    // -------------------------------------------------------------------------

    /**
     * Updates all camera matrices for this frame.
     * Called once per frame from the Kernel or camera ECS system BEFORE phaseRender().
     *
     * // [NON_BLOCKING] [ZERO_ALLOCATION] [RENDER_THREAD_ONLY]
     *
     * @param view   float[16] view matrix.
     * @param proj   float[16] projection matrix.
     * @param camX   Camera world X position.
     * @param camY   Camera world Y position.
     * @param camZ   Camera world Z position.
     */
    public static void update(float[] view, float[] proj, float camX, float camY, float camZ) {
        System.arraycopy(view, 0, VIEW_MATRIX, 0, 16);
        System.arraycopy(proj, 0, PROJ_MATRIX, 0, 16);
        CAMERA_POS[0] = camX;
        CAMERA_POS[1] = camY;
        CAMERA_POS[2] = camZ;
    }

    private DarkCameraState() {
        // Static registry — never instantiate.
    }
}
