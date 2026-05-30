package org.mcwonderland.uhc.port;

public interface WorldBorderPort {

    void reset(String worldName);

    void setCenter(String worldName, double x, double z);

    void setSize(String worldName, double size);

    void changeSize(String worldName, double size, long ticks);

    void setWarningDistance(String worldName, int blocks);

    void setWarningTimeTicks(String worldName, int ticks);

    void setDamageBuffer(String worldName, double blocks);

    double getSize(String worldName);
}
