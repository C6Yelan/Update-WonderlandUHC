package org.mcwonderland.uhc.application.world;

public final class CenterTerrainSample {
    private static final int HIGHLAND_OFFSET = 55;
    private static final int EXTREME_HIGHLAND_OFFSET = 85;
    private static final int ROUGH_SLOPE_HEIGHT_DIFF = 7;
    private static final int CLIFF_HEIGHT_DIFF = 24;

    private final CenterSamplePoint point;
    private final String biomeKey;
    private final String surfaceMaterialKey;
    private final int surfaceY;
    private final int seaLevel;
    private final int minHeight;
    private final int maxHeight;
    private final int maxNeighborHeightDifference;
    private final boolean standable;
    private final boolean waterSurface;

    public CenterTerrainSample(
            CenterSamplePoint point,
            String biomeKey,
            String surfaceMaterialKey,
            int surfaceY,
            int seaLevel,
            int minHeight,
            int maxHeight,
            int maxNeighborHeightDifference,
            boolean standable,
            boolean waterSurface
    ) {
        if (point == null)
            throw new IllegalArgumentException("point cannot be null.");
        if (minHeight >= maxHeight)
            throw new IllegalArgumentException("minHeight must be lower than maxHeight.");
        if (surfaceY < minHeight || surfaceY >= maxHeight)
            throw new IllegalArgumentException("surfaceY must be inside the world height range.");
        if (maxNeighborHeightDifference < 0)
            throw new IllegalArgumentException("maxNeighborHeightDifference cannot be negative.");

        this.point = point;
        this.biomeKey = normalize(biomeKey);
        this.surfaceMaterialKey = normalize(surfaceMaterialKey);
        this.surfaceY = surfaceY;
        this.seaLevel = seaLevel;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.maxNeighborHeightDifference = maxNeighborHeightDifference;
        this.standable = standable;
        this.waterSurface = waterSurface;
    }

    public CenterSamplePoint getPoint() {
        return point;
    }

    public String getBiomeKey() {
        return biomeKey;
    }

    public String getSurfaceMaterialKey() {
        return surfaceMaterialKey;
    }

    public int getSurfaceY() {
        return surfaceY;
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getMaxNeighborHeightDifference() {
        return maxNeighborHeightDifference;
    }

    public boolean isStandable() {
        return standable;
    }

    public boolean isWaterSurface() {
        return waterSurface;
    }

    public boolean isHighland() {
        return surfaceY >= seaLevel + HIGHLAND_OFFSET;
    }

    public boolean isExtremeHighland() {
        return surfaceY >= seaLevel + EXTREME_HIGHLAND_OFFSET;
    }

    public boolean isRoughSlope() {
        return maxNeighborHeightDifference >= ROUGH_SLOPE_HEIGHT_DIFF;
    }

    public boolean isCliff() {
        return maxNeighborHeightDifference >= CLIFF_HEIGHT_DIFF;
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
