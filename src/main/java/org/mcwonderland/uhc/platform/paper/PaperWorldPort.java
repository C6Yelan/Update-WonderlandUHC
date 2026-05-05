package org.mcwonderland.uhc.platform.paper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.mcwonderland.uhc.port.WorldPort;

public final class PaperWorldPort implements WorldPort {

    @Override
    public boolean worldExists(String worldName) {
        return Bukkit.getWorld(worldName) != null;
    }

    @Override
    public void createWorld(String worldName) {
        Bukkit.createWorld(new WorldCreator(worldName));
    }

    @Override
    public void createNetherWorld(String worldName) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(World.Environment.NETHER);
        Bukkit.createWorld(worldCreator);
    }
}
