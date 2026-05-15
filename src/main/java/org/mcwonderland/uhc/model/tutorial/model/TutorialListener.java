package org.mcwonderland.uhc.model.tutorial.model;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class TutorialListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        System.out.println("Hello");
        Player player = e.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(e.message());

        Tutorial tutorial = Tutorial.getCurrentTutorial(player);

        if (tutorial == null)
            return;

        e.setCancelled(true);

        if (message.equalsIgnoreCase(Tutorial.CANCELLER)) {
            Tutorial.exit(player);
            Extra.sound(player, Sounds.Tutorial.CANCELLED);
            return;
        }

        tutorial.showNextSection();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Tutorial.exit(e.getPlayer());
    }
}
