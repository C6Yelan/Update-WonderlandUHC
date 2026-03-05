package org.mcwonderland.uhc;


import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
    public void test() {
        PlayerMock player = server.addPlayer();
        Assert.assertEquals(1, 1);
    }

}
