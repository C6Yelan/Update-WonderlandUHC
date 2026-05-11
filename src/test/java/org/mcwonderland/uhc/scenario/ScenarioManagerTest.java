package org.mcwonderland.uhc.scenario;

import org.bukkit.inventory.ItemStack;
import org.junit.Test;
import org.mcwonderland.uhc.api.Scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ScenarioManagerTest {

    @Test
    public void registerIsolatesOnlyFailingScenario() {
        ScenarioManager manager = new ScenarioManager(failure -> {
        });

        TestScenario stable = new TestScenario("Stable");
        TestScenario broken = new TestScenario("Broken");
        broken.failReload = true;

        manager.register(stable);
        manager.register(broken);

        assertTrue(manager.isScenarioAvailable("Stable"));
        assertFalse(manager.isScenarioAvailable("Broken"));
        assertEquals(1, manager.getScenarios().size());
        assertEquals(1, manager.getRegistrationFailures().size());
        assertEquals("reload", manager.getRegistrationFailure("Broken").getAction());
        assertNull(manager.getScenario("Broken"));
    }

    @Test
    public void reloadAllKeepsHealthyScenariosAvailable() {
        ScenarioManager manager = new ScenarioManager(failure -> {
        });

        TestScenario stable = new TestScenario("Stable");
        TestScenario brokenAfterRegister = new TestScenario("BrokenAfterRegister");

        manager.register(stable);
        manager.register(brokenAfterRegister);

        brokenAfterRegister.failReload = true;
        manager.reloadAll();

        assertTrue(manager.isScenarioAvailable("Stable"));
        assertFalse(manager.isScenarioAvailable("BrokenAfterRegister"));
        assertEquals(1, manager.getScenarios().size());
        assertEquals("reload", manager.getRegistrationFailure("BrokenAfterRegister").getAction());
    }

    @Test
    public void toggleScenarioIsolatesEnableFailure() {
        ScenarioManager manager = new ScenarioManager(failure -> {
        });
        TestScenario broken = new TestScenario("BrokenEnable");

        manager.register(broken);

        broken.failEnable = true;
        boolean toggled = manager.toggleScenario(broken, true);

        assertFalse(toggled);
        assertFalse(manager.isScenarioAvailable("BrokenEnable"));
        assertEquals("enable", manager.getRegistrationFailure("BrokenEnable").getAction());
    }

    @Test
    public void reopenEnabledScenariosDoesNotStopAfterOneFailure() {
        ScenarioManager manager = new ScenarioManager(failure -> {
        });
        TestScenario broken = new TestScenario("BrokenReopen");
        TestScenario stable = new TestScenario("StableReopen");

        manager.register(broken);
        manager.register(stable);
        manager.toggleScenario(broken, true);
        manager.toggleScenario(stable, true);

        broken.failEnable = true;
        manager.reopenEnabledScenarios();

        assertFalse(manager.isScenarioAvailable("BrokenReopen"));
        assertTrue(manager.isScenarioAvailable("StableReopen"));
        assertTrue(stable.enabled);
    }

    private static final class TestScenario implements Scenario {

        private final String name;
        private boolean enabled;
        private boolean failReload;
        private boolean failEnable;

        private TestScenario(String name) {
            this.name = name;
        }

        @Override
        public void reload() {
            if (failReload)
                throw new IllegalStateException("reload failed");
        }

        @Override
        public void enable() {
            if (failEnable)
                throw new IllegalStateException("enable failed");

            enabled = true;
        }

        @Override
        public void disable() {
            enabled = false;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public ItemStack getIcon() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getFancyName() {
            return name;
        }
    }
}
