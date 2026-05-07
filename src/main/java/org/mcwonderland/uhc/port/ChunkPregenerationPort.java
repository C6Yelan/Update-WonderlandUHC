package org.mcwonderland.uhc.port;

import java.util.function.BiConsumer;

public interface ChunkPregenerationPort {

    void startSquarePregeneration(String worldName, int radius, int frequency, int padding);

    default void onComplete(BiConsumer<String, Long> handler) {
    }
}
