package me.dw1e.kbm.util;

import org.bukkit.util.Vector;

public final class AABB implements Cloneable {

    private Vector min, max;

    public AABB(Vector min, Vector max) {
        this.min = min;
        this.max = max;
    }

    public Vector intersectsRay(Ray ray, float minDist, float maxDist) {
        Vector invDir = new Vector(
                1.0F / ray.getDirection().getX(),
                1.0F / ray.getDirection().getY(),
                1.0F / ray.getDirection().getZ()
        );

        boolean signDirX = invDir.getX() < 0.0;
        boolean signDirY = invDir.getY() < 0.0;
        boolean signDirZ = invDir.getZ() < 0.0;

        Vector bbox = signDirX ? max : min;
        double tmin = (bbox.getX() - ray.getOrigin().getX()) * invDir.getX();
        bbox = signDirX ? min : max;
        double tmax = (bbox.getX() - ray.getOrigin().getX()) * invDir.getX();
        bbox = signDirY ? max : min;
        double tyMin = (bbox.getY() - ray.getOrigin().getY()) * invDir.getY();
        bbox = signDirY ? min : max;
        double tyMax = (bbox.getY() - ray.getOrigin().getY()) * invDir.getY();

        if ((tmin > tyMax) || (tyMin > tmax)) return null;

        if (tyMin > tmin) tmin = tyMin;
        if (tyMax < tmax) tmax = tyMax;

        bbox = signDirZ ? max : min;
        double tzMin = (bbox.getZ() - ray.getOrigin().getZ()) * invDir.getZ();
        bbox = signDirZ ? min : max;
        double tzMax = (bbox.getZ() - ray.getOrigin().getZ()) * invDir.getZ();

        if ((tmin > tzMax) || (tzMin > tmax)) return null;

        if (tzMin > tmin) tmin = tzMin;
        if (tzMax < tmax) tmax = tzMax;

        if ((tmin < maxDist) && (tmax > minDist)) return ray.getPointAtDistance(tmin);

        return null;
    }

    public void translate(Vector vector) {
        min.add(vector);
        max.add(vector);
    }

    public void translateTo(Vector vector) {
        max.setX(vector.getX() + (max.getX() - min.getX()));
        max.setY(vector.getY() + (max.getY() - min.getY()));
        max.setZ(vector.getZ() + (max.getZ() - min.getZ()));

        min.setX(vector.getX());
        min.setY(vector.getY());
        min.setZ(vector.getZ());
    }

    public AABB clone() {
        AABB clone;
        try {
            clone = (AABB) super.clone();
            clone.min = this.min.clone();
            clone.max = this.max.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Vector getMax() {
        return max;
    }

    public Vector getMin() {
        return min;
    }
}