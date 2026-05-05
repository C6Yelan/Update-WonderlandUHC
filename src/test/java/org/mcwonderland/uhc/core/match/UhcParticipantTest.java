package org.mcwonderland.uhc.core.match;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UhcParticipantTest {

    @Test
    public void participantStartsAliveWithNoKills() {
        UUID uniqueId = UUID.randomUUID();
        UhcParticipant participant = new UhcParticipant(uniqueId, "Alice");

        assertSame(uniqueId, participant.getUniqueId());
        assertEquals("Alice", participant.getName());
        assertEquals(null, participant.getTeamId());
        assertTrue(participant.isAlive());
        assertFalse(participant.isDead());
        assertEquals(0, participant.getKills());
    }

    @Test
    public void participantCanBeMarkedDeadAndAliveAgain() {
        UhcParticipant participant = new UhcParticipant(UUID.randomUUID(), "Alice");

        participant.markDead();

        assertFalse(participant.isAlive());
        assertTrue(participant.isDead());

        participant.markAlive();

        assertTrue(participant.isAlive());
        assertFalse(participant.isDead());
    }

    @Test
    public void killsCanBeIncremented() {
        UhcParticipant participant = new UhcParticipant(UUID.randomUUID(), "Alice");

        participant.addKill();
        participant.addKill();

        assertEquals(2, participant.getKills());
    }

    @Test(expected = IllegalArgumentException.class)
    public void uniqueIdCannotBeNull() {
        new UhcParticipant(null, "Alice");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameCannotBeBlank() {
        new UhcParticipant(UUID.randomUUID(), " ");
    }
}
