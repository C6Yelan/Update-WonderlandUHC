package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.model.broadcast.AbstractBroadcastSender;
import org.mcwonderland.uhc.model.broadcast.BroadcastDeliveryException;
import org.mcwonderland.uhc.model.broadcast.GameStartTimeInputSession;
import org.mcwonderland.uhc.model.broadcast.impl.DiscordBroadcastSender;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;

public class BroadcastSettingsMenu extends PluginMenu {
    private static final String SECTION = "Broadcast";
    private static final String DISCORD_BUTTON = "Discord";
    private static final int BACK_OFFSET = 1;

    public BroadcastSettingsMenu() {
        super(PluginMenuSection.of(SECTION));
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getBackButtonSlot())
            return PluginItems.fromConfig(UHCFiles.MENUS, "Leave");

        if (slot == getSection().getButtonSlot(DISCORD_BUTTON))
            return getSection().getButtonItem(DISCORD_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getBackButtonSlot()) {
            new MainSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(DISCORD_BUTTON))
            startDiscordBroadcast(player);
    }

    private void startDiscordBroadcast(Player player) {
        Extra.sound(player, Sounds.Host.SCENARIO_TOGGLED);

        if (!Dependency.DISCORD_SRV.isHooked()) {
            Chat.send(player, PluginText.replaceToString(
                    Messages.Dependency.REQUIRE_SOFT_DEPENDENCY,
                    "{plugin}", Dependency.DISCORD_SRV.getPluginName(),
                    "{url}", Dependency.DISCORD_SRV.getDownloadUrl()
            ));
            return;
        }

        AbstractBroadcastSender sender = new DiscordBroadcastSender();
        GameStartTimeInputSession.start(player, info -> {
            try {
                sender.sendBroadcast(info);
                Chat.send(player, "<green>Discord 公告已送出。</green>");
            } catch (BroadcastDeliveryException e) {
                Chat.send(player, e.getMessage());
            }
        });
    }

    private int getBackButtonSlot() {
        return getSection().getSize() - BACK_OFFSET;
    }
}
