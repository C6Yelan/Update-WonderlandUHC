package org.mcwonderland.uhc.model.deathmsg;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeathMessageLoader {
    private static final String PLAYER_DEATH_PATH = "Game.PlayerDeath";

    private HashMap<EntityDamageEvent.DamageCause, List<String>> messages = new HashMap<>();
    private List<String> defaultMessages;
    private List<String> playerKilledMessages;

    public DeathMessageLoader() {
        loadMessages();
    }

    private void loadMessages() {
        File file = new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.MESSAGES);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

        if (!configuration.isSet(PLAYER_DEATH_PATH)) {
            configuration.createSection(PLAYER_DEATH_PATH);
            save(configuration, file);
        }

        ConfigurationSection section = configuration.getConfigurationSection(PLAYER_DEATH_PATH);

        this.defaultMessages = configuration.getStringList(PLAYER_DEATH_PATH + ".Other");
        this.playerKilledMessages = configuration.getStringList(PLAYER_DEATH_PATH + ".Player_Killed");

        if (section == null)
            return;

        section.getKeys(false).forEach(key -> {
            List<String> messages = section.getStringList(key);
            Set<EntityDamageEvent.DamageCause> causes = getEnumCauses(key);

            causes.forEach(damageCause -> this.messages.put(damageCause, messages));
        });
    }

    private void save(YamlConfiguration configuration, File file) {
        try {
            configuration.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save " + UHCFiles.MESSAGES, ex);
        }
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
