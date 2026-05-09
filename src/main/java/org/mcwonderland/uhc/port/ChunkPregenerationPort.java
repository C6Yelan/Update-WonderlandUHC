package org.mcwonderland.uhc.port;

import org.mcwonderland.uhc.application.world.MatchCenter;

import java.util.function.BiConsumer;

public interface ChunkPregenerationPort {

    void startSquarePregeneration(String worldName, MatchCenter center, int radius, int frequency, int padding);

    default void onComplete(BiConsumer<String, Long> handler) {
    }
}
