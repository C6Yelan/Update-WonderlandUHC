package org.mcwonderland.uhc.port;

public interface WorldPort {

    boolean worldExists(String worldName);

    void createWorld(String worldName);

    void createNetherWorld(String worldName);
}
