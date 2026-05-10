package org.mcwonderland.uhc.application.match;

public final class MatchTimerTickUseCase {

    public MatchTimerTickResult advance(boolean running, int tick, int totalSecond) {
        if (!running)
            return MatchTimerTickResult.skipped(tick, totalSecond);

        int executionTick = tick;
        int currentTotalSecond = totalSecond;

        if (executionTick > 19) {
            currentTotalSecond++;
            executionTick = 0;
        }

        return MatchTimerTickResult.running(executionTick, currentTotalSecond);
    }
}
