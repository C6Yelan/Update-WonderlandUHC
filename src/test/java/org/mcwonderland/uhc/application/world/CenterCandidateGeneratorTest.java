package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CenterCandidateGeneratorTest {

    @Test
    public void firstLayerStartsWithZeroZeroAndContainsNineCenters() {
        List<MatchCenter> centers = CenterCandidateGenerator.firstLayer(2000);

        assertEquals(9, centers.size());
        assertEquals(new MatchCenter(0, 0, 2000), centers.get(0));
        assertEquals(9, new HashSet<>(centers).size());
    }

    @Test
    public void firstLayerUsesChunkRoundedOffset() {
        List<MatchCenter> centers = CenterCandidateGenerator.firstLayer(3000);

        assertEquals(1120, CenterCandidateGenerator.offset(3000));
        assertTrue(centers.contains(new MatchCenter(1120, 0, 3000)));
        assertTrue(centers.contains(new MatchCenter(-1120, -1120, 3000)));
    }

    @Test
    public void offsetIsClampedForSmallAndLargeBorders() {
        assertEquals(384, CenterCandidateGenerator.offset(128));
        assertEquals(2000, CenterCandidateGenerator.offset(6000));
    }

    @Test
    public void secondLayerAddsSixteenExtraCenters() {
        List<MatchCenter> centers = CenterCandidateGenerator.withSecondLayer(2000);

        assertEquals(25, centers.size());
        assertEquals(25, new HashSet<>(centers).size());
        assertTrue(centers.contains(new MatchCenter(1504, 0, 2000)));
        assertTrue(centers.contains(new MatchCenter(752, -1504, 2000)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void offsetRejectsInvalidBorderSize() {
        CenterCandidateGenerator.offset(0);
    }
}
