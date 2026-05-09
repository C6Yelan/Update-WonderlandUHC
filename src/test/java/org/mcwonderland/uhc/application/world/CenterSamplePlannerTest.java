package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CenterSamplePlannerTest {

    @Test
    public void coarseSamplesUseFixedOneHundredPointBudget() {
        MatchCenter center = new MatchCenter(0, 0, 2000);
        List<CenterSamplePoint> samples = CenterSamplePlanner.coarseSamples(center);

        assertEquals(100, samples.size());
        assertEquals(100, new HashSet<>(samples).size());
        assertEquals(new CenterSamplePoint(-900, -900), samples.get(0));
        assertTrue(samples.contains(new CenterSamplePoint(900, 900)));
    }

    @Test
    public void detailedSamplesUseFixedTwoHundredEightyNinePointBudget() {
        MatchCenter center = new MatchCenter(100, -200, 2000);
        List<CenterSamplePoint> samples = CenterSamplePlanner.detailedSamples(center);

        assertEquals(289, samples.size());
        assertEquals(289, new HashSet<>(samples).size());
        assertEquals(new CenterSamplePoint(-841, -1141), samples.get(0));
        assertTrue(samples.contains(new CenterSamplePoint(1041, 741)));
    }

    @Test
    public void centerRefinementSamplesUseFixedOneHundredTwentyOnePointBudget() {
        MatchCenter center = new MatchCenter(100, -200, 2000);
        List<CenterSamplePoint> samples = CenterSamplePlanner.centerRefinementSamples(center);

        assertEquals(121, samples.size());
        assertEquals(121, new HashSet<>(samples).size());
        assertTrue(samples.contains(new CenterSamplePoint(100, -200)));
        assertEquals(new CenterSamplePoint(-20, -320), samples.get(0));
    }

    @Test
    public void centerRadiusIsClamped() {
        assertEquals(96, CenterSamplePlanner.centerRadius(1000));
        assertEquals(120, CenterSamplePlanner.centerRadius(2000));
        assertEquals(192, CenterSamplePlanner.centerRadius(4000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void samplePlanningRejectsNullCenter() {
        CenterSamplePlanner.coarseSamples(null);
    }
}
