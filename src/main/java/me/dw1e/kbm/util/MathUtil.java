package me.dw1e.kbm.util;

public final class MathUtil {

    public static double hypot(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static int floor(double v) {
        int i = (int) v;
        return (v < i) ? (i - 1) : i;
    }
}
