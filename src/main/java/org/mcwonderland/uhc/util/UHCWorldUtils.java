package org.mcwonderland.uhc.util;

import lombok.experimental.UtilityClass;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameManager;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.spawn.Spawns;
import org.mcwonderland.uhc.settings.spawn.UHCSpawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@UtilityClass
public class UHCWorldUtils {
    private static final double NETHER_COORDINATE_SCALE = 8D;

    public World getLobby() {
        return getLobbySpawn().getWorld();
    }

    public World getWorld() {
        return Bukkit.getWorld(getWorldName());
    }

    public World getNether() {
        return Bukkit.getWorld(getNetherName());
    }

    public String getWorldName() {
        return Settings.Game.UHC_WORLD_NAME;
    }

    public String getNetherName() {
        return Settings.Game.UHC_WORLD_NAME + "_nether";
    }

    public World[] getUhcWorlds() {
        return new World[]{getWorld(), getNether()};
    }

    public String[] getUhcWorldNames() {
        return new String[]{getWorldName(), getNetherName()};
    }

    public boolean isUhcWorld(World world) {
        return world != null && isUhcWorldName(world.getName());
    }

    public boolean isUhcWorldName(String worldName) {
        return worldName != null && (worldName.equals(getWorldName()) || worldName.equals(getNetherName()));
    }

    public Location getLobbySpawn() {
        UHCSpawn lobbySpawn = Spawns.getLobbySpawn();
        return lobbySpawn.getLocation();
    }

    public MatchCenter getMatchCenter() {
        return Game.getGame().getMatchCenter();
    }

    public Location getMatchCenterLocation() {
        MatchCenter center = getMatchCenter();
        World world = getWorld();

        if (world == null)
            return new Location(null, center.getX() + 0.5D, 100, center.getZ() + 0.5D);

        Block block = GameManager.getHighestBlock(world, center.getX(), center.getZ());
        double y = block == null ? 100 : block.getY() + 2.5D;

        return new Location(world, center.getX() + 0.5D, y, center.getZ() + 0.5D);
    }

    public MatchCenter getBorderCenter(World world, int borderSize) {
        int safeBorderSize = Math.max(1, borderSize);

        if (world == null)
            return new MatchCenter(0, 0, safeBorderSize);

        MatchCenter center = getMatchCenter();

        if (world.getName().equals(getNetherName()))
            return new MatchCenter(scaleToNether(center.getX()), scaleToNether(center.getZ()), safeBorderSize);

        if (!world.getName().equals(getWorldName()))
            return new MatchCenter(0, 0, safeBorderSize);

        return new MatchCenter(center.getX(), center.getZ(), safeBorderSize);
    }

    private int scaleToNether(int coordinate) {
        return (int) Math.round(coordinate / NETHER_COORDINATE_SCALE);
    }
}
