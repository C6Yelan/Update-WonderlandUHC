package org.mcwonderland.uhc.tools.lobby;

import lombok.Getter;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mcwonderland.uhc.tools.UHCTool;

public final class ConfigItem extends UHCTool {

    @Getter
    private static final ConfigItem instance = new ConfigItem();

    private ConfigItem() {
        super("Lobby.Config");
    }

    @Override
    protected void onRightClick(PlayerInteractEvent event) {
        event.getPlayer().performCommand("config");
    }
}
