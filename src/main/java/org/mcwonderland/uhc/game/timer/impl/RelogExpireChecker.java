package org.mcwonderland.uhc.game.timer.impl;

import com.google.common.collect.Lists;
import org.mcwonderland.uhc.application.match.CombatRelogTickAction;
import org.mcwonderland.uhc.application.match.CombatRelogTickResult;
import org.mcwonderland.uhc.application.match.CombatRelogTickUseCase;
import org.mcwonderland.uhc.game.CombatRelog;
import org.mcwonderland.uhc.game.border.BorderType;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.timer.SecondTimer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.GameUtils;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class RelogExpireChecker extends SecondTimer {
    private static final CombatRelogTickUseCase COMBAT_RELOG_TICK = new CombatRelogTickUseCase();

    @Override
    public void run() {
        for (CombatRelog relog : CombatRelog.getRelogs()) {
            UHCPlayer uhcPlayer = relog.getUhcPlayer();
            LivingEntity relogEntity = relog.getEntity();
            int remainingSeconds = relog.getTime();
            boolean borderMoving = GameUtils.getBorderType() == BorderType.MOVING;
            boolean insideBorder = !borderMoving || remainingSeconds <= 0 || BorderUtil.isInBorder(relogEntity.getLocation());
            CombatRelogTickResult result = COMBAT_RELOG_TICK.tick(
                    remainingSeconds,
                    borderMoving,
                    insideBorder);

            if (result.getAction() == CombatRelogTickAction.EXPIRE) {
                Chat.broadcast(Messages.Game.RELOG_DEATH
                        .replace("{player}", uhcPlayer.getName())
                        .replace("{playerKills}", uhcPlayer.getStats().kills + "")
                        .replace("{minute}", Settings.CombatRelog.RELOG_IN_MINUTES + ""));
                killAndRemove(relog);
            } else if (result.getAction() == CombatRelogTickAction.DAMAGE_OUTSIDE_BORDER) {
                relogEntity.damage(2.0);
            }

            relog.setTime(result.getNextRemainingSeconds());
        }
    }

    private static void killAndRemove(CombatRelog relog) {
        LivingEntity relogEntity = relog.getEntity();
        Location deathLocation = relogEntity.getLocation();
        DamageSource damageSource = DamageSource.builder(DamageType.GENERIC_KILL).build();
        EntityDeathEvent deathEvent = new EntityDeathEvent(relogEntity, damageSource, Lists.newArrayList());
        LegacyFoundationAdapter.callEvent(deathEvent);

        for (ItemStack drop : deathEvent.getDrops()) {
            if (drop == null || LegacyFoundationAdapter.isAir(drop.getType()))
                continue;

            deathLocation.getWorld().dropItemNaturally(deathLocation, drop);
        }

        relogEntity.remove();
        relog.remove();
    }

}
