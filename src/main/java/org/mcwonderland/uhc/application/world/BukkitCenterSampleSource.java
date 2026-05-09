package org.mcwonderland.uhc.application.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

final class BukkitCenterSampleSource implements CenterSampleSource {
    private final World world;

    BukkitCenterSampleSource(World world) {
        if (world == null)
            throw new IllegalArgumentException("world cannot be null.");

        this.world = world;
    }

    @Override
    public int getMinHeight() {
        return world.getMinHeight();
    }

    @Override
    public int getMaxHeight() {
        return world.getMaxHeight();
    }

    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override
    public int getSurfaceY(int x, int z) {
        return world.getHighestBlockAt(x, z).getY();
    }

    @Override
    public String getBiomeKey(int x, int z) {
        Biome biome = world.getBiome(x, clampY(getSurfaceY(x, z)), z);
        return biome.getKey().toString();
    }

    @Override
    public String getSurfaceMaterialKey(int x, int z) {
        return surfaceType(x, z).getKey().toString();
    }

    @Override
    public boolean isStandable(int x, int z) {
        int surfaceY = getSurfaceY(x, z);
        Material surface = world.getBlockAt(x, surfaceY, z).getType();
        Material above = world.getBlockAt(x, Math.min(surfaceY + 1, world.getMaxHeight() - 1), z).getType();
        return surface.isSolid() && above.isAir();
    }

    @Override
    public boolean isWaterSurface(int x, int z) {
        String material = surfaceType(x, z).name();
        return material.contains("WATER") || material.contains("KELP") || material.contains("SEAGRASS");
    }

    private Material surfaceType(int x, int z) {
        Block block = world.getHighestBlockAt(x, z);
        return block.getType();
    }

    private int clampY(int y) {
        return Math.min(world.getMaxHeight() - 1, Math.max(world.getMinHeight(), y));
    }
}
