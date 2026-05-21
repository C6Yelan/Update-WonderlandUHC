package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CenterCandidateScoreTest {
    private final MatchCenter center = new MatchCenter(0, 0, 2000);

    @Test
    public void lowRiskCandidateIsRecommended() {
        CenterCandidateScore score = CenterCandidateScore.builder(center)
                .oceanRatio(0.04D)
                .waterRatio(0.06D)
                .forestRatio(0.20D)
                .denseForestRatio(0.04D)
                .highlandRatio(0.08D)
                .extremeHighlandRatio(0.02D)
                .maxSectionWaterRatio(0.20D)
                .maxAdjacentSectionWaterRatio(0.18D)
                .centerWaterRatio(0.03D)
                .centerStandableRatio(0.92D)
                .centerCliffRatio(0.02D)
                .centerHeightSpread(18)
                .lowSectionCount(0)
                .build();

        assertSame(CenterSearchStatus.RECOMMENDED, score.getStatus());
        assertTrue(score.getTotalScore() >= 75D);
        assertTrue(score.getReasons().isEmpty());
    }

    @Test
    public void extremeCenterLargeWaterLimitRejectsCandidate() {
        CenterCandidateScore score = CenterCandidateScore.builder(center)
                .centerWaterRatio(0.46D)
                .centerLargeWaterRatio(0.36D)
                .centerStandableRatio(0.90D)
                .build();

        assertSame(CenterSearchStatus.REJECTED, score.getStatus());
        assertTrue(score.getReasons().contains(CenterScoreReason.CENTER_WATER_TOO_HIGH));
    }

    @Test
    public void smallWaterDoesNotTriggerLargeWaterReject() {
        CenterCandidateScore score = CenterCandidateScore.builder(center)
                .oceanRatio(0.06D)
                .riverRatio(0.18D)
                .waterRatio(0.36D)
                .forestRatio(0.20D)
                .denseForestRatio(0.04D)
                .highlandRatio(0.08D)
                .extremeHighlandRatio(0.02D)
                .maxSectionWaterRatio(0.70D)
                .maxAdjacentSectionWaterRatio(0.60D)
                .maxSectionLargeWaterRatio(0.32D)
                .maxAdjacentSectionLargeWaterRatio(0.28D)
                .centerWaterRatio(0.18D)
                .centerLargeWaterRatio(0.08D)
                .centerStandableRatio(0.86D)
                .centerCliffRatio(0.04D)
                .centerHeightSpread(24)
                .lowSectionCount(0)
                .build();

        assertTrue(score.getStatus() != CenterSearchStatus.REJECTED);
        assertFalse(score.getReasons().contains(CenterScoreReason.SECTION_WATER_TOO_HIGH));
        assertFalse(score.getReasons().contains(CenterScoreReason.CENTER_WATER_TOO_HIGH));
    }

    @Test
    public void tooManyLowSectionsRejectCandidate() {
        CenterCandidateScore score = CenterCandidateScore.builder(center)
                .forestRatio(0.20D)
                .centerStandableRatio(0.90D)
                .lowSectionCount(8)
                .build();

        assertSame(CenterSearchStatus.REJECTED, score.getStatus());
        assertTrue(score.getReasons().contains(CenterScoreReason.TOO_MANY_LOW_SECTIONS));
    }

    @Test
    public void softReasonPreventsRecommendedButKeepsCandidateUsable() {
        CenterCandidateScore score = CenterCandidateScore.builder(center)
                .oceanRatio(0.04D)
                .waterRatio(0.06D)
                .forestRatio(0.20D)
                .denseForestRatio(0.29D)
                .highlandRatio(0.08D)
                .extremeHighlandRatio(0.02D)
                .maxSectionWaterRatio(0.20D)
                .maxAdjacentSectionWaterRatio(0.18D)
                .centerWaterRatio(0.03D)
                .centerStandableRatio(0.92D)
                .centerCliffRatio(0.02D)
                .centerHeightSpread(18)
                .lowSectionCount(0)
                .build();

        assertSame(CenterSearchStatus.ACCEPTABLE, score.getStatus());
        assertTrue(score.getTotalScore() >= 65D);
        assertTrue(score.getReasons().contains(CenterScoreReason.DENSE_FOREST_RATIO_TOO_HIGH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ratioValuesMustStayInBounds() {
        CenterCandidateScore.builder(center)
                .waterRatio(1.01D)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void centerHeightSpreadCannotBeNegative() {
        CenterCandidateScore.builder(center)
                .centerHeightSpread(-1)
                .build();
    }

    @Test
    public void completedSearchUsesCandidateStatus() {
        CenterCandidateScore score = CenterCandidateScore.builder(center)
                .forestRatio(0.20D)
                .centerStandableRatio(0.90D)
                .build();

        CenterSearchResult result = CenterSearchResult.completed(score, 1200L);

        assertSame(score, result.getBestCandidate());
        assertEquals(1200L, result.getElapsedMillis());
        assertSame(score.getStatus(), result.getStatus());
    }

    @Test
    public void timeLimitedSearchKeepsBestCandidate() {
        CenterCandidateScore score = CenterCandidateScore.builder(center)
                .forestRatio(0.20D)
                .centerStandableRatio(0.90D)
                .build();

        CenterSearchResult result = CenterSearchResult.timeLimited(score, 90000L);

        assertSame(score, result.getBestCandidate());
        assertSame(CenterSearchStatus.TIME_LIMITED, result.getStatus());
        assertTrue(result.isTimeLimited());
    }
}
