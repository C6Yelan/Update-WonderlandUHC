package org.mcwonderland.uhc.model;

import com.google.common.collect.Maps;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class InvinciblePlayer {
    private static final String INVINCIBLE_DAMAGE_CANCEL_BYPASS_TAG = "wonderlanduhc_invincible_damage_cancel_bypass";
    private static final Map<UHCPlayer, InvinciblePlayer> invinciblePlayers = new HashMap<>();

    public static void startTask() {
        LegacyFoundationAdapter.runTimerAsync(20 * 1, new InvinciblePlayerTask());
    }

    private int time;

    private InvinciblePlayer(int time) {
        this.time = time;
    }

    public static void addInvincible(UHCPlayer uhcPlayer, int time) {
        if (isNeedToAdd(uhcPlayer, time)) {
            invinciblePlayers.put(uhcPlayer, new InvinciblePlayer(time));

            Chat.send(uhcPlayer.getPlayer(), LegacyFoundationAdapter.replaceTimePlaceholders(Messages.Game.NoClean.OBTAINED, time));
        }
    }

    private static boolean isNeedToAdd(UHCPlayer uhcPlayer, int time) {
        InvinciblePlayer invinciblePlayer = InvinciblePlayer.getInvinciblePlayer(uhcPlayer);

        return invinciblePlayer == null || invinciblePlayer.time < time;
    }

    public static void removeInvincible(UHCPlayer uhcPlayer) {
        InvinciblePlayer removed = invinciblePlayers.remove(uhcPlayer);

        if (removed != null)
            tellInvincibleEnd(uhcPlayer.getPlayer());
    }

    private static void tellInvincibleEnd(Player player) {
        Chat.send(player, Messages.Game.NoClean.END);
        Extra.sound(player, Sounds.Game.INVINCIBLE_END);
    }

    public static InvinciblePlayer getInvinciblePlayer(UHCPlayer uhcPlayer) {
        return invinciblePlayers.get(uhcPlayer);
    }

    public static boolean isInvincible(UHCPlayer uhcPlayer) {
        return getInvinciblePlayer(uhcPlayer) != null;
    }

    public static boolean shouldCancelDamage(UHCPlayer uhcPlayer) {
        return isInvincible(uhcPlayer)
                && !LegacyFoundationAdapter.hasTempMetadata(uhcPlayer.getPlayer(), INVINCIBLE_DAMAGE_CANCEL_BYPASS_TAG);
    }

    public static void runBypassingInvincibleDamageCancel(Player player, Runnable runnable) {
        LegacyFoundationAdapter.setTempMetadata(player, INVINCIBLE_DAMAGE_CANCEL_BYPASS_TAG);
        try {
            runnable.run();
        } finally {
            LegacyFoundationAdapter.removeTempMetadata(player, INVINCIBLE_DAMAGE_CANCEL_BYPASS_TAG);
        }
    }

    private static class InvinciblePlayerTask extends BukkitRunnable {

        public void run() {
            Maps.newHashMap(invinciblePlayers).forEach((uhcPlayer, invinciblePlayer) -> {
                invinciblePlayer.time--;

                if (invinciblePlayer.time <= 0)
                    removeInvincible(uhcPlayer);

                if (Settings.Misc.NO_CLEAN_ACTION_BAR)
                    sendActionBar(uhcPlayer, invinciblePlayer.time);
            });
        }

        private void sendActionBar(UHCPlayer uhcPlayer, int time) {
            Player player = uhcPlayer.getPlayer();

            if (time > 0)
                LegacyFoundationAdapter.sendActionBar(player, LegacyFoundationAdapter.replaceTimePlaceholders(Messages.Game.NoClean.ACTION_BAR, time));
            else
                LegacyFoundationAdapter.sendActionBar(player, Messages.Game.NoClean.ACTION_BAR_END);
        }
    }
}
