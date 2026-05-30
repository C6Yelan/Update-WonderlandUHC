package org.mcwonderland.uhc.hook.voice;

import org.mcwonderland.uhc.platform.event.PluginEvents;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.GuildVoiceState;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import lombok.Getter;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.listener.VoiceListener;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Objects;

public class DiscordVoiceHook {
    private static final int DISCORD_READY_WAIT_ATTEMPTS = 120;
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

        if (Settings.DiscordVoice.USE) {
            Thread setupThread = new Thread(() -> {
                int waitAttempts = 0;

                while (!DiscordSRV.isReady && waitAttempts < DISCORD_READY_WAIT_ATTEMPTS) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    waitAttempts++;
                }

                if (!DiscordSRV.isReady) {
                    PluginConsole.log("<red>Discord voice setup failed: DiscordSRV is not ready.</red>");
                    return;
                }

                discordSRV = DiscordSRV.getPlugin();
                String guildId = Settings.DiscordVoice.GUILD_ID;
                if (guildId == null || guildId.isBlank()) {
                    PluginConsole.log("<red>Discord voice setup failed: guild id is not configured.</red>");
                    return;
                }

                guild = discordSRV.getJda().getGuildById(guildId);
                if (guild == null) {
                    PluginConsole.log("<red>Discord voice setup failed: guild not found: " + guildId + "</red>");
                    return;
                }

                String voiceCategoryId = Settings.DiscordVoice.VOICE_CATEGORY;
                if (voiceCategoryId == null || voiceCategoryId.isBlank()) {
                    PluginConsole.log("<red>Discord voice setup failed: voice category id is not configured.</red>");
                    return;
                }

                voiceCategory = guild.getCategoryById(voiceCategoryId);
                if (voiceCategory == null) {
                    PluginConsole.log("<red>Discord voice setup failed: voice category not found: " + voiceCategoryId + "</red>");
                    return;
                }

                String lobbyVoiceId = Settings.DiscordVoice.LOBBY_VOICE;
                if (lobbyVoiceId == null || lobbyVoiceId.isBlank()) {
                    PluginConsole.log("<red>Discord voice setup failed: lobby voice channel id is not configured.</red>");
                    return;
                }

                lobbyVoice = guild.getVoiceChannelById(lobbyVoiceId);
                if (lobbyVoice == null) {
                    PluginConsole.log("<red>Discord voice setup failed: lobby voice channel not found: " + lobbyVoiceId + "</red>");
                    return;
                }

                clearChannels();
                PluginEvents.registerEvents(voiceListener);
            }, "WonderlandUHC-DiscordVoiceHook");
            setupThread.setDaemon(true);
            setupThread.start();
        }
    }

    private static void clearChannels() {
        voiceCategory.getChannels().forEach(ch -> {
            if (ch.getIdLong() != lobbyVoice.getIdLong())
                ch.delete().complete();
        });
    }

    public static VoiceChannel createHiddenChannel(String name) {
        VoiceChannel channel = voiceCategory.createVoiceChannel(Objects.requireNonNull(name, "name")).complete();
        channel.putPermissionOverride(guild.getPublicRole()).setDeny(Permission.VOICE_CONNECT).queue();
        return channel;
    }

    public static void renameChannel(VoiceChannel channel, String newName) {
        channel.getManager().setName(Objects.requireNonNull(newName, "newName")).queue();
    }

    public static void moveVoiceChannel(UHCPlayer uhcPlayer, VoiceChannel channel) {
        Player player = uhcPlayer.getPlayer();
        if (channel == null) {
            Chat.send(player, Messages.DiscordVoice.MOVE_FAILED);
            PluginConsole.log("<red>Discord voice move failed for " + player.getName() + ": target channel is not available.</red>");
            return;
        }

        Member member = getMember(player);

        if (member == null) {
            Chat.send(player, Messages.DiscordVoice.REQUIRES_LINKED_ACCOUNT);
            return;
        }

        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inVoiceChannel()) {
            Chat.send(player, Messages.DiscordVoice.NOT_IN_VOICE);
            return;
        }

        String movedMsg = PluginText.replaceToString(
                Messages.DiscordVoice.MOVED,
                "{channel}", channel.getName()
        );
        guild.moveVoiceMember(member, channel).queue(
                aVoid -> Chat.send(player, movedMsg),
                error -> {
                    Chat.send(player, Messages.DiscordVoice.MOVE_FAILED);
                    PluginConsole.log("<red>Discord voice move failed for " + player.getName() + ": " + error.getMessage() + "</red>");
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
        if (member == null || lobbyVoice == null)
            return;

        guild.moveVoiceMember(member, lobbyVoice).queue(
                ignored -> {
                },
                error -> PluginConsole.log("<red>Discord voice lobby move failed for " + member.getEffectiveName() + ": " + error.getMessage() + "</red>"));
    }

}
