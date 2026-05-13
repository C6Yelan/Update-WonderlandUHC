package org.mcwonderland.uhc.scenario.impl.rush;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScenarioCutCleanTest {

    @Test
    public void cutCleanSetsBlockExpWhenDropsChangedAndExpIsUnclaimed() {
        assertTrue(ScenarioCutClean.shouldSetBlockExp(true, false));
    }

    @Test
    public void cutCleanDoesNotRestoreExpWhenAnotherScenarioAlreadyChangedIt() {
        assertFalse(ScenarioCutClean.shouldSetBlockExp(true, true));
    }

    @Test
    public void cutCleanDoesNotSetBlockExpWhenDropsDidNotChange() {
        assertFalse(ScenarioCutClean.shouldSetBlockExp(false, false));
    }
}
