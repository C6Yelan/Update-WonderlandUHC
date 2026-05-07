package org.mcwonderland.uhc.platform.paper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.mcwonderland.uhc.port.WorldBorderPort;

public final class PaperWorldBorderAdapter implements WorldBorderPort {

    @Override
    public void reset(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null)
            world.getWorldBorder().reset();
    }

    @Override
    public void setCenter(String worldName, double x, double z) {
        worldBorder(worldName).setCenter(x, z);
    }

    @Override
    public void setSize(String worldName, double size) {
        worldBorder(worldName).setSize(size);
    }

    @Override
    public void changeSize(String worldName, double size, long seconds) {
        worldBorder(worldName).changeSize(size, seconds);
    }

    @Override
    public void setWarningDistance(String worldName, int blocks) {
        worldBorder(worldName).setWarningDistance(blocks);
    }

    @Override
    public void setWarningTimeTicks(String worldName, int ticks) {
        worldBorder(worldName).setWarningTimeTicks(ticks);
    }

    @Override
    public double getSize(String worldName) {
        return worldBorder(worldName).getSize();
    }

    private WorldBorder worldBorder(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null)
            throw new IllegalArgumentException("World is not loaded: " + worldName);

        return world.getWorldBorder();
    }
}
