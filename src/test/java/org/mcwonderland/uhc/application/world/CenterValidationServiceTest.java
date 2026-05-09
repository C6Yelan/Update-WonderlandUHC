package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CenterValidationServiceTest {

    @Test
    public void serviceReturnsBestCandidateFromBoundedSearch() {
        FakeSource source = new FakeSource();
        source.oceanHalfSize = 180;

        CenterValidationService service = new CenterValidationService(new CenterWorldSampleReader(source, 16));
        CenterSearchResult result = service.findBestCenter(400, CenterValidationService.Options.builder()
                .candidateLimit(2)
                .build());

        assertSame(CenterSearchStatus.RECOMMENDED, result.getStatus());
        assertNotNull(result.getBestCandidate());
        assertEquals(384, result.getBestCandidate().getCenter().getX());
        assertEquals(0, result.getBestCandidate().getCenter().getZ());
        assertTrue(result.shouldPregenerate());
    }

    @Test
    public void candidateLimitCanKeepOnlyZeroZeroCandidate() {
        FakeSource source = new FakeSource();
        source.oceanHalfSize = 180;

        CenterValidationService service = new CenterValidationService(new CenterWorldSampleReader(source, 16));
        CenterSearchResult result = service.findBestCenter(400, CenterValidationService.Options.builder()
                .candidateLimit(1)
                .build());

        assertSame(CenterSearchStatus.REJECTED, result.getStatus());
        assertEquals(0, result.getBestCandidate().getCenter().getX());
        assertEquals(0, result.getBestCandidate().getCenter().getZ());
    }

    @Test
    public void serviceCanReturnCancelledBeforeScoringCandidate() {
        CenterValidationService service = new CenterValidationService(new CenterWorldSampleReader(new FakeSource(), 16));

        CenterSearchResult result = service.findBestCenter(400, CenterValidationService.Options.builder()
                .cancellation(() -> true)
                .build());

        assertSame(CenterSearchStatus.CANCELLED, result.getStatus());
        assertEquals(null, result.getBestCandidate());
    }

    @Test
    public void softTimeLimitReturnsCurrentBestAfterCompletedCandidate() {
        CenterValidationService service = new CenterValidationService(new CenterWorldSampleReader(new FakeSource(), 16));

        CenterSearchResult result = service.findBestCenter(400, CenterValidationService.Options.builder()
                .candidateLimit(2)
                .softTimeLimitMillis(1L)
                .hardTimeLimitMillis(10_000L)
                .timeSource(new IncrementingTimeSource())
                .build());

        assertSame(CenterSearchStatus.TIME_LIMITED, result.getStatus());
        assertTrue(result.isTimeLimited());
        assertNotNull(result.getBestCandidate());
    }

    @Test
    public void progressListenerReceivesValidationStagesAndScores() {
        List<CenterValidationService.CenterValidationStage> stages = new ArrayList<>();
        List<CenterCandidateScore> scored = new ArrayList<>();

        CenterValidationService service = new CenterValidationService(new CenterWorldSampleReader(new FakeSource(), 16));
        service.findBestCenter(400, CenterValidationService.Options.builder()
                .candidateLimit(1)
                .progressListener(new CenterValidationService.ProgressListener() {
                    @Override
                    public void onStage(MatchCenter center, CenterValidationService.CenterValidationStage stage, int candidateIndex, int candidateCount) {
                        stages.add(stage);
                    }

                    @Override
                    public void onCandidateScored(CenterCandidateScore score, CenterCandidateScore bestScore, int candidateIndex, int candidateCount) {
                        scored.add(score);
                    }
                })
                .build());

        assertEquals(3, stages.size());
        assertSame(CenterValidationService.CenterValidationStage.COARSE_SCAN, stages.get(0));
        assertSame(CenterValidationService.CenterValidationStage.DETAILED_SCAN, stages.get(1));
        assertSame(CenterValidationService.CenterValidationStage.CENTER_SCAN, stages.get(2));
        assertEquals(1, scored.size());
    }

    private static final class IncrementingTimeSource implements LongSupplier {
        private long value;

        @Override
        public long getAsLong() {
            return value++;
        }
    }

    private static final class FakeSource implements CenterSampleSource {
        private int oceanHalfSize;

        @Override
        public int getMinHeight() {
            return -64;
        }

        @Override
        public int getMaxHeight() {
            return 320;
        }

        @Override
        public int getSeaLevel() {
            return 63;
        }

        @Override
        public int getSurfaceY(int x, int z) {
            return isOceanArea(x, z) ? 63 : 80;
        }

        @Override
        public String getBiomeKey(int x, int z) {
            return isOceanArea(x, z) ? "minecraft:ocean" : "minecraft:plains";
        }

        @Override
        public String getSurfaceMaterialKey(int x, int z) {
            return isOceanArea(x, z) ? "minecraft:water" : "minecraft:grass_block";
        }

        @Override
        public boolean isStandable(int x, int z) {
            return !isOceanArea(x, z);
        }

        @Override
        public boolean isWaterSurface(int x, int z) {
            return isOceanArea(x, z);
        }

        private boolean isOceanArea(int x, int z) {
            return Math.abs(x) <= oceanHalfSize && Math.abs(z) <= oceanHalfSize;
        }
    }
}
