package org.mcwonderland.uhc.application.world;

import java.util.Objects;

public final class MatchCenter {
    private final int x;
    private final int z;
    private final int borderSize;

    public MatchCenter(int x, int z, int borderSize) {
        if (borderSize <= 0)
            throw new IllegalArgumentException("borderSize must be positive.");

        this.x = x;
        this.z = z;
        this.borderSize = borderSize;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getBorderSize() {
        return borderSize;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof MatchCenter))
            return false;

        MatchCenter that = (MatchCenter) object;
        return x == that.x
                && z == that.z
                && borderSize == that.borderSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, borderSize);
    }
}
