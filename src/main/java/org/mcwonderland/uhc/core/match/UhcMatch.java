package org.mcwonderland.uhc.core.match;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class UhcMatch {
    private MatchState state;
    private MatchSettings settings;
    private final Map<UUID, UhcParticipant> participants = new LinkedHashMap<>();
    private final Map<UUID, UhcTeam> teams = new LinkedHashMap<>();

    private UhcMatch(MatchSettings settings) {
        this.state = MatchState.WAITING;
        this.settings = settings;
    }

    public static UhcMatch create() {
        return create(MatchSettings.defaults());
    }

    public static UhcMatch create(MatchSettings settings) {
        if (settings == null)
            throw new IllegalArgumentException("settings cannot be null.");

        return new UhcMatch(settings);
    }

    public MatchState getState() {
        return state;
    }

    public MatchSettings getSettings() {
        return settings;
    }

    public void updateSettings(MatchSettings settings) {
        if (settings == null)
            throw new IllegalArgumentException("settings cannot be null.");

        this.settings = settings;
    }

    public MatchState advanceState() {
        this.state = this.state.nextState();
        return this.state;
    }

    public UhcParticipant addParticipant(UUID uniqueId, String name) {
        UhcParticipant existing = participants.get(uniqueId);

        if (existing != null)
            return existing;

        UhcParticipant participant = new UhcParticipant(uniqueId, name);
        participants.put(uniqueId, participant);
        return participant;
    }

    public UhcParticipant getParticipant(UUID uniqueId) {
        return participants.get(uniqueId);
    }

    public Collection<UhcParticipant> getParticipants() {
        return Collections.unmodifiableCollection(participants.values());
    }

    public UhcTeam createTeam(UUID teamId, String name, UUID ownerId) {
        UhcTeam existing = teams.get(teamId);

        if (existing != null)
            return existing;

        UhcTeam team = new UhcTeam(teamId, name, ownerId);
        teams.put(teamId, team);
        return team;
    }

    public UhcTeam getTeam(UUID teamId) {
        return teams.get(teamId);
    }

    public Collection<UhcTeam> getTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    public Collection<UhcTeam> getAliveTeams() {
        List<UhcTeam> aliveTeams = new ArrayList<>();

        for (UhcTeam team : teams.values()) {
            if (hasAliveMember(team))
                aliveTeams.add(team);
        }

        return Collections.unmodifiableList(aliveTeams);
    }

    public UhcTeam getWinnerCandidate() {
        Collection<UhcTeam> aliveTeams = getAliveTeams();

        if (aliveTeams.size() != 1)
            return null;

        return aliveTeams.iterator().next();
    }

    public void joinTeam(UUID teamId, UUID participantId) {
        UhcTeam team = requireTeam(teamId);
        UhcParticipant participant = requireParticipant(participantId);

        if (teamId.equals(participant.getTeamId()))
            return;

        leaveCurrentTeam(participant);
        team.addMember(participantId);
        participant.assignTeam(teamId);
    }

    public void leaveTeam(UUID participantId) {
        UhcParticipant participant = requireParticipant(participantId);
        leaveCurrentTeam(participant);
    }

    private void leaveCurrentTeam(UhcParticipant participant) {
        UUID currentTeamId = participant.getTeamId();

        if (currentTeamId == null)
            return;

        UhcTeam currentTeam = teams.get(currentTeamId);

        if (currentTeam != null) {
            currentTeam.removeMember(participant.getUniqueId());

            if (currentTeam.isEmpty())
                teams.remove(currentTeamId);
        }

        participant.clearTeam();
    }

    private UhcParticipant requireParticipant(UUID participantId) {
        UhcParticipant participant = participants.get(participantId);

        if (participant == null)
            throw new IllegalArgumentException("Unknown participant: " + participantId);

        return participant;
    }

    private UhcTeam requireTeam(UUID teamId) {
        UhcTeam team = teams.get(teamId);

        if (team == null)
            throw new IllegalArgumentException("Unknown team: " + teamId);

        return team;
    }

    private boolean hasAliveMember(UhcTeam team) {
        for (UUID memberId : team.getMemberIds()) {
            UhcParticipant participant = participants.get(memberId);

            if (participant != null && participant.isAlive())
                return true;
        }

        return false;
    }
}
