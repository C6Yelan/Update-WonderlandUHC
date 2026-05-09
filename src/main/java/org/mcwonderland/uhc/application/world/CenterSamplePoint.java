package org.mcwonderland.uhc.application.world;

import java.util.Objects;

public final class CenterSamplePoint {
    private final int x;
    private final int z;

    public CenterSamplePoint(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof CenterSamplePoint))
            return false;

        CenterSamplePoint that = (CenterSamplePoint) object;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}
