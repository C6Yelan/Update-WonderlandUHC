package org.mcwonderland.uhc.application.match;

import org.mcwonderland.uhc.core.match.MatchState;

public final class MatchTransitionResult {
    private final MatchTransitionStatus status;
    private final MatchState sourceState;
    private final MatchState targetState;
    private final String failureReason;

    private MatchTransitionResult(MatchTransitionStatus status, MatchState sourceState, MatchState targetState, String failureReason) {
        if (status == null)
            throw new IllegalArgumentException("status cannot be null.");

        this.status = status;
        this.sourceState = sourceState;
        this.targetState = targetState;
        this.failureReason = failureReason;
    }

    public static MatchTransitionResult success(MatchState sourceState, MatchState targetState) {
        return new MatchTransitionResult(MatchTransitionStatus.SUCCESS, sourceState, targetState, null);
    }

    public static MatchTransitionResult failure(MatchTransitionStatus status, MatchState sourceState, MatchState targetState, String failureReason) {
        if (status == MatchTransitionStatus.SUCCESS)
            throw new IllegalArgumentException("success status cannot be used for a failure.");

        if (failureReason == null || failureReason.isEmpty())
            throw new IllegalArgumentException("failureReason cannot be empty.");

        return new MatchTransitionResult(status, sourceState, targetState, failureReason);
    }

    public boolean isSuccess() {
        return status == MatchTransitionStatus.SUCCESS;
    }

    public MatchTransitionStatus getStatus() {
        return status;
    }

    public MatchState getSourceState() {
        return sourceState;
    }

    public MatchState getTargetState() {
        return targetState;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
