package org.mcwonderland.uhc.integration;

import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.integration.chunky.ChunkyPregenerationAdapter;
import org.mcwonderland.uhc.integration.worldborder.LegacyWorldBorderPregenerationAdapter;
import org.mcwonderland.uhc.port.ChunkPregenerationPort;

public final class ChunkPregenerationAdapters {

    private ChunkPregenerationAdapters() {
    }

    public static ChunkPregenerationPort create() {
        if (Dependency.CHUNKY.isHooked())
            return new ChunkyPregenerationAdapter();

        if (Dependency.WORLD_BORDER.isHooked())
            return new LegacyWorldBorderPregenerationAdapter();

        return new MissingChunkPregenerationAdapter();
    }

    public static boolean usesLegacyWorldBorder() {
        return !Dependency.CHUNKY.isHooked() && Dependency.WORLD_BORDER.isHooked();
    }

    private static final class MissingChunkPregenerationAdapter implements ChunkPregenerationPort {

        @Override
        public void startSquarePregeneration(String worldName, MatchCenter center, int radius, int frequency, int padding) {
            throw new IllegalStateException("Chunk pregeneration requires Chunky. Install Chunky or temporarily enable the legacy WorldBorder plugin.");
        }
    }
}
