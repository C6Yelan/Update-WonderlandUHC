package org.mcwonderland.uhc.tools.lobby;

import lombok.Getter;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mcwonderland.uhc.tools.UHCTool;

public final class ScenariosItem extends UHCTool {

    @Getter
    private static final ScenariosItem instance = new ScenariosItem();

    private ScenariosItem() {
        super("Lobby.Scenarios");
    }

    @Override
    protected void onRightClick(PlayerInteractEvent event) {
        event.getPlayer().performCommand("scenarios");
    }
}
