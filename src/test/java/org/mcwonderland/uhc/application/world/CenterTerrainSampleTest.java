package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CenterTerrainSampleTest {

    @Test
    public void sampleClassifiesHighlandsAndCliffsFromThresholds() {
        CenterTerrainSample sample = new CenterTerrainSample(
                new CenterSamplePoint(0, 0),
                "minecraft:stony_peaks",
                "minecraft:stone",
                149,
                63,
                -64,
                320,
                24,
                true,
                false
        );

        assertTrue(sample.isHighland());
        assertTrue(sample.isExtremeHighland());
        assertTrue(sample.isRoughSlope());
        assertTrue(sample.isCliff());
    }

    @Test
    public void sampleDoesNotOverClassifyNormalTerrain() {
        CenterTerrainSample sample = new CenterTerrainSample(
                new CenterSamplePoint(0, 0),
                "minecraft:plains",
                "minecraft:grass_block",
                80,
                63,
                -64,
                320,
                4,
                true,
                false
        );

        assertFalse(sample.isHighland());
        assertFalse(sample.isExtremeHighland());
        assertFalse(sample.isRoughSlope());
        assertFalse(sample.isCliff());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sampleRejectsSurfaceOutsideHeightRange() {
        new CenterTerrainSample(
                new CenterSamplePoint(0, 0),
                "minecraft:plains",
                "minecraft:grass_block",
                400,
                63,
                -64,
                320,
                0,
                true,
                false
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void sampleRejectsExclusiveMaxHeight() {
        new CenterTerrainSample(
                new CenterSamplePoint(0, 0),
                "minecraft:plains",
                "minecraft:grass_block",
                320,
                63,
                -64,
                320,
                0,
                true,
                false
        );
    }
}
