package org.mcwonderland.uhc.game.timer.impl.countdown;

import org.mcwonderland.uhc.platform.event.PluginEvents;
import org.mcwonderland.uhc.api.enums.RoleName;
import org.mcwonderland.uhc.api.event.timer.FinalHealEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.game.timer.Countdown;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class FinalHealCountdown extends Countdown {

    @Override
    public void execute() {
        PluginEvents.callEvent(new FinalHealEvent());
        Game.getGame().setFinalHealEnabled(true);

        for (UHCPlayer uhcPlayer : UHCPlayers.getStatusIs(RoleName.PLAYER)) {
            LivingEntity entity = uhcPlayer.getEntity();
            entity.setHealth(getMaxHealth(entity));
        }
    }

    private double getMaxHealth(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 20.0 : maxHealth.getValue();
    }

    @Override
    public int getToggleTimer() {
        return getTimerSettings().getHealTime();
    }

    @Override
    public String getCountdownBroadcast() {
        Extra.sound(Sounds.Countdown.FinalHeal.TICK);
        return Messages.CountDown.FINAL_HEAL_ANNOUNCE;
    }

    @Override
    public String getToggledBroadcast() {
        Extra.sound(Sounds.Countdown.FinalHeal.RUN);
        return Messages.CountDown.FINAL_HEAL_ENABLED;
    }
}
