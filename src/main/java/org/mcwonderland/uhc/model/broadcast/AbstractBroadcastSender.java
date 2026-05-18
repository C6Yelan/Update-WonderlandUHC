package org.mcwonderland.uhc.model.broadcast;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.model.GamePlaceholderReplacer;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.io.File;
import java.util.List;

/**
 * 2019-11-20 下午 07:50
 */
public abstract class AbstractBroadcastSender {

    private final ConfigurationSection section;
    private final List<String> broadCastMessageModel;

    protected AbstractBroadcastSender(String type) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(broadcastsFile());

        this.section = configuration.getConfigurationSection(type);
        broadCastMessageModel = getStringList("Formatting");
    }

    protected final String getString(String path) {
        return section == null ? null : section.getString(path);
    }

    protected final List<String> getStringList(String path) {
        return section == null ? List.of() : section.getStringList(path);
    }

    public final void sendBroadcast(GameStartTimeInfo info) {
        List<String> list = getPlaceholderReplacedNewList(info);
        send(list);
    }

    private List<String> getPlaceholderReplacedNewList(GameStartTimeInfo info) {
        List<String> list = GamePlaceholderReplacer.replace(broadCastMessageModel);

        list = PluginText.replaceToList(
                list,
                "{host}", Game.getGame().getHost(),
                "{ip}", info.getIp(),
                "{join_time}", info.getJoinTime(),
                "{start_time}", info.getStartTime());

        cleanColorCodes(list);

        return list;
    }

    private void cleanColorCodes(List<String> list) {
        for (int i = 0; i < list.size(); i++)
            list.set(i, PluginText.stripColors(list.get(i)));
    }

    protected abstract void send(List<String> messages);

    public abstract Dependency getDependency();

    private static File broadcastsFile() {
        return new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.BROADCAST);
    }

}
