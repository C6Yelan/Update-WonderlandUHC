package org.mcwonderland.uhc.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@UtilityClass
public class Chat {
    private final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private final char LEGACY_COLOR_CHAR = LegacyComponentSerializer.SECTION_CHAR;

    public void send(CommandSender sender, List<String> messages) {
        send(sender, messages.toArray(new String[0]));
    }

    public void send(CommandSender sender, String... messages) {
        for (String message : messages) {
            sender.sendMessage(toComponent(message));
        }
    }

    public void sendConversing(Player sender, String... messages) {
        for (String message : messages) {
            sender.sendRawMessage(toLegacySection(message));
        }
    }

    public void broadcast(String... messages) {
        Bukkit.getOnlinePlayers().forEach(player -> send(player, messages));
    }

    public void broadcastWithPerm(String permission, String... messages) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> {
                    send(player, messages);
                });
    }

    private Component toComponent(String message) {
        return LEGACY_SECTION.deserialize(toLegacySection(message));
    }

    private String toLegacySection(String message) {
        char[] chars = message.toCharArray();

        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = LEGACY_COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }
}
