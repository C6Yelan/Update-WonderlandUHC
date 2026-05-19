package org.mcwonderland.uhc.application.world;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.sub.UHCBorderSettings;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.port.ChunkPregenerationPort;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.mcwonderland.uhc.util.WorldUtils;

public final class ChunkPregenerationService {

    private final ChunkPregenerationPort pregeneration;

    public ChunkPregenerationService(ChunkPregenerationPort pregeneration) {
        this.pregeneration = pregeneration;
        this.pregeneration.onComplete((worldName, totalChunks) ->
                PluginScheduler.runLater(0, () -> finish(worldName, totalChunks))
        );
    }

    public void start(World world) {
        String worldName = world.getName();
        int radius = pregenerationRadius(world);
        int borderSize = pregenerationBorderSize(world);
        PluginConsole.log(PluginText.replaceToString(
                Messages.Console.CHUNK_LOAD_STARTED,
                "{world}", worldName
        ));
        pregeneration.startSquarePregeneration(
                worldName,
                UHCWorldUtils.getBorderCenter(world, borderSize),
                radius,
                Settings.ChunkLoading.FREQUENCY,
                Settings.ChunkLoading.PADDING
        );
    }

    public void finish(World world, long totalChunks) {
        if (!WorldUtils.isUHCWorld(world))
            return;

        PluginConsole.logNoPrefix(PluginText.replaceToString(
                Messages.Console.CHUNK_LOAD_FINISHED,
                "{world}", world.getName(),
                "{number}", totalChunks
        ));

        UHCBorderSettings borderSettings = Game.getSettings().getBorderSettings();

        if (world == UHCWorldUtils.getWorld())
            buildBorders(world, borderSettings.getInitialBorder());
        else if (world == UHCWorldUtils.getNether())
            buildBorders(world, borderSettings.getInitialNetherBorder());
    }

    private void finish(String worldName, long totalChunks) {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            PluginConsole.log("<red>Chunk pregeneration finished for unloaded world: " + worldName + "</red>");
            return;
        }

        finish(world, totalChunks);
    }

    private int pregenerationRadius(World world) {
        return BorderUtil.getRadius(pregenerationBorderSize(world)) + 1;
    }

    private int pregenerationBorderSize(World world) {
        UHCBorderSettings borderSettings = Game.getSettings().getBorderSettings();
        if (world == UHCWorldUtils.getNether())
            return borderSettings.getInitialNetherBorder();

        return borderSettings.getInitialBorder();
    }

    private void buildBorders(World world, int size) {
        PluginConsole.logNoPrefix("<yellow>Generating Border...</yellow>");

        generateBorder(world, size);

        PluginScheduler.runLater(20 * 3, () -> {
            for (int i = 0; i < Settings.Border.BEDROCK_BORDER_HEIGHT - 1; i++)
                generateBorder(world, size);

            PluginConsole.logNoPrefix("<yellow>Border Generated!</yellow>");
            onBorderGenerated(world);
        });
    }

    private void generateBorder(World world, int size) {
        if (Settings.Border.BEDROCK_BORDER_HEIGHT > 0)
            BorderUtil.generateBorder(world, size);
    }

    private void onBorderGenerated(World world) {
        if (world == UHCWorldUtils.getWorld())
            checkNether();
        else if (world == UHCWorldUtils.getNether())
            saveAndRestart();
    }

    private void checkNether() {
        if (Game.getSettings().isUsingNether()) {
            PluginConsole.logNoPrefix(Messages.Console.CHUNK_LOAD_NETHER_DETECTED);
            start(UHCWorldUtils.getNether());
        } else
            checkForceChunk();
    }

    private void checkForceChunk() {
        if (Settings.ChunkLoading.FORCE_LOADING_NETHER_CHUNK) {
            PluginConsole.logNoPrefix(Messages.Console.FORCE_NETHER_CHUNK_ON);
            start(UHCWorldUtils.getNether());
        } else
            saveAndRestart();
    }

    private void saveAndRestart() {
        WorldLoadingCacheState.setLoadingStatus(LoadingStatus.DONE);
        WorldLoadingCacheState.saveCache();
        Extra.restartServer();
    }
}
