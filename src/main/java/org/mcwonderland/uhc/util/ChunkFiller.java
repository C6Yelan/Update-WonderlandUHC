package org.mcwonderland.uhc.util;

import org.bukkit.World;
import org.mcwonderland.uhc.application.world.ChunkPregenerationService;
import org.mcwonderland.uhc.integration.ChunkPregenerationAdapters;

public class ChunkFiller {
    private static final ChunkPregenerationService PREGENERATION_SERVICE =
            new ChunkPregenerationService(ChunkPregenerationAdapters.create());

    public static void fill(World world) {
        PREGENERATION_SERVICE.start(world);
    }
}
