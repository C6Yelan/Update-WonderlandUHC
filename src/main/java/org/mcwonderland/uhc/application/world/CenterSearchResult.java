package org.mcwonderland.uhc.application.world;

public final class CenterSearchResult {
    private final CenterCandidateScore bestCandidate;
    private final CenterSearchStatus status;
    private final boolean timeLimited;
    private final long elapsedMillis;

    private CenterSearchResult(CenterCandidateScore bestCandidate, CenterSearchStatus status, boolean timeLimited, long elapsedMillis) {
        if (status == null)
            throw new IllegalArgumentException("status cannot be null.");
        if (elapsedMillis < 0L)
            throw new IllegalArgumentException("elapsedMillis cannot be negative.");

        this.bestCandidate = bestCandidate;
        this.status = status;
        this.timeLimited = timeLimited;
        this.elapsedMillis = elapsedMillis;
    }

    public static CenterSearchResult completed(CenterCandidateScore bestCandidate, long elapsedMillis) {
        if (bestCandidate == null)
            throw new IllegalArgumentException("bestCandidate cannot be null.");

        return new CenterSearchResult(bestCandidate, bestCandidate.getStatus(), false, elapsedMillis);
    }

    public static CenterSearchResult timeLimited(CenterCandidateScore bestCandidate, long elapsedMillis) {
        return new CenterSearchResult(bestCandidate, CenterSearchStatus.TIME_LIMITED, true, elapsedMillis);
    }

    public static CenterSearchResult cancelled(CenterCandidateScore bestCandidate, long elapsedMillis) {
        return new CenterSearchResult(bestCandidate, CenterSearchStatus.CANCELLED, false, elapsedMillis);
    }

    public CenterCandidateScore getBestCandidate() {
        return bestCandidate;
    }

    public CenterSearchStatus getStatus() {
        return status;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public boolean shouldPregenerate() {
        return bestCandidate != null && status.shouldPregenerate();
    }
}
