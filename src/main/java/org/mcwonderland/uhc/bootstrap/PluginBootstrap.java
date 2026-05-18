package org.mcwonderland.uhc.bootstrap;

import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameTimerRunnable;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettingsSaver;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.menu.ButtonLocalization;
import org.mcwonderland.uhc.model.InvinciblePlayer;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.platform.paper.PaperPluginAssetPort;
import org.mcwonderland.uhc.platform.paper.PaperSchedulerPort;
import org.mcwonderland.uhc.platform.paper.PaperWorldPort;
import org.mcwonderland.uhc.port.PluginAssetPort;
import org.mcwonderland.uhc.port.SchedulerPort;
import org.mcwonderland.uhc.port.WorldPort;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.stats.storages.StatsStorage;
import org.mcwonderland.uhc.stats.storages.StatsStorageYaml;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.ChunkFiller;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.UHCWorldUtils;

public final class PluginBootstrap {

    private final WonderlandUHC plugin;
    private final PluginAssetPort pluginAssets;
    private final SchedulerPort scheduler;
    private final WorldPort worlds;

    public PluginBootstrap(WonderlandUHC plugin) {
        this(plugin, new PaperPluginAssetPort(), new PaperSchedulerPort(plugin), new PaperWorldPort());
    }

    PluginBootstrap(WonderlandUHC plugin, PluginAssetPort pluginAssets, SchedulerPort scheduler, WorldPort worlds) {
        this.plugin = plugin;
        this.pluginAssets = pluginAssets;
        this.scheduler = scheduler;
        this.worlds = worlds;
    }

    public void loadFiles() {
        UHCFiles.getFileNames().forEach(this::saveResourceIfMissing);
    }

    private void saveResourceIfMissing(String path) {
        if (new java.io.File(plugin.getDataFolder(), path).exists())
            return;

        plugin.saveResource(path, false);
    }

    public DependencyReport checkDependencies() {
        DependencyReport report = new DependencyReport();

        checkOptionalDependency(report, Dependency.CHUNKY);

        logDependencyReport(report);

        return report;
    }

    private void logDependencyReport(DependencyReport report) {
        PluginConsole.log("&7Dependency status:");

        for (DependencyReport.Entry entry : report.getEntries()) {
            String status = entry.isAvailable() ? "&aAvailable" : entry.isDisabled() ? "&eDisabled" : "&cUnavailable";
            String reason = entry.getReason().isEmpty() ? "" : " &7(" + entry.getReason() + ")";

            PluginConsole.log("&7- &f" + entry.getDependency().getPluginName() + "&7: " + status + reason);
        }
    }

    private void checkOptionalDependency(DependencyReport report, Dependency dependency) {
        if (dependency.isHooked())
            report.markAvailable(dependency);
        else
            report.markDisabled(dependency, "Plugin is not hooked.");
    }

    public StatsStorage loadStatsStorage() {
        return new StatsStorageYaml();
    }

    public void configureFoundationLibrary() {
        ButtonLocalization.load();
        LegacyFoundationAdapter.configureMenuClickSound();
    }

    public void createPluginAssets() {
        pluginAssets.registerRecipes();
    }

    public void scheduleDelayedStartupTasks(FeatureRegistry featureRegistry) {
        scheduler.runLater(1, () -> {
            featureRegistry.registerDefaultScenarios();
            restoreWorldLoadingStatus();
            UHCGameSettingsSaver.reloadFromFile();
            logPluginEnabledMessage();
        });
    }

    public void startRuntimeTasks() {
        GameTimerRunnable.start();
        InvinciblePlayer.startTask();
    }

    public boolean isWorldLoadingDone() {
        return CacheSaver.getLoadingStatus() == LoadingStatus.DONE;
    }

    public void applyTestModeSettings() {
        if (!WonderlandUHC.TEST_MODE)
            return;

        Settings.Game.TIME_TO_START_AFTER_TELEPORT = 10;
        Settings.Game.PRE_START_TIME = 10;
    }

    public void restoreWorldLoadingStatus() {
        LoadingStatus loadingStatus = CacheSaver.getLoadingStatus();
        Game.getGame().setHost(CacheSaver.getHost());

        if (!loadingStatus.shouldKeepGeneratedWorlds()) {
            BorderUtil.removeUHCWorldWBBorders();
            Extra.deleteWorld(UHCWorldUtils.getWorldName());
            Extra.deleteWorld(UHCWorldUtils.getNetherName());
        } else {
            Game.changeSettings(CacheSaver.getSettings());
            Game.getGame().setMatchCenter(CacheSaver.getMatchCenter());
            createUhcWorldIfNotExist();
            checkNetherWorld();
            BorderUtil.setInitialBorders();

            if (loadingStatus.shouldResumePregeneration())
                ChunkFiller.fill(UHCWorldUtils.getWorld());
        }
    }

    private void createUhcWorldIfNotExist() {
        if (!worlds.worldExists(Settings.Game.UHC_WORLD_NAME)) {
            worlds.createWorld(Settings.Game.UHC_WORLD_NAME);
        }
    }

    private void checkNetherWorld() {
        if (!worlds.worldExists(UHCWorldUtils.getNetherName())) {
            worlds.createNetherWorld(Settings.Game.UHC_WORLD_NAME + "_nether");
        }
    }

    public void logPluginEnabledMessage() {
        PluginConsole.logNoPrefix(
                PluginConsole.consoleLineSmooth(),
                " _    _                 _           _                 _   _   _ _   _ _____",
                "| |  | |               | |         | |               | | | | | | | | /  __ \\",
                "| |  | | ___  _ __   __| | ___ _ __| | __ _ _ __   __| | | | | | |_| | /  \\/",
                "| |/\\| |/ _ \\| '_ \\ / _` |/ _ \\ '__| |/ _` | '_ \\ / _` | | | | |  _  | |    ",
                "\\  /\\  / (_) | | | | (_| |  __/ |  | | (_| | | | | (_| | | |_| | | | | \\__/\\",
                " \\/  \\/ \\___/|_| |_|\\__,_|\\___|_|  |_|\\__,_|_| |_|\\__,_|  \\___/\\_| |_/\\____/",
                "                                                  ",
                "&3Author: &fLU__LU",
                "&3Version: &f" + plugin.getPluginMeta().getVersion(),
                "",
                "&3Enjoy your own UHC time!",
                PluginConsole.consoleLineSmooth());
    }
}
