package org.mcwonderland.uhc.util;

import lombok.experimental.UtilityClass;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.legacy.LegacyDatouNmsAdapter;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.platform.PlayerHand;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 2019-12-08 上午 09:16
 */

@UtilityClass
public class PlayerUtils {

    public Player getShooter(Entity damager) {
        if (!(damager instanceof Projectile))
            return null;

        return getShooter(( Projectile ) damager);
    }

    public Player getShooter(Projectile projectile) {
        if (projectile.getShooter() instanceof Player)
            return ( Player ) projectile.getShooter();

        return null;
    }

    public UHCPlayer getUHCShooter(Projectile projectile) {
        Player shooter = getShooter(projectile);

        if (shooter != null)
            return UHCPlayer.getUHCPlayer(shooter);

        return null;
    }

    public double getFullHealth(LivingEntity entity) {
        double health = entity.getHealth();
        double absorption = 0;

        if (entity instanceof Player)
            absorption = LegacyDatouNmsAdapter.current().getAbsorptionHearts(( Player ) entity);

        return health + absorption;
    }

    public void costPlayerToolDurability(Player p) {
        ItemStack itemInHand = PlayerHand.getMainHandItem(p);

        if (itemInHand == null || !Extra.isDamageable(itemInHand.getType()))
            return;

        ItemMeta itemMeta = itemInHand.getItemMeta();

        if (!(itemMeta instanceof Damageable))
            return;

        Damageable damageable = ( Damageable ) itemMeta;
        damageable.setDamage(damageable.getDamage() + 1);

        if (isItemReachedMaxDurability(itemInHand, damageable)) {
            p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            p.updateInventory();
            LegacyFoundationAdapter.playItemBreakSound(p);
            return;
        }

        itemInHand.setItemMeta(itemMeta);
    }

    public boolean respawnIfDead(Player player) {
        if (player == null || !player.isOnline())
            return false;

        if (!player.isDead() && player.getHealth() > 0)
            return false;

        player.spigot().respawn();
        return true;
    }

    private boolean isItemReachedMaxDurability(ItemStack i, Damageable damageable) {
        return damageable.getDamage() > i.getType().getMaxDurability();
    }

    public void breakBlockNms(Player player, Block toBreak) {
        Object entityPlayer = LegacyFoundationAdapter.getHandleEntity(player);
        Object playerInteractManager = LegacyFoundationAdapter.getFieldContent(entityPlayer, "playerInteractManager");
        Object blockPosition = LegacyFoundationAdapter.newBlockPosition(toBreak.getX(), toBreak.getY(), toBreak.getZ());

        Extra.playBlockBreakEffect(toBreak.getLocation(), toBreak.getType());
        LegacyFoundationAdapter.invoke("breakBlock", playerInteractManager, blockPosition);
    }

    public double getArmorPoints(LivingEntity livingEntity) {
        ItemStack[] armors = livingEntity.getEquipment().getArmorContents();
        double totalPoints = 0;

        for (ItemStack itemStack : armors) {
            if (itemStack != null)
                totalPoints += getArmorPoints(itemStack);
        }

        return totalPoints;
    }

    private double getArmorPoints(ItemStack itemStack) {
        return LegacyDatouNmsAdapter.current().getArmorPoints(itemStack);
    }

    public boolean isShieldBlocked(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player))
            return false;

        Player player = ( Player ) e.getEntity();
        return player.isBlocking() && e.getFinalDamage() <= 0;
    }
}
