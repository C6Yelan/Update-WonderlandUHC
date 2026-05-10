package org.mcwonderland.uhc.application.match;

public final class MatchTimerTickResult {
    private final boolean runTimers;
    private final int executionTick;
    private final int totalSecond;

    private MatchTimerTickResult(boolean runTimers, int executionTick, int totalSecond) {
        this.runTimers = runTimers;
        this.executionTick = executionTick;
        this.totalSecond = totalSecond;
    }

    public static MatchTimerTickResult skipped(int tick, int totalSecond) {
        return new MatchTimerTickResult(false, tick, totalSecond);
    }

    public static MatchTimerTickResult running(int executionTick, int totalSecond) {
        return new MatchTimerTickResult(true, executionTick, totalSecond);
    }

    public boolean shouldRunTimers() {
        return runTimers;
    }

    public int getExecutionTick() {
        return executionTick;
    }

    public int getTotalSecond() {
        return totalSecond;
    }
}
