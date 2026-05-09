package org.mcwonderland.uhc.application.world;

public enum CenterSearchStatus {
    RECOMMENDED,
    ACCEPTABLE,
    POOR,
    REJECTED,
    TIME_LIMITED,
    CANCELLED;

    public boolean shouldPregenerate() {
        return this == RECOMMENDED || this == ACCEPTABLE;
    }
}
