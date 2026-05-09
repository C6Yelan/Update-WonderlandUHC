package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CenterBiomeClassifierTest {

    @Test
    public void classifierAcceptsNamespacedAndUppercaseKeys() {
        assertTrue(CenterBiomeClassifier.isOcean("minecraft:deep_ocean"));
        assertTrue(CenterBiomeClassifier.isDenseForest("JUNGLE"));
        assertTrue(CenterBiomeClassifier.isMountainHint("minecraft:stony_peaks"));
    }

    @Test
    public void classifierSeparatesKnownCategories() {
        assertTrue(CenterBiomeClassifier.isWaterLike("river"));
        assertTrue(CenterBiomeClassifier.isForest("cherry_grove"));

        assertFalse(CenterBiomeClassifier.isOcean("river"));
        assertFalse(CenterBiomeClassifier.isDenseForest("forest"));
        assertFalse(CenterBiomeClassifier.isMountainHint("plains"));
    }

    @Test
    public void classifierTreatsUnknownOrBlankKeysAsOther() {
        assertFalse(CenterBiomeClassifier.isOcean("minecraft:plains"));
        assertFalse(CenterBiomeClassifier.isWaterLike(""));
        assertFalse(CenterBiomeClassifier.isForest(null));
    }
}
