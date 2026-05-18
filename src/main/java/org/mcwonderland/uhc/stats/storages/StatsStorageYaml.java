package org.mcwonderland.uhc.stats.storages;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.stats.UHCStats;

import java.io.File;
import java.io.IOException;

public class StatsStorageYaml implements StatsStorage {

    private final File file;
    private final YamlConfiguration configuration;

    public StatsStorageYaml() {
        this.file = new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.STATS);
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public UHCStats loadOrCreate(UHCPlayer uhcPlayer) {
        String path = uhcPlayer.getUniqueId().toString();
        UHCStats stats = new UHCStats();

        stats.gamePlayed = configuration.getInt(path + ".Game_Played", 0);
        stats.totalKills = configuration.getInt(path + ".Kills", 0);
        stats.totalWins = configuration.getInt(path + ".Wins", 0);

        return stats;
    }

    @Override
    public void save(UHCPlayer uhcPlayer) {
        String path = uhcPlayer.getUniqueId().toString();
        UHCStats stats = uhcPlayer.getStats();

        configuration.set(path + ".Game_Played", stats.gamePlayed);
        configuration.set(path + ".Kills", stats.totalKills);
        configuration.set(path + ".Wins", stats.totalWins);

        try {
            configuration.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save " + UHCFiles.STATS, ex);
        }
    }
}
