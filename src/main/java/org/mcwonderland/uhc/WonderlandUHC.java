package org.mcwonderland.uhc;

import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcwonderland.uhc.bootstrap.FeatureRegistry;
import org.mcwonderland.uhc.bootstrap.PluginBootstrap;
import org.mcwonderland.uhc.game.settings.UHCGameSettingsSaver;
import org.mcwonderland.uhc.practice.Practice;
import org.mcwonderland.uhc.practice.SimplePractice;
import org.mcwonderland.uhc.scenario.ScenarioManager;
import org.mcwonderland.uhc.settings.spawn.Spawns;
import org.mcwonderland.uhc.stats.storages.StatsStorage;

public class WonderlandUHC extends JavaPlugin {
    private static WonderlandUHC instance;

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
        if (instance == null)
            instance = JavaPlugin.getPlugin(WonderlandUHC.class);

        return instance;
    }

    public WonderlandUHC() {
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        startReloadableRuntime();
        startPluginRuntime();
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        instance = null;
    }

    public void reload() {
        getServer().getScheduler().cancelTasks(this);
        reloadRuntimeState();
        startReloadableRuntime();
    }

    private void startPluginRuntime() {
        PluginBootstrap bootstrap = new PluginBootstrap(this);
        FeatureRegistry featureRegistry = new FeatureRegistry(this);
        featureRegistry.registerListeners(this::registerEvents);
        featureRegistry.registerNativeCommands();
        bootstrap.createPluginAssets();
        featureRegistry.setupPractice(practice);
        featureRegistry.setupDiscordVoiceHook();

        bootstrap.scheduleDelayedStartupTasks(featureRegistry);

    }

    private void reloadRuntimeState() {
        scenarioManager.reloadAll();
        UHCGameSettingsSaver.reloadFromFile();
    }

    private void startReloadableRuntime() {
        PluginBootstrap bootstrap = new PluginBootstrap(this);
        FeatureRegistry featureRegistry = new FeatureRegistry(this);

        bootstrap.loadFiles();
        bootstrap.loadStaticConfiguration();
        bootstrap.checkDependencies();

        featureRegistry.loadScoreboardThemes();
        Spawns.reload();

        statsStorage = bootstrap.loadStatsStorage();

        if (bootstrap.isWorldLoadingDone()) {
            featureRegistry.startScoreboardUpdater();
            bootstrap.startRuntimeTasks();
        }

        bootstrap.applyTestModeSettings();
    }

    private void registerEvents(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

}
