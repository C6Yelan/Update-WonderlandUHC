package org.mcwonderland.uhc.application.world;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

public final class CenterValidationService {
    private static final int MAX_CANDIDATES = 25;
    private static final long DEFAULT_SOFT_TIME_LIMIT_MILLIS = 150_000L;
    private static final long DEFAULT_HARD_TIME_LIMIT_MILLIS = 180_000L;

    private final CenterWorldSampleReader sampleReader;

    public CenterValidationService(World world) {
        this(new CenterWorldSampleReader(world));
    }

    CenterValidationService(CenterWorldSampleReader sampleReader) {
        if (sampleReader == null)
            throw new IllegalArgumentException("sampleReader cannot be null.");

        this.sampleReader = sampleReader;
    }

    public CenterSearchResult findBestCenter(int initialBorderSize) {
        return findBestCenter(initialBorderSize, Options.defaults());
    }

    public CenterSearchResult findBestCenter(int initialBorderSize, Options options) {
        if (initialBorderSize <= 0)
            throw new IllegalArgumentException("initialBorderSize must be positive.");
        if (options == null)
            throw new IllegalArgumentException("options cannot be null.");

        long startMillis = options.now();
        List<MatchCenter> candidates = candidates(initialBorderSize, options.getCandidateLimit());
        CenterCandidateScore bestCandidate = null;

        for (int index = 0; index < candidates.size(); index++) {
            if (options.isCancelled())
                return CenterSearchResult.cancelled(bestCandidate, elapsedMillis(startMillis, options));
            if (isHardTimeLimited(startMillis, options))
                return CenterSearchResult.timeLimited(bestCandidate, elapsedMillis(startMillis, options));

            MatchCenter candidate = candidates.get(index);
            CandidateEvaluation evaluation = evaluateCandidate(candidate, index + 1, candidates.size(), startMillis, options);

            if (evaluation.isCancelled())
                return CenterSearchResult.cancelled(bestCandidate, elapsedMillis(startMillis, options));
            if (evaluation.isTimeLimited())
                return CenterSearchResult.timeLimited(bestCandidate, elapsedMillis(startMillis, options));

            CenterCandidateScore score = evaluation.getScore();
            bestCandidate = betterOf(bestCandidate, score);
            options.progressListener.onCandidateScored(score, bestCandidate, index + 1, candidates.size());

            if (elapsedMillis(startMillis, options) >= options.getSoftTimeLimitMillis())
                return CenterSearchResult.timeLimited(bestCandidate, elapsedMillis(startMillis, options));
        }

        return CenterSearchResult.completed(bestCandidate, elapsedMillis(startMillis, options));
    }

    private CandidateEvaluation evaluateCandidate(MatchCenter center, int candidateIndex, int candidateCount, long startMillis, Options options) {
        SampleStage coarseStage = sampleStage(center, CenterValidationStage.COARSE_SCAN, CenterSamplePlanner.coarseSamples(center),
                candidateIndex, candidateCount, startMillis, options);
        if (coarseStage.isStopped())
            return CandidateEvaluation.stopped(coarseStage);

        SampleStage detailedStage = sampleStage(center, CenterValidationStage.DETAILED_SCAN, CenterSamplePlanner.detailedSamples(center),
                candidateIndex, candidateCount, startMillis, options);
        if (detailedStage.isStopped())
            return CandidateEvaluation.stopped(detailedStage);

        SampleStage centerStage = sampleStage(center, CenterValidationStage.CENTER_SCAN, CenterSamplePlanner.centerRefinementSamples(center),
                candidateIndex, candidateCount, startMillis, options);
        if (centerStage.isStopped())
            return CandidateEvaluation.stopped(centerStage);

        return CandidateEvaluation.completed(scoreCandidate(center, detailedStage.getSamples(), centerStage.getSamples()));
    }

    private SampleStage sampleStage(
            MatchCenter center,
            CenterValidationStage stage,
            List<CenterSamplePoint> points,
            int candidateIndex,
            int candidateCount,
            long startMillis,
            Options options
    ) {
        options.progressListener.onStage(center, stage, candidateIndex, candidateCount);
        List<CenterTerrainSample> samples = new ArrayList<>(points.size());

        for (CenterSamplePoint point : points) {
            if (options.isCancelled())
                return SampleStage.cancelled(samples);
            if (isHardTimeLimited(startMillis, options))
                return SampleStage.timeLimited(samples);

            samples.add(sampleReader.sample(point));
        }

        return SampleStage.completed(samples);
    }

    public static CenterCandidateScore scoreCandidate(MatchCenter center, List<CenterTerrainSample> detailedSamples, List<CenterTerrainSample> centerSamples) {
        TerrainRatios detailed = TerrainRatios.from(detailedSamples);
        TerrainRatios centerRatios = TerrainRatios.from(centerSamples);
        SectionStats sections = SectionStats.from(center, detailedSamples);

        return CenterCandidateScore.builder(center)
                .oceanRatio(detailed.getOceanRatio())
                .riverRatio(detailed.getRiverRatio())
                .waterRatio(detailed.getWaterRatio())
                .forestRatio(detailed.getForestRatio())
                .denseForestRatio(detailed.getDenseForestRatio())
                .highlandRatio(detailed.getHighlandRatio())
                .extremeHighlandRatio(detailed.getExtremeHighlandRatio())
                .maxSectionWaterRatio(sections.getMaxSectionWaterRatio())
                .maxAdjacentSectionWaterRatio(sections.getMaxAdjacentSectionWaterRatio())
                .maxSectionLargeWaterRatio(sections.getMaxSectionLargeWaterRatio())
                .maxAdjacentSectionLargeWaterRatio(sections.getMaxAdjacentSectionLargeWaterRatio())
                .centerWaterRatio(centerRatios.getWaterRatio())
                .centerLargeWaterRatio(centerRatios.getLargeWaterRatio())
                .centerStandableRatio(centerRatios.getStandableRatio())
                .centerCliffRatio(centerRatios.getCliffRatio())
                .centerHeightSpread(heightSpread(centerSamples))
                .lowSectionCount(sections.getLowSectionCount())
                .build();
    }

    private static int heightSpread(List<CenterTerrainSample> samples) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (CenterTerrainSample sample : samples) {
            min = Math.min(min, sample.getSurfaceY());
            max = Math.max(max, sample.getSurfaceY());
        }

        return max - min;
    }

    private static CenterCandidateScore betterOf(CenterCandidateScore currentBest, CenterCandidateScore next) {
        if (currentBest == null)
            return next;

        return next.getTotalScore() > currentBest.getTotalScore() ? next : currentBest;
    }

    private static List<MatchCenter> candidates(int initialBorderSize, int candidateLimit) {
        List<MatchCenter> allCandidates = CenterCandidateGenerator.withSecondLayer(initialBorderSize);
        int limit = Math.min(candidateLimit, allCandidates.size());
        return allCandidates.subList(0, limit);
    }

    private static boolean isHardTimeLimited(long startMillis, Options options) {
        return elapsedMillis(startMillis, options) >= options.getHardTimeLimitMillis();
    }

    private static long elapsedMillis(long startMillis, Options options) {
        return Math.max(0L, options.now() - startMillis);
    }

    public enum CenterValidationStage {
        COARSE_SCAN,
        DETAILED_SCAN,
        CENTER_SCAN
    }

    public interface Cancellation {
        boolean isCancelled();
    }

    public interface ProgressListener {
        void onStage(MatchCenter center, CenterValidationStage stage, int candidateIndex, int candidateCount);

        void onCandidateScored(CenterCandidateScore score, CenterCandidateScore bestScore, int candidateIndex, int candidateCount);
    }

    public static final class Options {
        private static final Cancellation NEVER_CANCELLED = () -> false;
        private static final ProgressListener NO_PROGRESS = new ProgressListener() {
            @Override
            public void onStage(MatchCenter center, CenterValidationStage stage, int candidateIndex, int candidateCount) {
            }

            @Override
            public void onCandidateScored(CenterCandidateScore score, CenterCandidateScore bestScore, int candidateIndex, int candidateCount) {
            }
        };

        private final long softTimeLimitMillis;
        private final long hardTimeLimitMillis;
        private final int candidateLimit;
        private final Cancellation cancellation;
        private final ProgressListener progressListener;
        private final LongSupplier timeSource;

        private Options(Builder builder) {
            if (builder.softTimeLimitMillis < 0L)
                throw new IllegalArgumentException("softTimeLimitMillis cannot be negative.");
            if (builder.hardTimeLimitMillis < 0L)
                throw new IllegalArgumentException("hardTimeLimitMillis cannot be negative.");
            if (builder.softTimeLimitMillis > builder.hardTimeLimitMillis)
                throw new IllegalArgumentException("softTimeLimitMillis cannot exceed hardTimeLimitMillis.");
            if (builder.candidateLimit <= 0 || builder.candidateLimit > MAX_CANDIDATES)
                throw new IllegalArgumentException("candidateLimit must be between 1 and " + MAX_CANDIDATES + ".");

            this.softTimeLimitMillis = builder.softTimeLimitMillis;
            this.hardTimeLimitMillis = builder.hardTimeLimitMillis;
            this.candidateLimit = builder.candidateLimit;
            this.cancellation = builder.cancellation == null ? NEVER_CANCELLED : builder.cancellation;
            this.progressListener = builder.progressListener == null ? NO_PROGRESS : builder.progressListener;
            this.timeSource = builder.timeSource == null ? System::currentTimeMillis : builder.timeSource;
        }

        public static Options defaults() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

        long getSoftTimeLimitMillis() {
            return softTimeLimitMillis;
        }

        long getHardTimeLimitMillis() {
            return hardTimeLimitMillis;
        }

        int getCandidateLimit() {
            return candidateLimit;
        }

        boolean isCancelled() {
            return cancellation.isCancelled();
        }

        long now() {
            return timeSource.getAsLong();
        }

        public static final class Builder {
            private long softTimeLimitMillis = DEFAULT_SOFT_TIME_LIMIT_MILLIS;
            private long hardTimeLimitMillis = DEFAULT_HARD_TIME_LIMIT_MILLIS;
            private int candidateLimit = MAX_CANDIDATES;
            private Cancellation cancellation;
            private ProgressListener progressListener;
            private LongSupplier timeSource;

            public Builder softTimeLimitMillis(long softTimeLimitMillis) {
                this.softTimeLimitMillis = softTimeLimitMillis;
                return this;
            }

            public Builder hardTimeLimitMillis(long hardTimeLimitMillis) {
                this.hardTimeLimitMillis = hardTimeLimitMillis;
                return this;
            }

            public Builder candidateLimit(int candidateLimit) {
                this.candidateLimit = candidateLimit;
                return this;
            }

            public Builder cancellation(Cancellation cancellation) {
                this.cancellation = cancellation;
                return this;
            }

            public Builder progressListener(ProgressListener progressListener) {
                this.progressListener = progressListener;
                return this;
            }

            Builder timeSource(LongSupplier timeSource) {
                this.timeSource = timeSource;
                return this;
            }

            public Options build() {
                return new Options(this);
            }
        }
    }

    private static final class CandidateEvaluation {
        private final CenterCandidateScore score;
        private final boolean cancelled;
        private final boolean timeLimited;

        private CandidateEvaluation(CenterCandidateScore score, boolean cancelled, boolean timeLimited) {
            this.score = score;
            this.cancelled = cancelled;
            this.timeLimited = timeLimited;
        }

        static CandidateEvaluation completed(CenterCandidateScore score) {
            return new CandidateEvaluation(score, false, false);
        }

        static CandidateEvaluation stopped(SampleStage stage) {
            return new CandidateEvaluation(null, stage.isCancelled(), stage.isTimeLimited());
        }

        CenterCandidateScore getScore() {
            return score;
        }

        boolean isCancelled() {
            return cancelled;
        }

        boolean isTimeLimited() {
            return timeLimited;
        }
    }

    private static final class SampleStage {
        private final List<CenterTerrainSample> samples;
        private final boolean cancelled;
        private final boolean timeLimited;

        private SampleStage(List<CenterTerrainSample> samples, boolean cancelled, boolean timeLimited) {
            this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
            this.cancelled = cancelled;
            this.timeLimited = timeLimited;
        }

        static SampleStage completed(List<CenterTerrainSample> samples) {
            return new SampleStage(samples, false, false);
        }

        static SampleStage cancelled(List<CenterTerrainSample> samples) {
            return new SampleStage(samples, true, false);
        }

        static SampleStage timeLimited(List<CenterTerrainSample> samples) {
            return new SampleStage(samples, false, true);
        }

        List<CenterTerrainSample> getSamples() {
            return samples;
        }

        boolean isStopped() {
            return cancelled || timeLimited;
        }

        boolean isCancelled() {
            return cancelled;
        }

        boolean isTimeLimited() {
            return timeLimited;
        }
    }

    private static final class TerrainRatios {
        private final int sampleCount;
        private final int oceanCount;
        private final int riverCount;
        private final int waterCount;
        private final int forestCount;
        private final int denseForestCount;
        private final int highlandCount;
        private final int extremeHighlandCount;
        private final int standableCount;
        private final int cliffCount;
        private final double largeWaterRisk;

        private TerrainRatios(
                int sampleCount,
                int oceanCount,
                int riverCount,
                int waterCount,
                int forestCount,
                int denseForestCount,
                int highlandCount,
                int extremeHighlandCount,
                int standableCount,
                int cliffCount,
                double largeWaterRisk
        ) {
            this.sampleCount = sampleCount;
            this.oceanCount = oceanCount;
            this.riverCount = riverCount;
            this.waterCount = waterCount;
            this.forestCount = forestCount;
            this.denseForestCount = denseForestCount;
            this.highlandCount = highlandCount;
            this.extremeHighlandCount = extremeHighlandCount;
            this.standableCount = standableCount;
            this.cliffCount = cliffCount;
            this.largeWaterRisk = largeWaterRisk;
        }

        static TerrainRatios from(List<CenterTerrainSample> samples) {
            if (samples == null || samples.isEmpty())
                throw new IllegalArgumentException("samples cannot be empty.");

            int ocean = 0;
            int river = 0;
            int water = 0;
            int forest = 0;
            int denseForest = 0;
            int highland = 0;
            int extremeHighland = 0;
            int standable = 0;
            int cliff = 0;
            double largeWaterRisk = 0D;

            for (CenterTerrainSample sample : samples) {
                boolean oceanSample = CenterBiomeClassifier.isOcean(sample.getBiomeKey());
                boolean riverSample = CenterBiomeClassifier.isWaterLike(sample.getBiomeKey());
                boolean waterSample = sample.isWaterSurface() || oceanSample || riverSample;
                boolean mountainSample = sample.isHighland() || CenterBiomeClassifier.isMountainHint(sample.getBiomeKey());
                largeWaterRisk += largeWaterRisk(sample, oceanSample, riverSample);

                if (oceanSample)
                    ocean++;
                if (riverSample)
                    river++;
                if (waterSample)
                    water++;
                if (CenterBiomeClassifier.isForest(sample.getBiomeKey()))
                    forest++;
                if (CenterBiomeClassifier.isDenseForest(sample.getBiomeKey()))
                    denseForest++;
                if (mountainSample)
                    highland++;
                if (sample.isExtremeHighland())
                    extremeHighland++;
                if (sample.isStandable())
                    standable++;
                if (sample.isCliff())
                    cliff++;
            }

            return new TerrainRatios(samples.size(), ocean, river, water, forest, denseForest, highland, extremeHighland, standable, cliff, largeWaterRisk);
        }

        double getOceanRatio() {
            return ratio(oceanCount);
        }

        double getRiverRatio() {
            return ratio(riverCount);
        }

        double getWaterRatio() {
            return ratio(waterCount);
        }

        double getLargeWaterRatio() {
            return largeWaterRisk / sampleCount;
        }

        double getForestRatio() {
            return ratio(forestCount);
        }

        double getDenseForestRatio() {
            return ratio(denseForestCount);
        }

        double getHighlandRatio() {
            return ratio(highlandCount);
        }

        double getExtremeHighlandRatio() {
            return ratio(extremeHighlandCount);
        }

        double getStandableRatio() {
            return ratio(standableCount);
        }

        double getCliffRatio() {
            return ratio(cliffCount);
        }

        private double ratio(int value) {
            return value / (double) sampleCount;
        }

        private static double largeWaterRisk(CenterTerrainSample sample, boolean oceanSample, boolean riverSample) {
            if (oceanSample)
                return 1D;
            if (sample.isWaterSurface())
                return 0.45D;
            if (riverSample)
                return 0.25D;

            return 0D;
        }
    }

    private static final class SectionStats {
        private static final int SECTION_COUNT = 9;
        private static final double LOW_SECTION_WATER_RATIO = 0.45D;

        private final double maxSectionWaterRatio;
        private final double maxAdjacentSectionWaterRatio;
        private final double maxSectionLargeWaterRatio;
        private final double maxAdjacentSectionLargeWaterRatio;
        private final int lowSectionCount;

        private SectionStats(
                double maxSectionWaterRatio,
                double maxAdjacentSectionWaterRatio,
                double maxSectionLargeWaterRatio,
                double maxAdjacentSectionLargeWaterRatio,
                int lowSectionCount
        ) {
            this.maxSectionWaterRatio = maxSectionWaterRatio;
            this.maxAdjacentSectionWaterRatio = maxAdjacentSectionWaterRatio;
            this.maxSectionLargeWaterRatio = maxSectionLargeWaterRatio;
            this.maxAdjacentSectionLargeWaterRatio = maxAdjacentSectionLargeWaterRatio;
            this.lowSectionCount = lowSectionCount;
        }

        static SectionStats from(MatchCenter center, List<CenterTerrainSample> samples) {
            int[] totals = new int[SECTION_COUNT];
            int[] water = new int[SECTION_COUNT];
            double[] largeWaterRisk = new double[SECTION_COUNT];

            for (CenterTerrainSample sample : samples) {
                int index = sectionIndex(center, sample.getPoint());
                totals[index]++;

                if (isWater(sample))
                    water[index]++;
                largeWaterRisk[index] += largeWaterRisk(sample);
            }

            double maxSectionWaterRatio = 0D;
            double maxSectionLargeWaterRatio = 0D;
            int lowSections = 0;

            for (int index = 0; index < SECTION_COUNT; index++) {
                double sectionWaterRatio = ratio(water[index], totals[index]);
                double sectionLargeWaterRatio = ratio(largeWaterRisk[index], totals[index]);
                maxSectionWaterRatio = Math.max(maxSectionWaterRatio, sectionWaterRatio);
                maxSectionLargeWaterRatio = Math.max(maxSectionLargeWaterRatio, sectionLargeWaterRatio);

                if (sectionLargeWaterRatio > LOW_SECTION_WATER_RATIO)
                    lowSections++;
            }

            return new SectionStats(
                    maxSectionWaterRatio,
                    maxAdjacentWaterRatio(water, totals),
                    maxSectionLargeWaterRatio,
                    maxAdjacentLargeWaterRatio(largeWaterRisk, totals),
                    lowSections
            );
        }

        double getMaxSectionWaterRatio() {
            return maxSectionWaterRatio;
        }

        double getMaxAdjacentSectionWaterRatio() {
            return maxAdjacentSectionWaterRatio;
        }

        double getMaxSectionLargeWaterRatio() {
            return maxSectionLargeWaterRatio;
        }

        double getMaxAdjacentSectionLargeWaterRatio() {
            return maxAdjacentSectionLargeWaterRatio;
        }

        int getLowSectionCount() {
            return lowSectionCount;
        }

        private static int sectionIndex(MatchCenter center, CenterSamplePoint point) {
            int xBucket = bucket(point.getX(), center.getX(), center.getBorderSize());
            int zBucket = bucket(point.getZ(), center.getZ(), center.getBorderSize());
            return (zBucket * 3) + xBucket;
        }

        private static int bucket(int coordinate, int centerCoordinate, int borderSize) {
            double thirdRadius = (borderSize / 2D) / 3D;

            if (coordinate < centerCoordinate - thirdRadius)
                return 0;
            if (coordinate > centerCoordinate + thirdRadius)
                return 2;

            return 1;
        }

        private static double maxAdjacentWaterRatio(int[] water, int[] totals) {
            double max = 0D;

            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    int index = (z * 3) + x;

                    if (x < 2)
                        max = Math.max(max, ratio(water[index] + water[index + 1], totals[index] + totals[index + 1]));
                    if (z < 2)
                        max = Math.max(max, ratio(water[index] + water[index + 3], totals[index] + totals[index + 3]));
                }
            }

            return max;
        }

        private static double maxAdjacentLargeWaterRatio(double[] largeWaterRisk, int[] totals) {
            double max = 0D;

            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    int index = (z * 3) + x;

                    if (x < 2)
                        max = Math.max(max, ratio(largeWaterRisk[index] + largeWaterRisk[index + 1], totals[index] + totals[index + 1]));
                    if (z < 2)
                        max = Math.max(max, ratio(largeWaterRisk[index] + largeWaterRisk[index + 3], totals[index] + totals[index + 3]));
                }
            }

            return max;
        }

        private static boolean isWater(CenterTerrainSample sample) {
            return sample.isWaterSurface()
                    || CenterBiomeClassifier.isOcean(sample.getBiomeKey())
                    || CenterBiomeClassifier.isWaterLike(sample.getBiomeKey());
        }

        private static double largeWaterRisk(CenterTerrainSample sample) {
            if (CenterBiomeClassifier.isOcean(sample.getBiomeKey()))
                return 1D;
            if (sample.isWaterSurface())
                return 0.45D;
            if (CenterBiomeClassifier.isWaterLike(sample.getBiomeKey()))
                return 0.25D;

            return 0D;
        }

        private static double ratio(int value, int total) {
            if (total <= 0)
                return 0D;

            return value / (double) total;
        }

        private static double ratio(double value, int total) {
            if (total <= 0)
                return 0D;

            return value / total;
        }
    }
}
