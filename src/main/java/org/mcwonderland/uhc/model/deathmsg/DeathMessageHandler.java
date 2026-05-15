package org.mcwonderland.uhc.model.deathmsg;

import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public class DeathMessageHandler {

    private static DeathMessageHandler instance;

    private DeathMessageLoader messageLoader = new DeathMessageLoader();

    private UHCPlayer uhcPlayer;
    private EntityDamageEvent damageEvent;

    private DeathMessageHandler() {

    }

    public static DeathMessageHandler getInstance() {
        if (instance == null)
            instance = new DeathMessageHandler();
        return instance;
    }

    public String getDeathMessage(UHCPlayer uhcPlayer) {
        this.uhcPlayer = uhcPlayer;
        this.damageEvent = uhcPlayer.getPlayer().getLastDamageCause();

        List<String> messages = getMessages(damageEvent);
        String msg = pickOne(messages);

        return format(msg);
    }

    private String format(String msg) {
        List<Object> replacements = new ArrayList<>();
        UHCTeam playerTeam = uhcPlayer.getTeam();

        replacements.add("{player}");
        replacements.add(playerTeam.getChatFormat() + uhcPlayer.getName());
        replacements.add("{playerKills}");
        replacements.add(playerTeam.getKills() + "");

        if (damageEvent instanceof EntityDamageByEntityEvent)
            addReplacement(replacements, "{entity}", (( EntityDamageByEntityEvent ) damageEvent).getDamager().getName());

        Player killer = uhcPlayer.getPlayer().getKiller();
        if (killer != null) {
            UHCPlayer uhcKiller = UHCPlayer.getUHCPlayer(killer);
            UHCTeam killerTeam = uhcKiller.getTeam();
            addReplacement(replacements, "{killer}", killerTeam.getChatFormat() + uhcKiller.getName());
            addReplacement(replacements, "{killerKills}", killerTeam.getKills() + "");
        }

        return LegacyFoundationAdapter.replaceToString(msg, replacements.toArray());
    }

    private void addReplacement(List<Object> replacements, String placeholder, Object value) {
        replacements.add(placeholder);
        replacements.add(value);
    }

    private String pickOne(List<String> messages) {
        if (messages.isEmpty())
            return "";

        return LegacyFoundationAdapter.nextItem(messages);
    }

    private List<String> getMessages(EntityDamageEvent damageEvent) {
        if (damageEvent == null)
            return messageLoader.getDefaultMessages();

        return getMessagesByCause(damageEvent.getCause());
    }

    private List<String> getMessagesByCause(EntityDamageEvent.DamageCause cause) {
        Player killer = uhcPlayer.getEntity().getKiller();
        if (killer != null && killer != uhcPlayer.getPlayer())
            return messageLoader.getPlayerKilledMessages();

        List<String> deathMessages = messageLoader.getDeathMessages(cause);

        if (deathMessages == null)
            deathMessages = messageLoader.getDefaultMessages();

        return deathMessages;
    }

}
