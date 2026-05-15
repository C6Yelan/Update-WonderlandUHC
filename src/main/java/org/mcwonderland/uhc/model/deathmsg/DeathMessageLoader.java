package org.mcwonderland.uhc.model.deathmsg;

import org.mcwonderland.uhc.settings.UHCFiles;
import org.bukkit.event.entity.EntityDamageEvent;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeathMessageLoader extends YamlConfig {
    private HashMap<EntityDamageEvent.DamageCause, List<String>> messages = new HashMap<>();
    private List<String> defaultMessages;
    private List<String> playerKilledMessages;

    public DeathMessageLoader() {
        setPathPrefix("Game");
        loadMessages();
    }

    private void loadMessages() {
        loadConfiguration(UHCFiles.MESSAGES);

        if (!isSet("PlayerDeath")) {
            set("PlayerDeath", new HashMap<>());
            save();
        }

        this.defaultMessages = getStringList("PlayerDeath.Other");
        this.playerKilledMessages = getStringList("PlayerDeath.Player_Killed");

        getMap("PlayerDeath").forEach((s, o) -> {
            List<String> messages = ( List<String> ) o;
            Set<EntityDamageEvent.DamageCause> causes = getEnumCauses(s);

            causes.forEach(damageCause -> this.messages.put(damageCause, messages));
        });
    }

    public List<String> getDefaultMessages() {
        return new ArrayList<>(defaultMessages);
    }

    public List<String> getPlayerKilledMessages() {
        return new ArrayList<>(playerKilledMessages);
    }

    public List<String> getDeathMessages(EntityDamageEvent.DamageCause cause) {
        return messages.get(cause);
    }


    private Set<EntityDamageEvent.DamageCause> getEnumCauses(String stringCauses) {
        String[] split = stringCauses.split(",");
        Set<EntityDamageEvent.DamageCause> enumCauses = new HashSet<>();

        for (String s : split) {
            try {
                enumCauses.add(EntityDamageEvent.DamageCause.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException ex) {
            }
        }

        return enumCauses;
    }
}
