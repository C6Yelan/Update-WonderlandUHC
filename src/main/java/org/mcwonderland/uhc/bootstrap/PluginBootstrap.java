package org.mcwonderland.uhc.bootstrap;

import lombok.SneakyThrows;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameTimerRunnable;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettingsSaver;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.legacy.LegacyDatouNmsAdapter;
import org.mcwonderland.uhc.menu.ButtonLocalization;
import org.mcwonderland.uhc.model.InvinciblePlayer;
import org.mcwonderland.uhc.platform.paper.PaperPluginAssetPort;
import org.mcwonderland.uhc.platform.paper.PaperPluginMessagingPort;
import org.mcwonderland.uhc.platform.paper.PaperSchedulerPort;
import org.mcwonderland.uhc.platform.paper.PaperWorldPort;
import org.mcwonderland.uhc.port.PluginAssetPort;
import org.mcwonderland.uhc.port.PluginMessagingPort;
import org.mcwonderland.uhc.port.SchedulerPort;
import org.mcwonderland.uhc.port.WorldPort;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.stats.storages.StatsStorage;
import org.mcwonderland.uhc.stats.storages.StatsStorageSql;
import org.mcwonderland.uhc.stats.storages.StatsStorageYaml;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.ChunkFiller;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.mcwonderland.uhc.util.VersionComparator;

public final class PluginBootstrap {

    private final WonderlandUHC plugin;
    private final PluginAssetPort pluginAssets;
    private final PluginMessagingPort pluginMessaging;
    private final SchedulerPort scheduler;
    private final WorldPort worlds;

    public PluginBootstrap(WonderlandUHC plugin) {
        this(plugin, new PaperPluginAssetPort(), new PaperPluginMessagingPort(plugin), new PaperSchedulerPort(plugin), new PaperWorldPort());
    }

    PluginBootstrap(WonderlandUHC plugin, PluginAssetPort pluginAssets, PluginMessagingPort pluginMessaging, SchedulerPort scheduler, WorldPort worlds) {
        this.plugin = plugin;
        this.pluginAssets = pluginAssets;
        this.pluginMessaging = pluginMessaging;
        this.scheduler = scheduler;
        this.worlds = worlds;
    }

    public void loadFiles() {
        UHCFiles.getFileNames().forEach(LegacyFoundationAdapter::extractFile);
        LegacyFoundationAdapter.extractFile(UHCFiles.PERMISSIONS, UHCFiles.PERMISSIONS);
    }

    public void setupNms() {
        LegacyDatouNmsAdapter.initialize(plugin);
    }

    public DependencyReport checkDependencies() {
        DependencyReport report = new DependencyReport();

        checkRequiredDependency(report, Dependency.WORLD_BORDER);
        checkOptionalDependency(report, Dependency.PACKET_LISTENER_API);
        checkWorldBorderVer();

        if (Dependency.CUSTOM_ORE_GENERATOR.isHooked())
            checkCustomOreGenerator(report);
        else
            report.markDisabled(Dependency.CUSTOM_ORE_GENERATOR, "Plugin is not hooked.");

        logDependencyReport(report);

        return report;
    }

    private void logDependencyReport(DependencyReport report) {
        LegacyFoundationAdapter.log("&7Dependency status:");

        for (DependencyReport.Entry entry : report.getEntries()) {
            String status = entry.isAvailable() ? "&aAvailable" : entry.isDisabled() ? "&eDisabled" : "&cUnavailable";
            String reason = entry.getReason().isEmpty() ? "" : " &7(" + entry.getReason() + ")";

            LegacyFoundationAdapter.log("&7- &f" + entry.getDependency().getPluginName() + "&7: " + status + reason);
        }
    }

    private void checkRequiredDependency(DependencyReport report, Dependency dependency) {
        if (dependency.isHooked())
            report.markAvailable(dependency);
        else
            report.markUnavailable(dependency, "Required plugin is missing.");

        dependency.check();
    }

    private void checkOptionalDependency(DependencyReport report, Dependency dependency) {
        if (dependency.isHooked())
            report.markAvailable(dependency);
        else
            report.markDisabled(dependency, "Plugin is not hooked.");
    }

    private void checkCustomOreGenerator(DependencyReport report) {
        try {
            Class.forName("de.derfrzocker.custom.ore.generator.api.OreSettingContainer");
            report.markAvailable(Dependency.CUSTOM_ORE_GENERATOR);
        } catch (ClassNotFoundException e) {
            report.markUnavailable(Dependency.CUSTOM_ORE_GENERATOR, "Installed plugin version is too old.");
            throw LegacyFoundationAdapter.failure("&cCustomOreGenerator 版本過舊，請至 &f"
                    + Dependency.CUSTOM_ORE_GENERATOR.getDownloadUrl() +
                    " &c下載最新版本！");
        }
    }

    @SneakyThrows
    private void checkWorldBorderVer() {
        Dependency worldBorder = Dependency.WORLD_BORDER;
        boolean newer = LegacyFoundationAdapter.isAtLeastMinecraft1_13();
        boolean usingNewerWb = VersionComparator.isNewerThan(worldBorder.getVersion(), "1.8.7");

        if (newer && !usingNewerWb) {
            LegacyFoundationAdapter.logReplacing(Messages.Dependency.USING_OLD_WORLD_BORDER_IN_NEW_VERSION, "{link}", worldBorder.getDownloadUrl());
            Thread.sleep(3 * 1000);
            return;
        }

        if (!newer && usingNewerWb) {
            Thread.sleep(3 * 1000);
            throw LegacyFoundationAdapter.failure(Messages.Dependency.CHANGE_TO_OLDER_WORLD_BORDER_VERSION
                    .replace("{link}", worldBorder.getDownloadUrl()));
        }
    }

    public StatsStorage loadStatsStorage() {
        if (Settings.Mysql.USE)
            return new StatsStorageSql();

        return new StatsStorageYaml();
    }

    public void configureFoundationLibrary() {
        ButtonLocalization.load();
        LegacyFoundationAdapter.configureMenuClickSound();
    }

    public void registerPluginChannels() {
        pluginMessaging.registerOutgoingChannel("BungeeCord");
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

        if (loadingStatus == LoadingStatus.CONFIGURING) {
            BorderUtil.removeUHCWorldWBBorders();
            Extra.deleteWorld(UHCWorldUtils.getWorldName());
            Extra.deleteWorld(UHCWorldUtils.getNetherName());
        } else {
            Game.changeSettings(CacheSaver.getSettings());
            createUhcWorldIfNotExist();
            checkNetherWorld();
            BorderUtil.setInitialBorders();

            if (loadingStatus == LoadingStatus.GENERATING)
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
        LegacyFoundationAdapter.logNoPrefix(
                LegacyFoundationAdapter.consoleLineSmooth(),
                " _    _                 _           _                 _   _   _ _   _ _____",
                "| |  | |               | |         | |               | | | | | | | | /  __ \\",
                "| |  | | ___  _ __   __| | ___ _ __| | __ _ _ __   __| | | | | | |_| | /  \\/",
                "| |/\\| |/ _ \\| '_ \\ / _` |/ _ \\ '__| |/ _` | '_ \\ / _` | | | | |  _  | |    ",
                "\\  /\\  / (_) | | | | (_| |  __/ |  | | (_| | | | | (_| | | |_| | | | | \\__/\\",
                " \\/  \\/ \\___/|_| |_|\\__,_|\\___|_|  |_|\\__,_|_| |_|\\__,_|  \\___/\\_| |_/\\____/",
                "                                                  ",
                "&3Author: &fLU__LU",
                "&3Version: &f" + plugin.getDescription().getVersion(),
                "",
                "&3Enjoy your own UHC time!",
                LegacyFoundationAdapter.consoleLineSmooth());
    }
}
