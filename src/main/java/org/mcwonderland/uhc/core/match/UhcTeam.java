package org.mcwonderland.uhc.core.match;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class UhcTeam {
    private final UUID id;
    private final String name;
    private UUID ownerId;
    private final Set<UUID> memberIds = new LinkedHashSet<>();

    public UhcTeam(UUID id, String name, UUID ownerId) {
        if (id == null)
            throw new IllegalArgumentException("id cannot be null.");

        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("name cannot be blank.");

        if (ownerId == null)
            throw new IllegalArgumentException("ownerId cannot be null.");

        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.memberIds.add(ownerId);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Set<UUID> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    public boolean addMember(UUID memberId) {
        if (memberId == null)
            throw new IllegalArgumentException("memberId cannot be null.");

        return memberIds.add(memberId);
    }

    public boolean removeMember(UUID memberId) {
        if (memberId == null)
            throw new IllegalArgumentException("memberId cannot be null.");

        boolean removed = memberIds.remove(memberId);

        if (removed && memberId.equals(ownerId))
            ownerId = memberIds.isEmpty() ? null : memberIds.iterator().next();

        return removed;
    }

    public boolean contains(UUID memberId) {
        return memberIds.contains(memberId);
    }

    public boolean isEmpty() {
        return memberIds.isEmpty();
    }

    public void promote(UUID newOwnerId) {
        if (newOwnerId == null)
            throw new IllegalArgumentException("newOwnerId cannot be null.");

        if (!memberIds.contains(newOwnerId))
            throw new IllegalArgumentException("New owner must be a team member.");

        this.ownerId = newOwnerId;
    }
}
