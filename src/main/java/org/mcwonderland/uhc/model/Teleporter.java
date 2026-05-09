package org.mcwonderland.uhc.model;

import lombok.experimental.UtilityClass;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameManager;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

@UtilityClass
public class Teleporter {

    private final Random r = new Random();
    private Queue<Location> locationCache = new LinkedList<>();

    public Location randomTp(UHCTeam team, World world) {
        Location loc = locationCache.poll();

        if (loc == null)
            loc = getRandomTp(world, Game.getGame().getCurrentBorder());

        teleportTeamTo(team, loc);
        return loc;
    }

    public void teleportTeamTo(UHCTeam team, Location toTeleport) {
        for (LivingEntity entity : team.getAliveEntities())
            entity.teleport(toTeleport);
    }

    public Location getRandomTp(World world, int border) {
        if (border % 2 != 0) {
            border++;
        }
        border = border / 2 + 1;
        int b = border;
        int x = 0;
        int y = 50;
        int z = 0;
        MatchCenter center = UHCWorldUtils.getBorderCenter(world, border * 2);
        for (int run = 0; run < 20; run++) { //最多嘗試20次
            int offsetX = r.nextInt(b);
            int offsetZ = r.nextInt(b);
            x = center.getX() + (r.nextBoolean() ? -offsetX : offsetX);
            y = 70;
            z = center.getZ() + (r.nextBoolean() ? -offsetZ : offsetZ);
            Block block = GameManager.getHighestBlock(world, x, z);

            if (block != null && !block.getType().toString().contains("LAVA") && !block.getType().toString().contains("WATER")) {
                y = block.getY();
                break;
            }
        }
        return new Location(world, x + 0.5, y + 2.5, z + 0.5);
    }
}
