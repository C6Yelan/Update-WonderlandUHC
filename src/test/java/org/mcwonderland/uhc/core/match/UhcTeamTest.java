package org.mcwonderland.uhc.core.match;

import org.junit.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UhcTeamTest {

    @Test
    public void ownerStartsAsMember() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UhcTeam team = new UhcTeam(id, "Red", ownerId);

        assertSame(id, team.getId());
        assertEquals("Red", team.getName());
        assertSame(ownerId, team.getOwnerId());
        assertTrue(team.contains(ownerId));
        assertEquals(1, team.getMemberIds().size());
        assertFalse(team.isEmpty());
    }

    @Test
    public void membersCanBeAddedAndRemoved() {
        UhcTeam team = new UhcTeam(UUID.randomUUID(), "Red", UUID.randomUUID());
        UUID memberId = UUID.randomUUID();

        assertTrue(team.addMember(memberId));
        assertFalse(team.addMember(memberId));
        assertTrue(team.contains(memberId));

        assertTrue(team.removeMember(memberId));
        assertFalse(team.removeMember(memberId));
        assertFalse(team.contains(memberId));
    }

    @Test
    public void memberIdsAreReadOnlyFromOutside() {
        UhcTeam team = new UhcTeam(UUID.randomUUID(), "Red", UUID.randomUUID());
        Set<UUID> memberIds = team.getMemberIds();

        try {
            memberIds.add(UUID.randomUUID());
        } catch (UnsupportedOperationException expected) {
            return;
        }

        throw new AssertionError("memberIds should be unmodifiable.");
    }

    @Test
    public void ownerCanBePromotedToExistingMember() {
        UUID ownerId = UUID.randomUUID();
        UUID newOwnerId = UUID.randomUUID();
        UhcTeam team = new UhcTeam(UUID.randomUUID(), "Red", ownerId);

        team.addMember(newOwnerId);
        team.promote(newOwnerId);

        assertSame(newOwnerId, team.getOwnerId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotPromoteMissingMember() {
        UhcTeam team = new UhcTeam(UUID.randomUUID(), "Red", UUID.randomUUID());

        team.promote(UUID.randomUUID());
    }

    @Test
    public void removingOwnerPromotesNextMemberOrClearsOwnerWhenEmpty() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UhcTeam team = new UhcTeam(UUID.randomUUID(), "Red", ownerId);

        team.addMember(memberId);
        team.removeMember(ownerId);

        assertSame(memberId, team.getOwnerId());
        assertTrue(team.contains(memberId));

        team.removeMember(memberId);

        assertEquals(null, team.getOwnerId());
        assertTrue(team.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void idCannotBeNull() {
        new UhcTeam(null, "Red", UUID.randomUUID());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameCannotBeBlank() {
        new UhcTeam(UUID.randomUUID(), " ", UUID.randomUUID());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ownerIdCannotBeNull() {
        new UhcTeam(UUID.randomUUID(), "Red", null);
    }
}
