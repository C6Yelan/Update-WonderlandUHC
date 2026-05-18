package org.mcwonderland.uhc.menu.impl.host;

import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.menu.UHCMenuSection;
import org.mcwonderland.uhc.model.broadcast.AbstractBroadcastSender;
import org.mcwonderland.uhc.model.broadcast.BroadcastDeliveryException;
import org.mcwonderland.uhc.model.broadcast.GameStartTimeInputSession;
import org.mcwonderland.uhc.model.broadcast.impl.DiscordBroadcastSender;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.config.ConfigClickableButton;
import org.mineacademy.fo.menu.config.ConfigMenu;
import org.mineacademy.fo.menu.config.ItemPath;

public class BroadcastSettingsMenu extends ConfigMenu {

    private final BroadcastButton discordButton;

    public BroadcastSettingsMenu(Menu parent) {
        super(UHCMenuSection.of("Broadcast"), parent);

        discordButton = new BroadcastButton(getButtonPath("Discord"));
    }


    private class BroadcastButton extends ConfigClickableButton {

        protected BroadcastButton(ItemPath path) {
            super(path);
        }

        @Override
        protected void onClicked(Player player, Menu menu, ClickType click) {
            if (!Dependency.DISCORD_SRV.isHooked()) {
                Chat.send(player, Messages.Dependency.REQUIRE_SOFT_DEPENDENCY
                        .replace("{plugin}", Dependency.DISCORD_SRV.getPluginName())
                        .replace("{url}", Dependency.DISCORD_SRV.getDownloadUrl()));
                return;
            }

            AbstractBroadcastSender sender = new DiscordBroadcastSender();
            GameStartTimeInputSession.start(player, info -> {
                try {
                    sender.sendBroadcast(info);
                    Chat.send(player, "&aDiscord 公告已送出。");
                } catch (BroadcastDeliveryException e) {
                    Chat.send(player, e.getMessage());
                }
            });
        }
    }

}
