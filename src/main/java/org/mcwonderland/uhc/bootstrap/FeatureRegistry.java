package org.mcwonderland.uhc.bootstrap;

import org.mcwonderland.uhc.platform.event.PluginEvents;
import com.google.common.collect.Lists;
import org.bukkit.event.Listener;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.event.scenario.ScenarioInitEvent;
import org.mcwonderland.uhc.command.impl.game.BackPackCommand;
import org.mcwonderland.uhc.command.impl.game.PracticeCommand;
import org.mcwonderland.uhc.command.impl.game.ReconnectCommand;
import org.mcwonderland.uhc.command.impl.game.SendCoordsCommand;
import org.mcwonderland.uhc.command.impl.game.SpecToggleCommand;
import org.mcwonderland.uhc.command.impl.host.BorderCommand;
import org.mcwonderland.uhc.command.impl.host.GiveAllCommand;
import org.mcwonderland.uhc.command.impl.host.InventoryEditorInputCommand;
import org.mcwonderland.uhc.command.impl.host.RespawnCommand;
import org.mcwonderland.uhc.command.impl.host.SetSpawnCommand;
import org.mcwonderland.uhc.command.impl.host.StaffCommand;
import org.mcwonderland.uhc.command.impl.info.ConfigCommand;
import org.mcwonderland.uhc.command.impl.info.DisableItemsCommand;
import org.mcwonderland.uhc.command.impl.info.ScenariosCommand;
import org.mcwonderland.uhc.command.impl.info.StatsCommand;
import org.mcwonderland.uhc.command.impl.info.TopKillsCommand;
import org.mcwonderland.uhc.command.impl.host.whitelist.WhitelistCommand;
import org.mcwonderland.uhc.command.team.TeamCommand;
import org.mcwonderland.uhc.command.uhc.UHCCommand;
import org.mcwonderland.uhc.hook.voice.DiscordVoiceHook;
import org.mcwonderland.uhc.listener.BooleanEvents;
import org.mcwonderland.uhc.listener.ChatListener;
import org.mcwonderland.uhc.listener.DamageListener;
import org.mcwonderland.uhc.listener.ExperiencePickupListener;
import org.mcwonderland.uhc.listener.GameSettingsScenarioListener;
import org.mcwonderland.uhc.listener.InvViewListener;
import org.mcwonderland.uhc.listener.StatsListener;
import org.mcwonderland.uhc.listener.ToolListener;
import org.mcwonderland.uhc.model.tutorial.model.TutorialListener;
import org.mcwonderland.uhc.practice.Practice;
import org.mcwonderland.uhc.platform.menu.PluginMenuListener;
import org.mcwonderland.uhc.scenario.ScenarioListener;
import org.mcwonderland.uhc.scoreboard.ScoreBoardUpdater;
import org.mcwonderland.uhc.scoreboard.ScoreListener;
import org.mcwonderland.uhc.scoreboard.SidebarTheme;

import java.util.function.Consumer;

public final class FeatureRegistry {

    private final WonderlandUHC plugin;

    public FeatureRegistry(WonderlandUHC plugin) {
        this.plugin = plugin;
    }

    public void registerListeners(Consumer<Listener> registerListener) {
        Lists.newArrayList(
                new BooleanEvents(),
                new ChatListener(),
                new TutorialListener(),
                new DamageListener(),
                new ExperiencePickupListener(),
                new InvViewListener(),
                new GameSettingsScenarioListener(plugin),
                new StatsListener(),
                new ToolListener(),
                new PluginMenuListener(),
                new ScoreListener(),
                new ScenarioListener(plugin)
        ).forEach(registerListener);
    }

    public void registerNativeCommands() {
        ConfigCommand.register(plugin);
        DisableItemsCommand.register(plugin);
        InventoryEditorInputCommand.register(plugin);
        ScenariosCommand.register(plugin);
        SetSpawnCommand.register(plugin);
        StaffCommand.register(plugin);
        ReconnectCommand.register(plugin);
        TopKillsCommand.register(plugin);
        StatsCommand.register(plugin);
        SendCoordsCommand.register(plugin);
        SpecToggleCommand.register(plugin);
        BackPackCommand.register(plugin);
        RespawnCommand.register(plugin);
        BorderCommand.register(plugin);
        GiveAllCommand.register(plugin);
        WhitelistCommand.register(plugin);
        PracticeCommand.register(plugin);
        TeamCommand.register(plugin);
        UHCCommand.register(plugin);
    }

    public void registerDefaultScenarios() {
        plugin.getScenarioManager().registerDefaults();
        PluginEvents.callEvent(new ScenarioInitEvent());
    }

    public void loadScoreboardThemes() {
        SidebarTheme.loadThemes();
    }

    public void startScoreboardUpdater() {
        ScoreBoardUpdater.start();
    }

    public void setupPractice(Practice practice) {
        practice.setup();
    }

    public void setupDiscordVoiceHook() {
        if (Dependency.DISCORD_SRV.isHooked())
            DiscordVoiceHook.setup();
    }
}
