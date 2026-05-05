package org.mcwonderland.uhc.core.match;

import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class UhcMatchTest {

    @Test
    public void matchStartsWaiting() {
        UhcMatch match = UhcMatch.create();

        assertSame(MatchState.WAITING, match.getState());
    }

    @Test
    public void matchUsesDefaultSettings() {
        UhcMatch match = UhcMatch.create();

        assertEquals(MatchSettings.defaults().getTitle(), match.getSettings().getTitle());
        assertEquals(MatchSettings.defaults().getMaxPlayers(), match.getSettings().getMaxPlayers());
        assertEquals(MatchSettings.defaults().getTeamSize(), match.getSettings().getTeamSize());
    }

    @Test
    public void matchCanStartWithCustomSettings() {
        MatchSettings settings = new MatchSettings("Custom UHC", 60, 3, true, true, "default", Collections.singleton("cutclean"));

        UhcMatch match = UhcMatch.create(settings);

        assertSame(settings, match.getSettings());
    }

    @Test
    public void matchSettingsCanBeUpdated() {
        UhcMatch match = UhcMatch.create();
        MatchSettings settings = new MatchSettings("Updated UHC", 40, 2, false, true, "", Collections.<String>emptySet());

        match.updateSettings(settings);

        assertSame(settings, match.getSettings());
    }

    @Test(expected = IllegalArgumentException.class)
    public void matchCannotStartWithNullSettings() {
        UhcMatch.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void matchSettingsCannotBeUpdatedToNull() {
        UhcMatch match = UhcMatch.create();

        match.updateSettings(null);
    }

    @Test
    public void matchAdvancesThroughKnownLifecycleStates() {
        UhcMatch match = UhcMatch.create();

        assertSame(MatchState.TELEPORTING, match.advanceState());
        assertSame(MatchState.TELEPORTING, match.getState());

        assertSame(MatchState.PRE_START, match.advanceState());
        assertSame(MatchState.PRE_START, match.getState());

        assertSame(MatchState.PLAYING, match.advanceState());
        assertSame(MatchState.PLAYING, match.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void playingCannotAdvancePastKnownLifecycle() {
        UhcMatch match = UhcMatch.create();

        match.advanceState();
        match.advanceState();
        match.advanceState();
        match.advanceState();
    }

    @Test
    public void participantsCanBeAddedAndFoundByUniqueId() {
        UhcMatch match = UhcMatch.create();
        UUID uniqueId = UUID.randomUUID();

        UhcParticipant participant = match.addParticipant(uniqueId, "Alice");

        assertSame(participant, match.getParticipant(uniqueId));
        assertEquals(1, match.getParticipants().size());
    }

    @Test
    public void duplicateParticipantReturnsExistingParticipant() {
        UhcMatch match = UhcMatch.create();
        UUID uniqueId = UUID.randomUUID();

        UhcParticipant first = match.addParticipant(uniqueId, "Alice");
        UhcParticipant second = match.addParticipant(uniqueId, "Bob");

        assertSame(first, second);
        assertEquals("Alice", second.getName());
        assertEquals(1, match.getParticipants().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void participantsAreReadOnlyFromOutside() {
        UhcMatch match = UhcMatch.create();
        match.addParticipant(UUID.randomUUID(), "Alice");

        match.getParticipants().clear();
    }

    @Test
    public void teamsCanBeCreatedAndFoundByUniqueId() {
        UhcMatch match = UhcMatch.create();
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        UhcTeam team = match.createTeam(teamId, "Red", ownerId);

        assertSame(team, match.getTeam(teamId));
        assertEquals(1, match.getTeams().size());
    }

    @Test
    public void duplicateTeamReturnsExistingTeam() {
        UhcMatch match = UhcMatch.create();
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        UhcTeam first = match.createTeam(teamId, "Red", ownerId);
        UhcTeam second = match.createTeam(teamId, "Blue", UUID.randomUUID());

        assertSame(first, second);
        assertEquals("Red", second.getName());
        assertSame(ownerId, second.getOwnerId());
        assertEquals(1, match.getTeams().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void teamsAreReadOnlyFromOutside() {
        UhcMatch match = UhcMatch.create();
        match.createTeam(UUID.randomUUID(), "Red", UUID.randomUUID());

        match.getTeams().clear();
    }

    @Test
    public void participantCanJoinTeam() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        UhcParticipant participant = match.addParticipant(participantId, "Alice");
        UhcTeam team = match.createTeam(teamId, "Red", UUID.randomUUID());

        match.joinTeam(teamId, participantId);

        assertSame(teamId, participant.getTeamId());
        assertSame(team, match.getTeam(teamId));
        assertEquals(true, team.contains(participantId));
    }

    @Test
    public void participantCanLeaveTeam() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        UhcParticipant participant = match.addParticipant(participantId, "Alice");
        UhcTeam team = match.createTeam(teamId, "Red", UUID.randomUUID());

        match.joinTeam(teamId, participantId);
        match.leaveTeam(participantId);

        assertEquals(null, participant.getTeamId());
        assertEquals(false, team.contains(participantId));
        assertSame(team, match.getTeam(teamId));
    }

    @Test
    public void joiningNewTeamMovesParticipantFromPreviousTeam() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();
        UUID firstTeamId = UUID.randomUUID();
        UUID secondTeamId = UUID.randomUUID();

        UhcParticipant participant = match.addParticipant(participantId, "Alice");
        UhcTeam firstTeam = match.createTeam(firstTeamId, "Red", UUID.randomUUID());
        UhcTeam secondTeam = match.createTeam(secondTeamId, "Blue", UUID.randomUUID());

        match.joinTeam(firstTeamId, participantId);
        match.joinTeam(secondTeamId, participantId);

        assertSame(secondTeamId, participant.getTeamId());
        assertEquals(false, firstTeam.contains(participantId));
        assertEquals(true, secondTeam.contains(participantId));
    }

    @Test
    public void emptyTeamIsRemovedWhenLastParticipantLeaves() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        UhcParticipant participant = match.addParticipant(participantId, "Alice");
        match.createTeam(teamId, "Red", participantId);

        match.joinTeam(teamId, participantId);
        match.leaveTeam(participantId);

        assertEquals(null, participant.getTeamId());
        assertEquals(null, match.getTeam(teamId));
        assertEquals(0, match.getTeams().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotJoinMissingTeam() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();

        match.addParticipant(participantId, "Alice");
        match.joinTeam(UUID.randomUUID(), participantId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotJoinMissingParticipant() {
        UhcMatch match = UhcMatch.create();
        UUID teamId = UUID.randomUUID();

        match.createTeam(teamId, "Red", UUID.randomUUID());
        match.joinTeam(teamId, UUID.randomUUID());
    }

    @Test
    public void teamWithAliveParticipantIsAlive() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        match.addParticipant(participantId, "Alice");
        UhcTeam team = match.createTeam(teamId, "Red", UUID.randomUUID());
        match.joinTeam(teamId, participantId);

        assertEquals(1, match.getAliveTeams().size());
        assertSame(team, match.getAliveTeams().iterator().next());
    }

    @Test
    public void teamWithOnlyDeadParticipantsIsNotAlive() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        UhcParticipant participant = match.addParticipant(participantId, "Alice");
        match.createTeam(teamId, "Red", UUID.randomUUID());
        match.joinTeam(teamId, participantId);

        participant.markDead();

        assertEquals(0, match.getAliveTeams().size());
    }

    @Test
    public void teamWithUnknownMembersIsNotAlive() {
        UhcMatch match = UhcMatch.create();

        match.createTeam(UUID.randomUUID(), "Red", UUID.randomUUID());

        assertEquals(0, match.getAliveTeams().size());
    }

    @Test
    public void aliveTeamsAreReadOnlyFromOutside() {
        UhcMatch match = UhcMatch.create();
        UUID participantId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        match.addParticipant(participantId, "Alice");
        match.createTeam(teamId, "Red", UUID.randomUUID());
        match.joinTeam(teamId, participantId);

        try {
            match.getAliveTeams().clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }

        throw new AssertionError("aliveTeams should be unmodifiable.");
    }

    @Test
    public void winnerCandidateReturnsOnlyAliveTeam() {
        UhcMatch match = UhcMatch.create();
        UUID winnerParticipantId = UUID.randomUUID();
        UUID deadParticipantId = UUID.randomUUID();
        UUID winnerTeamId = UUID.randomUUID();
        UUID deadTeamId = UUID.randomUUID();

        UhcParticipant deadParticipant = match.addParticipant(deadParticipantId, "Bob");
        UhcTeam winnerTeam = match.createTeam(winnerTeamId, "Red", UUID.randomUUID());
        match.createTeam(deadTeamId, "Blue", UUID.randomUUID());

        match.addParticipant(winnerParticipantId, "Alice");
        match.joinTeam(winnerTeamId, winnerParticipantId);
        match.joinTeam(deadTeamId, deadParticipantId);
        deadParticipant.markDead();

        assertSame(winnerTeam, match.getWinnerCandidate());
    }

    @Test
    public void winnerCandidateIsNullWhenNoTeamsAreAlive() {
        UhcMatch match = UhcMatch.create();

        match.createTeam(UUID.randomUUID(), "Red", UUID.randomUUID());

        assertEquals(null, match.getWinnerCandidate());
    }

    @Test
    public void winnerCandidateIsNullWhenMultipleTeamsAreAlive() {
        UhcMatch match = UhcMatch.create();
        UUID firstParticipantId = UUID.randomUUID();
        UUID secondParticipantId = UUID.randomUUID();
        UUID firstTeamId = UUID.randomUUID();
        UUID secondTeamId = UUID.randomUUID();

        match.addParticipant(firstParticipantId, "Alice");
        match.addParticipant(secondParticipantId, "Bob");
        match.createTeam(firstTeamId, "Red", UUID.randomUUID());
        match.createTeam(secondTeamId, "Blue", UUID.randomUUID());

        match.joinTeam(firstTeamId, firstParticipantId);
        match.joinTeam(secondTeamId, secondParticipantId);

        assertEquals(null, match.getWinnerCandidate());
    }
}
