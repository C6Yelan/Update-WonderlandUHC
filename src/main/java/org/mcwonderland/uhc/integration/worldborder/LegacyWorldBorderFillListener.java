package org.mcwonderland.uhc.integration.worldborder;

import com.wimbli.WorldBorder.Events.WorldBorderFillFinishedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mcwonderland.uhc.application.world.ChunkPregenerationService;

public final class LegacyWorldBorderFillListener implements Listener {

    private final ChunkPregenerationService pregenerationService;

    public LegacyWorldBorderFillListener(ChunkPregenerationService pregenerationService) {
        this.pregenerationService = pregenerationService;
    }

    @EventHandler
    public void worldFinish(WorldBorderFillFinishedEvent event) {
        pregenerationService.finish(event.getWorld(), event.getTotalChunks());
    }
}
