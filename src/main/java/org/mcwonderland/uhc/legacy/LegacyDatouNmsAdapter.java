package org.mcwonderland.uhc.legacy;

import me.lulu.datounms.DaTouNMS;
import me.lulu.datounms.UnSupportedNmsException;
import me.lulu.datounms.model.ArmorInfo;
import me.lulu.datounms.model.NewerSpigotAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.PlatformCapabilities;

public final class LegacyDatouNmsAdapter {

    private static LegacyDatouNmsAdapter current = unavailable();

    private final boolean supported;
    private final PlatformCapabilities capabilities;

    private LegacyDatouNmsAdapter(boolean supported, PlatformCapabilities capabilities) {
        this.supported = supported;
        this.capabilities = capabilities;
    }

    public static LegacyDatouNmsAdapter initialize(WonderlandUHC plugin) {
        try {
            DaTouNMS.setup(plugin);
            current = new LegacyDatouNmsAdapter(true, PlatformCapabilities.allLegacyNmsAvailable());
        } catch (UnSupportedNmsException e) {
            LegacyFoundationAdapter.log("&eDaTouNMS does not support this Minecraft version; legacy NMS-backed features will be unavailable.");
            current = unavailable();
        }

        return current;
    }

    public static LegacyDatouNmsAdapter current() {
        return current;
    }

    public boolean isSupported() {
        return supported;
    }

    public PlatformCapabilities capabilities() {
        return capabilities;
    }

    public double getAbsorptionHearts(Player player) {
        if (!capabilities.hasAbsorption())
            return getBukkitAbsorptionHearts(player);

        try {
            return DaTouNMS.getCommonNMS().getAbsorptionHeart(player);
        } catch (RuntimeException ex) {
            return getBukkitAbsorptionHearts(player);
        }
    }

    public double getArmorPoints(Material material) {
        if (!supported)
            return 0;

        try {
            ArmorInfo info = ArmorInfo.fromMaterial(material);
            return DaTouNMS.getCommonNMS().getArmorPoint(info);
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    public void setCanPickupExp(Player player, boolean canPickup) {
        if (!capabilities.hasPickupExpControl())
            return;

        DaTouNMS.getCommonNMS().setCanPickupExp(player, canPickup);
    }

    public void playDeathAnimation(Player player) {
        if (!capabilities.hasDeathAnimation())
            return;

        DaTouNMS.getCommonNMS().playDeathAnimation(player);
    }

    public void spawnExpOrb(Location location, int amount, int value) {
        if (amount <= 0 || value <= 0)
            return;

        if (capabilities.hasCustomExpOrb()) {
            DaTouNMS.getWorldNMS().spawnOrb(location, amount, value);
            return;
        }

        for (int i = 0; i < amount; i++) {
            ExperienceOrb orb = location.getWorld().spawn(location, ExperienceOrb.class);
            orb.setExperience(value);
        }
    }

    public void randomizeEnchantSeed(PrepareItemEnchantEvent event) {
        if (!capabilities.hasOldEnchant())
            return;

        DaTouNMS.getEnchantHandler().randomizeSeed(event);
    }

    public void applyOldEnchantCosts(PrepareItemEnchantEvent event) {
        if (!capabilities.hasOldEnchant())
            return;

        DaTouNMS.getEnchantHandler().oldEnchantCosts(event);
    }

    public void hideEnchants(PrepareItemEnchantEvent event) {
        if (!capabilities.hasOldEnchant())
            return;

        DaTouNMS.getEnchantHandler().hideEnchants(event);
    }

    public void setBlockFast(World world, int x, int y, int z, Material material, byte data, boolean applyPhysics) {
        if (capabilities.hasFastBlockSet()) {
            DaTouNMS.getWorldNMS().setBlockSuperFast(world, x, y, z, material, data, applyPhysics);
            return;
        }

        world.getBlockAt(x, y, z).setType(material, applyPhysics);
    }

    public void setBlockFast(Location location, Material material, byte data, boolean applyPhysics) {
        if (capabilities.hasFastBlockSet()) {
            DaTouNMS.getWorldNMS().setBlockSuperFast(location, material, data, applyPhysics);
            return;
        }

        location.getBlock().setType(material, applyPhysics);
    }

    public void mergeLargeChest(Block leftSide, Block rightSide) {
        if (capabilities.hasLargeChestMerge()) {
            NewerSpigotAPI.mergeChest(leftSide, rightSide);
            return;
        }

        if (leftSide.getType() != Material.CHEST || rightSide.getType() != Material.CHEST)
            return;

        BlockFace facing = getLargeChestFacing(leftSide, rightSide);
        if (facing == null)
            return;

        org.bukkit.block.data.type.Chest leftData = ( org.bukkit.block.data.type.Chest ) leftSide.getBlockData();
        org.bukkit.block.data.type.Chest rightData = ( org.bukkit.block.data.type.Chest ) rightSide.getBlockData();

        leftData.setFacing(facing);
        rightData.setFacing(facing);
        leftData.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
        rightData.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);

        leftSide.setBlockData(leftData, false);
        rightSide.setBlockData(rightData, false);
    }

    private BlockFace getLargeChestFacing(Block leftSide, Block rightSide) {
        int dx = rightSide.getX() - leftSide.getX();
        int dz = rightSide.getZ() - leftSide.getZ();

        if (dx == 0 && dz == -1)
            return BlockFace.WEST;
        if (dx == 0 && dz == 1)
            return BlockFace.EAST;
        if (dx == 1 && dz == 0)
            return BlockFace.NORTH;
        if (dx == -1 && dz == 0)
            return BlockFace.SOUTH;

        return null;
    }

    private double getBukkitAbsorptionHearts(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.ABSORPTION);
        if (effect == null)
            return 0;

        return (effect.getAmplifier() + 1) * 4.0;
    }

    private static LegacyDatouNmsAdapter unavailable() {
        return new LegacyDatouNmsAdapter(false, PlatformCapabilities.noLegacyNmsAvailable());
    }
}
