package org.mcwonderland.uhc.scenario.impl.special;

import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.platform.PlayerHand;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioTripleArrow extends ConfigBasedScenario implements Listener {

    private static final String NO_PICKUP_TAG = "triple_arrow_no_pickup";

    public ScenarioTripleArrow(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowShoot(EntityShootBowEvent e) {
        LivingEntity shooter = e.getEntity();

        if (shooter instanceof Player)
            tripleArrows(e.getProjectile(), ( Player ) shooter);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile entity = e.getEntity();

        if (entity.getPersistentDataContainer().has(noPickupKey(), PersistentDataType.BYTE)) {
            entity.remove();
        }
    }

    private void tripleArrows(Entity projectile, Player shooter) {
        Vector v = projectile.getVelocity();
        double vectorX = v.getX();

        v.setX(vectorX - 0.5);
        shootArrow(shooter, v);
        v.setX(vectorX + 0.5);
        shootArrow(shooter, v);
    }

    private void shootArrow(Player shooter, Vector v) {
        ItemStack arrowItem = getArrowsInInventory(shooter);

        if (arrowItem == null)
            return;

        Arrow arrow = shooter.launchProjectile(Arrow.class, v);
        arrow.getPersistentDataContainer().set(noPickupKey(), PersistentDataType.BYTE, ( byte ) 1);

        if (!isBowInfinity(shooter))
            arrowItem.setAmount(arrowItem.getAmount() - 1);
    }

    private boolean isBowInfinity(Player shooter) {
        return PlayerHand.getMainHandItem(shooter).getEnchantmentLevel(Enchantment.INFINITY) > 0;
    }

    private ItemStack getArrowsInInventory(Player shooter) {
        return PluginMaterials.getFirstItem(shooter, PluginMaterials.itemOf("ARROW"));
    }

    private NamespacedKey noPickupKey() {
        return new NamespacedKey(WonderlandUHC.getInstance(), NO_PICKUP_TAG);
    }
}
