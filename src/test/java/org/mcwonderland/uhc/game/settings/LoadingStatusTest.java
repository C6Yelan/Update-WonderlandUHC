package org.mcwonderland.uhc.game.settings;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoadingStatusTest {

    @Test
    public void configuringDoesNotKeepGeneratedWorlds() {
        assertFalse(LoadingStatus.CONFIGURING.shouldKeepGeneratedWorlds());
    }

    @Test
    public void worldReadyKeepsGeneratedWorldWithoutResumingPregeneration() {
        assertTrue(LoadingStatus.WORLD_READY.shouldKeepGeneratedWorlds());
        assertFalse(LoadingStatus.WORLD_READY.shouldResumePregeneration());
        assertTrue(LoadingStatus.WORLD_READY.isWaitingForHost());
    }

    @Test
    public void generatingKeepsWorldAndResumesPregeneration() {
        assertTrue(LoadingStatus.GENERATING.shouldKeepGeneratedWorlds());
        assertTrue(LoadingStatus.GENERATING.shouldResumePregeneration());
        assertFalse(LoadingStatus.GENERATING.isWaitingForHost());
    }

    @Test
    public void doneKeepsWorldWithoutWaitingForHost() {
        assertTrue(LoadingStatus.DONE.shouldKeepGeneratedWorlds());
        assertFalse(LoadingStatus.DONE.shouldResumePregeneration());
        assertFalse(LoadingStatus.DONE.isWaitingForHost());
    }
}
