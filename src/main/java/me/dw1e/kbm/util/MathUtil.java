package me.dw1e.kbm.util;

public final class MathUtil {

    public static double hypot(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static int floor(double v) {
        int i = (int) v;
        return (v < i) ? (i - 1) : i;
    }

    public static double calculateDistanceTraveled(double velocity, int time) {
        double totalDistance = 0;

        for (int i = 0; i < time; i++) {
            totalDistance += velocity;
            velocity = ((velocity - 0.08) * 0.98);
            velocity = Math.min(velocity, 3.92);
        }

        return totalDistance;
    }

    public static int calculateFallTime(double initialVelocity, double distance) {
        double velocity = Math.abs(initialVelocity);
        int ticks = 0;

        while (distance > 0) {
            velocity += 0.08;
            velocity = Math.min(velocity, 3.92);
            velocity *= 0.98;
            distance -= velocity;
            ticks++;
        }

        return ticks;
    }

    public static int calculateTimeToMaxVelocity(double targetVerticalVelocity) {
        double a = -0.08 * 0.98;
        double b = 0.08 + 3.92 * 0.98;
        double c = -2 * targetVerticalVelocity;

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return 0;

        double positiveRoot = (-b + Math.sqrt(discriminant)) / (2 * a);
        return (int) Math.ceil(positiveRoot * 20);
    }
}
