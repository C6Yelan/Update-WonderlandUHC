package org.mcwonderland.uhc.tools.lobby;

import lombok.Getter;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mcwonderland.uhc.tools.UHCTool;

public final class DisableItemsItem extends UHCTool {

    @Getter
    private static final DisableItemsItem instance = new DisableItemsItem();

    private DisableItemsItem() {
        super("Lobby.Disable_Items");
    }

    @Override
    protected void onRightClick(PlayerInteractEvent event) {
        event.getPlayer().performCommand("disableitems");
    }
}
