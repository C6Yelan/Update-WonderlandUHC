package org.mcwonderland.uhc.application.border;

import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.port.WorldBorderPort;

public final class BorderService {

    private static final double BORDER_SIZE_OFFSET = 2D;
    private static final double IMMEDIATE_DAMAGE_BUFFER = 0D;
    private static final int TICKS_PER_SECOND = 20;

    private final WorldBorderPort worldBorders;

    public BorderService(WorldBorderPort worldBorders) {
        this.worldBorders = worldBorders;
    }

    public void reset(String worldName) {
        worldBorders.reset(worldName);
    }

    public void setWarning(String worldName, int blocks, int seconds) {
        worldBorders.setWarningDistance(worldName, blocks);
        worldBorders.setWarningTimeTicks(worldName, seconds * TICKS_PER_SECOND);
    }

    public void setFixedBorder(String worldName, int size, MatchCenter center) {
        setExactFixedBorder(worldName, size + BORDER_SIZE_OFFSET, center);
    }

    public void setExactFixedBorderAtOrigin(String worldName, double size) {
        worldBorders.setCenter(worldName, 0, 0);
        worldBorders.setSize(worldName, size);
        worldBorders.setDamageBuffer(worldName, IMMEDIATE_DAMAGE_BUFFER);
    }

    public void setExactFixedBorder(String worldName, double size, MatchCenter center) {
        worldBorders.setCenter(worldName, center.getX(), center.getZ());
        worldBorders.setSize(worldName, size);
        worldBorders.setDamageBuffer(worldName, IMMEDIATE_DAMAGE_BUFFER);
    }

    public void shrinkBorder(String worldName, int finalSize, long seconds, MatchCenter center) {
        worldBorders.setCenter(worldName, center.getX(), center.getZ());
        worldBorders.changeSize(worldName, finalSize, seconds * TICKS_PER_SECOND);
        worldBorders.setDamageBuffer(worldName, IMMEDIATE_DAMAGE_BUFFER);
    }

    public void setWarningDistance(String worldName, int blocks) {
        worldBorders.setWarningDistance(worldName, blocks);
    }

    public int getSize(String worldName) {
        return ( int ) worldBorders.getSize(worldName);
    }
}
