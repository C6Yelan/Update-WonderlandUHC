package org.mcwonderland.uhc.game.settings;

public enum LoadingStatus {
    CONFIGURING,
    WORLD_READY,
    GENERATING,
    DONE;

    public boolean shouldKeepGeneratedWorlds() {
        return this != CONFIGURING;
    }

    public boolean shouldResumePregeneration() {
        return this == GENERATING;
    }

    public boolean isWaitingForHost() {
        return this == CONFIGURING || this == WORLD_READY;
    }
}
