// Reading Order: 00000010
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
package sv.dark.math;

import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: Native Zero-GC Math Library for Graphics.
 * WHY: External libraries (like JOML) can generate object allocations and overhead. We need raw float[] arrays for direct FFI memory mapping.
 * TECHNIQUE: Column-major 4x4 matrix operations on 1D float[16] arrays.
 * GUARANTEES: Zero Garbage Collection. High Performance. FFI compatible.
 */
@AAACertified(date = "2026-06-27", maxLatencyNs = 0, notes = "Zero-GC Matrix Math")
public final class DarkMath {

    public static void identity(float[] m) {
        m[0] = 1; m[4] = 0; m[8] = 0; m[12] = 0;
        m[1] = 0; m[5] = 1; m[9] = 0; m[13] = 0;
        m[2] = 0; m[6] = 0; m[10]= 1; m[14] = 0;
        m[3] = 0; m[7] = 0; m[11]= 0; m[15] = 1;
    }

    public static void multiply(float[] res, float[] a, float[] b) {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                res[c * 4 + r] = a[r] * b[c * 4] + 
                                 a[r + 4] * b[c * 4 + 1] + 
                                 a[r + 8] * b[c * 4 + 2] + 
                                 a[r + 12] * b[c * 4 + 3];
            }
        }
    }

    public static void ortho(float[] m, float left, float right, float bottom, float top, float zNear, float zFar) {
        identity(m);
        m[0] = 2.0f / (right - left);
        m[5] = 2.0f / (top - bottom);
        m[10] = -2.0f / (zFar - zNear);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(zFar + zNear) / (zFar - zNear);
    }

    public static void perspective(float[] m, float fovY, float aspect, float zNear, float zFar) {
        identity(m);
        float f = (float) (1.0 / Math.tan(fovY / 2.0));
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (zFar + zNear) / (zNear - zFar);
        m[11] = -1.0f;
        m[14] = (2.0f * zFar * zNear) / (zNear - zFar);
        m[15] = 0.0f;
    }

    public static void lookAt(float[] m, float eyeX, float eyeY, float eyeZ, 
                              float centerX, float centerY, float centerZ, 
                              float upX, float upY, float upZ) {
        // Forward vector
        float fx = centerX - eyeX;
        float fy = centerY - eyeY;
        float fz = centerZ - eyeZ;
        float fInv = 1.0f / (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx *= fInv; fy *= fInv; fz *= fInv;

        // Right vector (Cross F, U)
        float rx = fy * upZ - fz * upY;
        float ry = fz * upX - fx * upZ;
        float rz = fx * upY - fy * upX;
        float rInv = 1.0f / (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        rx *= rInv; ry *= rInv; rz *= rInv;

        // Up vector (Cross R, F)
        float ux = ry * fz - rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy - ry * fx;

        m[0] = rx; m[4] = ry; m[8] = rz; m[12] = -(rx * eyeX + ry * eyeY + rz * eyeZ);
        m[1] = ux; m[5] = uy; m[9] = uz; m[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
        m[2] = -fx; m[6] = -fy; m[10]= -fz; m[14] = -(-fx * eyeX - fy * eyeY - fz * eyeZ);
        m[3] = 0;  m[7] = 0;  m[11]= 0;  m[15] = 1;
    }
}
