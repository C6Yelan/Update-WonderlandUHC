package org.mcwonderland.uhc;

import lombok.Getter;
import org.mcwonderland.uhc.bootstrap.FeatureRegistry;
import org.mcwonderland.uhc.bootstrap.PluginBootstrap;
import org.mcwonderland.uhc.game.settings.UHCGameSettingsSaver;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.practice.Practice;
import org.mcwonderland.uhc.practice.SimplePractice;
import org.mcwonderland.uhc.scenario.ScenarioManager;
import org.mcwonderland.uhc.settings.spawn.Spawns;
import org.mcwonderland.uhc.stats.storages.StatsStorage;
import org.mineacademy.fo.plugin.SimplePlugin;

public class WonderlandUHC extends SimplePlugin {
    public static Boolean TEST_MODE = false;
    @Getter
    private static StatsStorage statsStorage;
    @Getter
    private ScenarioManager scenarioManager = new ScenarioManager();
    @Getter
    private Practice practice = new SimplePractice();

    //todo
    // 觀察者推礦車
    // redglass border
    // 讓剛購買的客戶永遠能下載最新版本插件
    // reload 後 scenario 會消失
    // generating 時沒有禁止玩家登入，motd 依然顯示開放入場

    public static WonderlandUHC getInstance() {
        return ( WonderlandUHC ) SimplePlugin.getInstance();
    }

    public WonderlandUHC() {
    }

    @Override
    public void onPluginStart() {
        PluginBootstrap bootstrap = new PluginBootstrap(this);
        FeatureRegistry featureRegistry = new FeatureRegistry(this);
        bootstrap.loadFiles();
        featureRegistry.registerListeners(this::registerEvents);
        featureRegistry.registerCommandGroups(LegacyFoundationAdapter.commandGroupRegistrar(this::registerCommands));
        featureRegistry.registerCommands(this::registerCommand);
        bootstrap.createPluginAssets();
        featureRegistry.setupPractice(practice);
        featureRegistry.setupDiscordVoiceHook();

        bootstrap.scheduleDelayedStartupTasks(featureRegistry);

        LegacyFoundationAdapter.setTellPrefix("");
    }

    @Override
    protected void onPluginReload() {
        new FeatureRegistry(this).registerCommandGroups(LegacyFoundationAdapter.commandGroupRegistrar(this::registerCommands));
        scenarioManager.reloadAll();
        UHCGameSettingsSaver.reloadFromFile();
    }

    @Override
    protected void onReloadablesStart() {
        PluginBootstrap bootstrap = new PluginBootstrap(this);
        FeatureRegistry featureRegistry = new FeatureRegistry(this);

        bootstrap.checkDependencies();

        bootstrap.registerPluginChannels();
        bootstrap.configureFoundationLibrary();

        featureRegistry.loadScoreboardThemes();
        Spawns.reload();

        statsStorage = bootstrap.loadStatsStorage();

        if (bootstrap.isWorldLoadingDone()) {
            featureRegistry.startScoreboardUpdater();
            bootstrap.startRuntimeTasks();
        }

        bootstrap.applyTestModeSettings();
    }

}
