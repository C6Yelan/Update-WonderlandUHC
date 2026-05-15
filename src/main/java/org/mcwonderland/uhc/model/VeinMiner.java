package org.mcwonderland.uhc.model;

import lombok.experimental.UtilityClass;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.platform.PlayerHand;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.mcwonderland.uhc.util.UniqueQueue;
import org.mcwonderland.uhc.util.cuboid.Cuboid;
import org.mcwonderland.uhc.util.cuboid.SelectMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class VeinMiner {
    private static final int MAX_COUNT = 100;

    private Set<UUID> mining = new HashSet<>();

    public void mineVeins(Block block, Player player, SelectMode mode) {
        if (isMining(player))
            return;

        mining.add(player.getUniqueId());

        try {
            Set<Block> blocks = calculateConnectBlocks(block, mode);
            blocks.forEach(b -> breakBlock(player, b));
        } finally {
            mining.remove(player.getUniqueId());
        }
    }

    public int countConnectedBlocks(Block block, SelectMode mode) {
        return calculateConnectBlocks(block, mode).size();
    }

    private void breakBlock(Player player, Block block) {
        try {
            PlayerUtils.breakBlockNms(player, block);
        } catch (RuntimeException | LinkageError ex) {
            breakBlockWithBukkit(player, block, ex);
        }
    }

    private void breakBlockWithBukkit(Player player, Block block, Throwable cause) {
        try {
            if (player.breakBlock(block))
                return;
        } catch (RuntimeException | LinkageError ignored) {
        }

        try {
            if (!block.breakNaturally(PlayerHand.getMainHandItem(player)))
                block.setType(Material.AIR);
        } catch (RuntimeException | LinkageError fallbackEx) {
            LegacyFoundationAdapter.error(
                    fallbackEx,
                    "VeinMiner failed to break a connected block after the legacy NMS break failed.",
                    "Original failure: " + cause.getClass().getSimpleName() + ": " + cause.getMessage()
            );
        }
    }

    private Set<Block> calculateConnectBlocks(Block startBlock, SelectMode mode) {
        Material type = getType(startBlock);
        Set<Block> allBlocks = new HashSet<>();
        allBlocks.add(startBlock);
        UniqueQueue<Block> temp = new UniqueQueue<>();
        temp.addAll(getNearBlocks(startBlock, mode));

        int count = 0;

        while (!temp.isEmpty() && count < MAX_COUNT) {
            Block b = temp.remove();

            if (getType(b) == type && allBlocks.add(b)) {
                temp.addAll(getNearBlocks(b, mode));
                count = nextVisitedCount(count, MAX_COUNT);
            }
        }

        allBlocks.remove(startBlock);
        return allBlocks;
    }

    private Set<Block> getNearBlocks(Block block, SelectMode mode) {
        return Cuboid.getBlocksNearBy(block, mode)
                .filter(b -> getType(b) == getType(block))
                .collect(Collectors.toSet());
    }

    private Material getType(Block block) {
        return block.getType().toString().equalsIgnoreCase("GLOWING_REDSTONE_ORE") ?
                Material.REDSTONE_ORE : block.getType();
    }

    static int nextVisitedCount(int currentCount, int maxCount) {
        return Math.min(currentCount + 1, maxCount);
    }

    public boolean isMining(Player player) {
        return mining.contains(player.getUniqueId());
    }
}
