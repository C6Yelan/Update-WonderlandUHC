package org.mcwonderland.uhc.port;

public interface SchedulerPort {

    void runLater(long delayTicks, Runnable task);
}
