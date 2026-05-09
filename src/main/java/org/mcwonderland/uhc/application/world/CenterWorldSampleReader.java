package org.mcwonderland.uhc.application.world;

import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public final class CenterWorldSampleReader {
    private static final int DEFAULT_NEIGHBOR_DISTANCE = 16;

    private final CenterSampleSource source;
    private final int neighborDistance;
    private final Map<CenterSamplePoint, CenterTerrainSample> cache = new HashMap<>();

    public CenterWorldSampleReader(World world) {
        this(new BukkitCenterSampleSource(world), DEFAULT_NEIGHBOR_DISTANCE);
    }

    CenterWorldSampleReader(CenterSampleSource source, int neighborDistance) {
        if (source == null)
            throw new IllegalArgumentException("source cannot be null.");
        if (neighborDistance <= 0)
            throw new IllegalArgumentException("neighborDistance must be positive.");

        this.source = source;
        this.neighborDistance = neighborDistance;
    }

    public CenterTerrainSample sample(CenterSamplePoint point) {
        if (point == null)
            throw new IllegalArgumentException("point cannot be null.");

        CenterTerrainSample cached = cache.get(point);

        if (cached != null)
            return cached;

        CenterTerrainSample sample = readSample(point);
        cache.put(point, sample);
        return sample;
    }

    public int getCachedSampleCount() {
        return cache.size();
    }

    public void clearCache() {
        cache.clear();
    }

    private CenterTerrainSample readSample(CenterSamplePoint point) {
        int surfaceY = source.getSurfaceY(point.getX(), point.getZ());
        int maxNeighborDiff = maxNeighborHeightDifference(point, surfaceY);

        return new CenterTerrainSample(
                point,
                source.getBiomeKey(point.getX(), point.getZ()),
                source.getSurfaceMaterialKey(point.getX(), point.getZ()),
                surfaceY,
                source.getSeaLevel(),
                source.getMinHeight(),
                source.getMaxHeight(),
                maxNeighborDiff,
                source.isStandable(point.getX(), point.getZ()),
                source.isWaterSurface(point.getX(), point.getZ())
        );
    }

    private int maxNeighborHeightDifference(CenterSamplePoint point, int surfaceY) {
        int maxDiff = 0;
        maxDiff = Math.max(maxDiff, heightDiff(point.getX() + neighborDistance, point.getZ(), surfaceY));
        maxDiff = Math.max(maxDiff, heightDiff(point.getX() - neighborDistance, point.getZ(), surfaceY));
        maxDiff = Math.max(maxDiff, heightDiff(point.getX(), point.getZ() + neighborDistance, surfaceY));
        maxDiff = Math.max(maxDiff, heightDiff(point.getX(), point.getZ() - neighborDistance, surfaceY));
        return maxDiff;
    }

    private int heightDiff(int x, int z, int surfaceY) {
        return Math.abs(source.getSurfaceY(x, z) - surfaceY);
    }
}
