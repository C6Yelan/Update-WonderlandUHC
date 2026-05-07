package org.mcwonderland.uhc.command.impl.game;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.hook.voice.DiscordVoiceHook;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.Chat;
import org.mineacademy.fo.command.SimpleCommand;

public class ReconnectCommand extends SimpleCommand {

    public ReconnectCommand(String label) {
        super(label);

        setDescription("重新連結 Discord 語音頻道。");
        setPermission(UHCPermission.COMMAND_RECONNECT.toString());
    }

    @Override
    protected void onCommand() {
        Dependency.DISCORD_SRV.checkSoft();
        checkBoolean(Settings.DiscordVoice.USE, Messages.DiscordVoice.MOVE_FAILED);

        Player player = getPlayer();
        Chat.send(player, Messages.DiscordVoice.RECONNECTING);
        DiscordVoiceHook.reconnect(UHCPlayer.getUHCPlayer(player));
    }
}
