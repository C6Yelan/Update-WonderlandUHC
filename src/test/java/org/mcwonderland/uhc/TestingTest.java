package org.mcwonderland.uhc;


import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mcwonderland.uhc.game.StateName;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TestingTest {
    private ServerMock server;

    @Before
    public void setUp() {
        server = MockBukkit.mock();
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void mockBukkitCreatesOnlinePlayerInDefaultWorld() {
        PlayerMock player = server.addPlayer();

        assertTrue(player.isOnline());
        assertNotNull(player.getWorld());
        assertSame(server.getWorlds().get(0), player.getWorld());
        assertNotNull(player.getLocation());
    }

    @Test
    public void stateNameClassifiesLobbyTeleportAndStartedPhases() {
        assertTrue(StateName.WAITING.isLobby());
        assertFalse(StateName.WAITING.isTeleport());
        assertFalse(StateName.WAITING.isStarted());

        assertFalse(StateName.TELEPORTING.isLobby());
        assertTrue(StateName.TELEPORTING.isTeleport());
        assertFalse(StateName.TELEPORTING.isStarted());

        assertFalse(StateName.PRE_START.isLobby());
        assertTrue(StateName.PRE_START.isTeleport());
        assertFalse(StateName.PRE_START.isStarted());

        assertFalse(StateName.PLAYING.isLobby());
        assertFalse(StateName.PLAYING.isTeleport());
        assertTrue(StateName.PLAYING.isStarted());
    }

}
