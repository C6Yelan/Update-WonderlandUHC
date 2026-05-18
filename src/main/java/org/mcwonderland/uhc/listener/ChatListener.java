package org.mcwonderland.uhc.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.menu.impl.game.TeamSettingsMenu;
import org.mcwonderland.uhc.menu.impl.host.BorderSettingsMenu;
import org.mcwonderland.uhc.menu.impl.host.MainSettingsMenu;
import org.mcwonderland.uhc.menu.impl.host.TimeSettingsMenu;
import org.mcwonderland.uhc.model.broadcast.GameStartTimeInputSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    //todo 重寫 (變成 ricipent?)
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(e.message());

        if (TeamSettingsMenu.handleInput(player, message)) {
            e.setCancelled(true);
            return;
        }

        if (TimeSettingsMenu.handleInput(player, message)) {
            e.setCancelled(true);
            return;
        }

        if (BorderSettingsMenu.handleInput(player, message)) {
            e.setCancelled(true);
            return;
        }

        if (MainSettingsMenu.handleInput(player, message)) {
            e.setCancelled(true);
            return;
        }

        if (GameStartTimeInputSession.handleInput(player, message)) {
            e.setCancelled(true);
            return;
        }

        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(player);

        e.setCancelled(true);
        uhcPlayer.chat(message);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        TeamSettingsMenu.clear(e.getPlayer());
        TimeSettingsMenu.clear(e.getPlayer());
        BorderSettingsMenu.clear(e.getPlayer());
        MainSettingsMenu.clear(e.getPlayer());
        GameStartTimeInputSession.clear(e.getPlayer());
    }
}
