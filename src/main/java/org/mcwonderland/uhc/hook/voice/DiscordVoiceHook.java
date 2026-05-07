package org.mcwonderland.uhc.hook.voice;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import lombok.Getter;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.listener.VoiceListener;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class DiscordVoiceHook {
    private static DiscordSRV discordSRV = DiscordSRV.getPlugin();
    private static Guild guild;
    private static Category voiceCategory;
    @Getter
    private static VoiceChannel lobbyVoice;
    @Getter
    private static TeamVoices teamVoices = new TeamVoices();
    private static Listener voiceListener = new VoiceListener();

    public static void setup() {
        HandlerList.unregisterAll(voiceListener);

        if (Settings.DiscordVoice.USE)
            new Thread(() -> {
                while (!DiscordSRV.isReady) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                discordSRV = DiscordSRV.getPlugin();
                guild = discordSRV.getJda().getGuildById(Settings.DiscordVoice.GUILD_ID);
                if (guild == null) {
                    LegacyFoundationAdapter.log("&cDiscord voice setup failed: guild not found: " + Settings.DiscordVoice.GUILD_ID);
                    return;
                }

                voiceCategory = guild.getCategoryById(Settings.DiscordVoice.VOICE_CATEGORY);
                if (voiceCategory == null) {
                    LegacyFoundationAdapter.log("&cDiscord voice setup failed: voice category not found: " + Settings.DiscordVoice.VOICE_CATEGORY);
                    return;
                }

                lobbyVoice = guild.getVoiceChannelById(Settings.DiscordVoice.LOBBY_VOICE);
                if (lobbyVoice == null) {
                    LegacyFoundationAdapter.log("&cDiscord voice setup failed: lobby voice channel not found: " + Settings.DiscordVoice.LOBBY_VOICE);
                    return;
                }

                clearChannels();
                LegacyFoundationAdapter.registerEvents(voiceListener);
            }).start();
    }

    private static void clearChannels() {
        voiceCategory.getChannels().forEach(ch -> {
            if (ch.getIdLong() != lobbyVoice.getIdLong())
                ch.delete().complete();
        });
    }

    public static VoiceChannel createHiddenChannel(String name) {
        VoiceChannel channel = voiceCategory.createVoiceChannel(name).complete();
        channel.putPermissionOverride(guild.getPublicRole()).setDeny(Permission.VOICE_CONNECT).queue();
        return channel;
    }

    public static void renameChannel(VoiceChannel channel, String newName) {
        channel.getManager().setName(newName).queue();
    }

    public static void moveVoiceChannel(UHCPlayer uhcPlayer, VoiceChannel channel) {
        Player player = uhcPlayer.getPlayer();
        if (channel == null) {
            Chat.send(player, Messages.DiscordVoice.MOVE_FAILED);
            LegacyFoundationAdapter.log("&cDiscord voice move failed for " + player.getName() + ": target channel is not available.");
            return;
        }

        Member member = getMember(player);

        if (member == null) {
            Chat.send(player, Messages.DiscordVoice.REQUIRES_LINKED_ACCOUNT);
            return;
        }

        if (!member.getVoiceState().inVoiceChannel()) {
            Chat.send(player, Messages.DiscordVoice.NOT_IN_VOICE);
            return;
        }

        String movedMsg = Messages.DiscordVoice.MOVED.replace("{channel}", channel.getName());
        guild.moveVoiceMember(member, channel).queue(
                aVoid -> Chat.send(player, movedMsg),
                error -> {
                    Chat.send(player, Messages.DiscordVoice.MOVE_FAILED);
                    LegacyFoundationAdapter.log("&cDiscord voice move failed for " + player.getName() + ": " + error.getMessage());
                });
    }

    public static void reconnect(UHCPlayer uhcPlayer) {
        if (uhcPlayer.isDead())
            moveToLobby(uhcPlayer);
        else if (uhcPlayer.getTeam() != null)
            moveToTeamVoice(uhcPlayer, uhcPlayer.getTeam());
        else
            moveToLobby(uhcPlayer);
    }

    public static void moveToTeamVoice(UHCPlayer uhcPlayer, UHCTeam team) {
        moveVoiceChannel(uhcPlayer, teamVoices.getChannel(team));
    }

    private static Member getMember(Player player) {
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        return discordId != null ? guild.getMemberById(discordId) : null;
    }

    public static void moveToLobby(UHCPlayer uhcPlayer) {
        moveVoiceChannel(uhcPlayer, lobbyVoice);
    }

    public static void moveToLobby(Member member) {
        guild.moveVoiceMember(member, lobbyVoice).queue(
                ignored -> {
                },
                error -> LegacyFoundationAdapter.log("&cDiscord voice lobby move failed for " + member.getEffectiveName() + ": " + error.getMessage()));
    }

}
