package org.mcwonderland.uhc.tools.lobby;

import lombok.Getter;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mcwonderland.uhc.tools.UHCTool;

public final class PracticeItem extends UHCTool {

    @Getter
    private static final PracticeItem instance = new PracticeItem();

    private PracticeItem() {
        super("Lobby.Practice");
    }

    @Override
    protected void onRightClick(PlayerInteractEvent event) {
        event.getPlayer().performCommand("practice");
    }
}
