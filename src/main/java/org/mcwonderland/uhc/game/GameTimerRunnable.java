package org.mcwonderland.uhc.game;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mcwonderland.uhc.application.match.MatchTimerTickResult;
import org.mcwonderland.uhc.application.match.MatchTimerTickUseCase;
import org.mcwonderland.uhc.game.timer.Timer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GameTimerRunnable implements Runnable {
    private static final MatchTimerTickUseCase TICK_USE_CASE = new MatchTimerTickUseCase();
    public static int tick = 1;
    public static int totalSecond = 0;
    public static boolean RUN = false;

    public static void start() {
        PluginScheduler.runTimer(1, new GameTimerRunnable());
    }

    @Override
    public void run() {
        MatchTimerTickResult tickResult = TICK_USE_CASE.advance(RUN, tick, totalSecond);

        if (!tickResult.shouldRunTimers())
            return;

        tick = tickResult.getExecutionTick();
        totalSecond = tickResult.getTotalSecond();

        for (Timer timer : Game.getGame().getCurrentState().getTimers()) {
            if (tick % timer.runTick() == 0)
                timer.run();
        }

        tick++;
    }
}
