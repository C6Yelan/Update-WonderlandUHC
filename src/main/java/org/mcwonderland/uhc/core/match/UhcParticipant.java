package org.mcwonderland.uhc.core.match;

import java.util.UUID;

public final class UhcParticipant {
    private final UUID uniqueId;
    private final String name;
    private UUID teamId;
    private boolean alive = true;
    private int kills;

    public UhcParticipant(UUID uniqueId, String name) {
        if (uniqueId == null)
            throw new IllegalArgumentException("uniqueId cannot be null.");

        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("name cannot be blank.");

        this.uniqueId = uniqueId;
        this.name = name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isDead() {
        return !alive;
    }

    public int getKills() {
        return kills;
    }

    public void markDead() {
        this.alive = false;
    }

    public void markAlive() {
        this.alive = true;
    }

    public void addKill() {
        this.kills++;
    }

    void assignTeam(UUID teamId) {
        this.teamId = teamId;
    }

    void clearTeam() {
        this.teamId = null;
    }
}
