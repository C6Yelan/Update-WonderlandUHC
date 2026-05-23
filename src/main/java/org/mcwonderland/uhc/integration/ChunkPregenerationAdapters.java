package org.mcwonderland.uhc.integration;

import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.integration.chunky.ChunkyPregenerationAdapter;
import org.mcwonderland.uhc.port.ChunkPregenerationPort;

public final class ChunkPregenerationAdapters {

    private ChunkPregenerationAdapters() {
    }

    public static ChunkPregenerationPort create() {
        if (Dependency.CHUNKY.isHooked())
            return new ChunkyPregenerationAdapter();

        return new MissingChunkPregenerationAdapter();
    }

    private static final class MissingChunkPregenerationAdapter implements ChunkPregenerationPort {

        @Override
        public void startSquarePregeneration(String worldName, MatchCenter center, int radius) {
            throw new IllegalStateException("Chunk pregeneration requires Chunky. Install Chunky and restart the server.");
        }
    }
}
