package org.mcwonderland.uhc.application.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.sub.UHCBorderSettings;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
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
                LegacyFoundationAdapter.runLater(0, () -> finish(worldName, totalChunks))
        );
    }

    public void start(World world) {
        String worldName = world.getName();
        LegacyFoundationAdapter.log(Messages.Console.CHUNK_LOAD_STARTED.replace("{world}", worldName));
        pregeneration.startSquarePregeneration(
                worldName,
                pregenerationRadius(world),
                Settings.ChunkLoading.FREQUENCY,
                Settings.ChunkLoading.PADDING
        );
    }

    public void finish(World world, long totalChunks) {
        if (!WorldUtils.isUHCWorld(world))
            return;

        LegacyFoundationAdapter.logNoPrefix(Messages.Console.CHUNK_LOAD_FINISHED
                .replace("{world}", world.getName())
                .replace("{number}", "" + totalChunks)
        );

        UHCBorderSettings borderSettings = Game.getSettings().getBorderSettings();

        if (world == UHCWorldUtils.getWorld())
            buildBorders(world, borderSettings.getInitialBorder());
        else if (world == UHCWorldUtils.getNether())
            buildBorders(world, borderSettings.getInitialNetherBorder());
    }

    private void finish(String worldName, long totalChunks) {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            LegacyFoundationAdapter.log("&cChunk pregeneration finished for unloaded world: " + worldName);
            return;
        }

        finish(world, totalChunks);
    }

    private int pregenerationRadius(World world) {
        UHCBorderSettings borderSettings = Game.getSettings().getBorderSettings();
        if (world == UHCWorldUtils.getNether())
            return BorderUtil.getRadius(borderSettings.getInitialNetherBorder()) + 1;

        return BorderUtil.getRadius(borderSettings.getInitialBorder()) + 1;
    }

    private void buildBorders(World world, int size) {
        LegacyFoundationAdapter.logNoPrefix("&eGenerating Border...");

        generateBorder(world, size);

        LegacyFoundationAdapter.runLater(20 * 3, () -> {
            for (int i = 0; i < Settings.Border.BEDROCK_BORDER_HEIGHT - 1; i++)
                generateBorder(world, size);

            LegacyFoundationAdapter.logNoPrefix("&eBorder Generated!");
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
            LegacyFoundationAdapter.logNoPrefix(Messages.Console.CHUNK_LOAD_NETHER_DETECTED);
            start(UHCWorldUtils.getNether());
        } else
            checkForceChunk();
    }

    private void checkForceChunk() {
        if (Settings.ChunkLoading.FORCE_LOADING_NETHER_CHUNK) {
            LegacyFoundationAdapter.logNoPrefix(Messages.Console.FORCE_NETHER_CHUNK_ON);
            start(UHCWorldUtils.getNether());
        } else
            saveAndRestart();
    }

    private void saveAndRestart() {
        CacheSaver.setLoadingStatus(LoadingStatus.DONE);
        CacheSaver.saveCache();
        Extra.restartServer();
    }
}
