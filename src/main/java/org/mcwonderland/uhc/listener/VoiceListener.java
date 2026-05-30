package org.mcwonderland.uhc.listener;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import org.mcwonderland.uhc.api.event.team.TeamCreatedEvent;
import org.mcwonderland.uhc.api.event.team.TeamDisbandedEvent;
import org.mcwonderland.uhc.api.event.team.TeamJoinedEvent;
import org.mcwonderland.uhc.api.event.team.TeamQuitedEvent;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.hook.voice.DiscordVoiceHook;
import org.mcwonderland.uhc.hook.voice.TeamVoices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.concurrent.TimeUnit;

public class VoiceListener implements Listener {

    private TeamVoices teamVoices = DiscordVoiceHook.getTeamVoices();

    @EventHandler
    public void onTeamCreated(TeamCreatedEvent e) {
        UHCTeam team = e.getTeam();

        PluginScheduler.runAsync(() -> {
            VoiceChannel channel = DiscordVoiceHook.createHiddenChannel(team.getName());

            teamVoices.add(team, channel);
            team.getPlayers().forEach(uhcPlayer -> DiscordVoiceHook.moveToTeamVoice(uhcPlayer, team));
        });
    }

    @EventHandler
    public void onTeamDisbanded(TeamDisbandedEvent e) {
        VoiceChannel voiceChannel = teamVoices.remove(e.getTeam());

        if (voiceChannel == null)
            return;

        voiceChannel.getMembers().forEach(DiscordVoiceHook::moveToLobby);
        voiceChannel.delete().queueAfter(1000, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onJoinedTeam(TeamJoinedEvent e) {
        UHCPlayer uhcPlayer = e.getPlayer();
        if (teamVoices.getChannel(e.getTeam()) == null)
            return;

        DiscordVoiceHook.moveToTeamVoice(uhcPlayer, e.getTeam());
    }

    @EventHandler
    public void onQuitedTeam(TeamQuitedEvent e) {
        DiscordVoiceHook.moveToLobby(e.getPlayer());
    }
}
