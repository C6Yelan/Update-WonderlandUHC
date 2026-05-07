package org.mcwonderland.uhc.integration.worldborder;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.port.ChunkPregenerationPort;

import java.io.File;
import java.io.IOException;

public final class LegacyWorldBorderPregenerationAdapter implements ChunkPregenerationPort {

    @Override
    public void startSquarePregeneration(String worldName, int radius, int frequency, int padding) {
        World world = Bukkit.getWorld(worldName);

        if (world == null)
            throw new IllegalArgumentException("World is not loaded: " + worldName);

        makeFirstRegionFileIfEmpty(world);

        String[] commands = {
                "wb " + worldName + " set " + radius + " " + radius + " 0 0",
                "wb wshape " + worldName + " square",
                "wb shape square",
                "wb " + worldName + " fill " + frequency + " " + padding + " false",
                "wb " + worldName + " fill confirm"
        };

        for (int i = 0; i < commands.length; i++) {
            String command = commands[i];

            LegacyFoundationAdapter.runLater(10 * i, () -> {
                LegacyFoundationAdapter.dispatchCommand(null, command);
            });
        }
    }

    private void makeFirstRegionFileIfEmpty(World world) {
        File regionFolder = new File(world.getWorldFolder(), "region");

        if (!regionFolder.exists())
            regionFolder.mkdir();

        if (regionFolder.listFiles().length == 0) {
            File mcaFile = new File(regionFolder, "r.0.0.mca");
            create(mcaFile);
        }
    }

    private void create(File mcaFile) {
        try {
            mcaFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
