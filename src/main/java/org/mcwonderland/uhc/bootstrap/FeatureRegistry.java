package org.mcwonderland.uhc.bootstrap;

import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.event.Listener;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.event.scenario.ScenarioInitEvent;
import org.mcwonderland.uhc.command.TestCommand;
import org.mcwonderland.uhc.command.impl.LeaveCommand;
import org.mcwonderland.uhc.command.impl.game.BackPackCommand;
import org.mcwonderland.uhc.command.impl.game.PracticeCommand;
import org.mcwonderland.uhc.command.impl.game.ReconnectCommand;
import org.mcwonderland.uhc.command.impl.game.SendCoordsCommand;
import org.mcwonderland.uhc.command.impl.game.SpecToggleCommand;
import org.mcwonderland.uhc.command.impl.host.BorderCommand;
import org.mcwonderland.uhc.command.impl.host.GiveAllCommand;
import org.mcwonderland.uhc.command.impl.host.MLGCommand;
import org.mcwonderland.uhc.command.impl.host.RespawnCommand;
import org.mcwonderland.uhc.command.impl.host.SetSpawnCommand;
import org.mcwonderland.uhc.command.impl.host.StaffCommand;
import org.mcwonderland.uhc.command.impl.info.ConfigCommand;
import org.mcwonderland.uhc.command.impl.info.DisableItemsCommand;
import org.mcwonderland.uhc.command.impl.info.ScenariosCommand;
import org.mcwonderland.uhc.command.impl.info.StatsCommand;
import org.mcwonderland.uhc.command.impl.info.TopKillsCommand;
import org.mcwonderland.uhc.command.impl.info.ViewHealCommand;
import org.mcwonderland.uhc.command.impl.host.whitelist.WhitelistCommandGroup;
import org.mcwonderland.uhc.command.team.TeamCommandGroup;
import org.mcwonderland.uhc.command.uhc.UHCMainCommandGroup;
import org.mcwonderland.uhc.hook.voice.DiscordVoiceHook;
import org.mcwonderland.uhc.application.world.ChunkPregenerationService;
import org.mcwonderland.uhc.integration.ChunkPregenerationAdapters;
import org.mcwonderland.uhc.integration.worldborder.LegacyWorldBorderFillListener;
import org.mcwonderland.uhc.integration.worldborder.LegacyWorldBorderPregenerationAdapter;
import org.mcwonderland.uhc.listener.BooleanEvents;
import org.mcwonderland.uhc.listener.ChatListener;
import org.mcwonderland.uhc.listener.DamageListener;
import org.mcwonderland.uhc.listener.GameSettingsScenarioListener;
import org.mcwonderland.uhc.listener.InvViewListener;
import org.mcwonderland.uhc.listener.OldEnchantListener;
import org.mcwonderland.uhc.listener.StatsListener;
import org.mcwonderland.uhc.listener.ToolListener;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter.CommandGroupRegistrar;
import org.mcwonderland.uhc.model.tutorial.model.TutorialListener;
import org.mcwonderland.uhc.practice.Practice;
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
                new InvViewListener(),
                new GameSettingsScenarioListener(plugin),
                new OldEnchantListener(),
                new StatsListener(),
                new ToolListener(),
                new ScoreListener(),
                new ScenarioListener(plugin)
        ).forEach(registerListener);

        if (ChunkPregenerationAdapters.usesLegacyWorldBorder())
            registerListener.accept(new LegacyWorldBorderFillListener(
                    new ChunkPregenerationService(new LegacyWorldBorderPregenerationAdapter())
            ));
    }

    public void registerCommands(Consumer<Command> registerCommand) {
        Lists.newArrayList(
                new BackPackCommand("backpack|bp"),
                new ConfigCommand("config|cfg"),
                new DisableItemsCommand("disableitems"),
                new GiveAllCommand("giveall"),
                new LeaveCommand("leave"),
                new MLGCommand("mlg"),
                new RespawnCommand("respawn"),
                new ScenariosCommand("scenarios"),
                new SendCoordsCommand("sendcoords|scs"),
                new SetSpawnCommand("setspawn"),
                new BorderCommand("border"),
                new SpecToggleCommand("spectoggle"),
                new StaffCommand("staff"),
                new StatsCommand("stats"),
                new TopKillsCommand("topkills|killtop|kt"),
                new ViewHealCommand("viewheal|h"),
                new PracticeCommand("practice"),
                new ReconnectCommand("reconnect")
        ).forEach(registerCommand);

        if (WonderlandUHC.TEST_MODE)
            registerCommand.accept(new TestCommand("test"));
    }

    public void registerCommandGroups(CommandGroupRegistrar registerCommandGroup) {
        Lists.newArrayList(
                new UHCMainCommandGroup("uhc"),
                new TeamCommandGroup("team"),
                new WhitelistCommandGroup("whitelist|wl")
        ).forEach(registerCommandGroup::register);
    }

    public void registerDefaultScenarios() {
        plugin.getScenarioManager().registerDefaults();
        LegacyFoundationAdapter.callEvent(new ScenarioInitEvent());
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
