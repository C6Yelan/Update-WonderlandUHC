package org.mcwonderland.uhc.integration.chunky;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.port.ChunkPregenerationPort;
import org.popcraft.chunky.api.ChunkyAPI;
import org.popcraft.chunky.api.event.task.GenerationCompleteEvent;
import org.popcraft.chunky.api.event.task.GenerationProgressEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ChunkyPregenerationAdapter implements ChunkPregenerationPort {
    private static final String SHAPE = "square";
    private static final String PATTERN = "region";

    private final ChunkyAPI chunky;
    private final Map<String, Long> lastChunkCounts = new ConcurrentHashMap<>();
    private volatile BiConsumer<String, Long> completionHandler = (worldName, totalChunks) -> {
    };

    public ChunkyPregenerationAdapter() {
        this(resolveChunkyApi());
    }

    ChunkyPregenerationAdapter(ChunkyAPI chunky) {
        this.chunky = chunky;
        this.chunky.onGenerationProgress(this::recordProgress);
        this.chunky.onGenerationComplete(this::handleComplete);
    }

    @Override
    public void startSquarePregeneration(String worldName, MatchCenter center, int radius, int frequency, int padding) {
        World world = Bukkit.getWorld(worldName);

        if (world == null)
            throw new IllegalArgumentException("World is not loaded: " + worldName);

        boolean started = chunky.startTask(
                worldName,
                SHAPE,
                center.getX(),
                center.getZ(),
                radius,
                radius,
                PATTERN
        );

        if (!started)
            throw new IllegalStateException("Chunky did not start pregeneration for world: " + worldName);

        LegacyFoundationAdapter.log("&7Chunky pregeneration started for world: &f" + worldName);
    }

    @Override
    public void onComplete(BiConsumer<String, Long> handler) {
        this.completionHandler = handler;
    }

    private void recordProgress(GenerationProgressEvent event) {
        lastChunkCounts.put(event.world(), event.chunks());
    }

    private void handleComplete(GenerationCompleteEvent event) {
        completionHandler.accept(event.world(), lastChunkCounts.getOrDefault(event.world(), 0L));
    }

    private static ChunkyAPI resolveChunkyApi() {
        RegisteredServiceProvider<ChunkyAPI> registration = Bukkit.getServicesManager().getRegistration(ChunkyAPI.class);

        if (registration == null)
            throw new IllegalStateException("Chunky is not installed or did not register ChunkyAPI.");

        return registration.getProvider();
    }
}
