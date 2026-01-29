package me.dw1e.kbm.util;

public final class MathUtil {

    public static float getAngle(double entityX, double entityZ, double viewerX, double viewerZ) {
        double dx = viewerX - entityX;
        double dz = viewerZ - entityZ;
        return (float) Math.toDegrees(Math.atan2(dz, dx));
    }

    public static int floor(double v) {
        int i = (int) v;
        return (v < i) ? (i - 1) : i;
    }
}
