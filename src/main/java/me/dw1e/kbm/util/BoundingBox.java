package me.dw1e.kbm.util;

import org.bukkit.util.Vector;

public final class BoundingBox implements Cloneable {

    private double minX, minY, minZ, maxX, maxY, maxZ;

    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public BoundingBox(Vector vector) {
        minX = vector.getX() - 0.3F;
        minY = vector.getY();
        minZ = vector.getZ() - 0.3F;

        maxX = vector.getX() + 0.3F;
        maxY = vector.getY() + 1.8F;
        maxZ = vector.getZ() + 0.3F;
    }

    public BoundingBox expand(double x, double y, double z) {
        minX -= x;
        minY -= y;
        minZ -= z;

        maxX += x;
        maxY += y;
        maxZ += z;

        return this;
    }

    public BoundingBox clone() {
        try {
            return (BoundingBox) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public double posX() {
        return (maxX + minX) / 2.0;
    }

    public double posZ() {
        return (maxZ + minZ) / 2.0;
    }

    public Vector toVector() {
        return new Vector(posX(), 0.0, posZ());
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }
}
