package org.mcwonderland.uhc.application.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CenterCandidateScore {
    private static final double TARGET_SCORE = 75D;
    private static final double ACCEPTABLE_SCORE = 60D;
    private static final double POOR_SCORE = 50D;
    private static final double WATER_WEIGHT = 0.22D;
    private static final double TERRAIN_WEIGHT = 0.24D;
    private static final double SECTION_BALANCE_WEIGHT = 0.18D;
    private static final double CENTER_WEIGHT = 0.22D;
    private static final double FOREST_WEIGHT = 0.14D;

    private final MatchCenter center;
    private final double totalScore;
    private final CenterSearchStatus status;
    private final double oceanRatio;
    private final double riverRatio;
    private final double waterRatio;
    private final double forestRatio;
    private final double denseForestRatio;
    private final double highlandRatio;
    private final double extremeHighlandRatio;
    private final double maxSectionWaterRatio;
    private final double maxAdjacentSectionWaterRatio;
    private final double maxSectionLargeWaterRatio;
    private final double maxAdjacentSectionLargeWaterRatio;
    private final double centerWaterRatio;
    private final double centerLargeWaterRatio;
    private final double standableRatio;
    private final double cliffRatio;
    private final int centerHeightSpread;
    private final int lowSectionCount;
    private final double waterScore;
    private final double terrainScore;
    private final double sectionBalanceScore;
    private final double centerScore;
    private final double forestScore;
    private final List<CenterScoreReason> reasons;

    private CenterCandidateScore(Builder builder) {
        this.center = builder.center;
        this.oceanRatio = ratio(builder.oceanRatio);
        this.riverRatio = ratio(builder.riverRatio);
        this.waterRatio = ratio(builder.waterRatio);
        this.forestRatio = ratio(builder.forestRatio);
        this.denseForestRatio = ratio(builder.denseForestRatio);
        this.highlandRatio = ratio(builder.highlandRatio);
        this.extremeHighlandRatio = ratio(builder.extremeHighlandRatio);
        this.maxSectionWaterRatio = ratio(builder.maxSectionWaterRatio);
        this.maxAdjacentSectionWaterRatio = ratio(builder.maxAdjacentSectionWaterRatio);
        this.maxSectionLargeWaterRatio = ratio(builder.maxSectionLargeWaterRatio);
        this.maxAdjacentSectionLargeWaterRatio = ratio(builder.maxAdjacentSectionLargeWaterRatio);
        this.centerWaterRatio = ratio(builder.centerWaterRatio);
        this.centerLargeWaterRatio = ratio(builder.centerLargeWaterRatio);
        this.standableRatio = ratio(builder.centerStandableRatio);
        this.cliffRatio = ratio(builder.centerCliffRatio);
        this.centerHeightSpread = requireNonNegative(builder.centerHeightSpread, "centerHeightSpread");
        this.lowSectionCount = requireNonNegative(builder.lowSectionCount, "lowSectionCount");

        List<CenterScoreReason> mutableReasons = new ArrayList<>();
        boolean hardRejected = addHardReasons(mutableReasons);
        addSoftReasons(mutableReasons);
        this.reasons = Collections.unmodifiableList(mutableReasons);

        this.waterScore = calculateWaterScore();
        this.terrainScore = calculateTerrainScore();
        this.sectionBalanceScore = calculateSectionBalanceScore();
        this.centerScore = calculateCenterScore();
        this.forestScore = calculateForestScore();
        this.totalScore = calculateTotalScore();
        this.status = calculateStatus(hardRejected, !mutableReasons.isEmpty(), totalScore);
    }

    public static Builder builder(MatchCenter center) {
        return new Builder(center);
    }

    public MatchCenter getCenter() {
        return center;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public CenterSearchStatus getStatus() {
        return status;
    }

    public double getOceanRatio() {
        return oceanRatio;
    }

    public double getRiverRatio() {
        return riverRatio;
    }

    public double getWaterRatio() {
        return waterRatio;
    }

    public double getForestRatio() {
        return forestRatio;
    }

    public double getDenseForestRatio() {
        return denseForestRatio;
    }

    public double getHighlandRatio() {
        return highlandRatio;
    }

    public double getExtremeHighlandRatio() {
        return extremeHighlandRatio;
    }

    public double getMaxSectionWaterRatio() {
        return maxSectionWaterRatio;
    }

    public double getMaxAdjacentSectionWaterRatio() {
        return maxAdjacentSectionWaterRatio;
    }

    public double getMaxSectionLargeWaterRatio() {
        return maxSectionLargeWaterRatio;
    }

    public double getMaxAdjacentSectionLargeWaterRatio() {
        return maxAdjacentSectionLargeWaterRatio;
    }

    public double getCenterWaterRatio() {
        return centerWaterRatio;
    }

    public double getCenterLargeWaterRatio() {
        return centerLargeWaterRatio;
    }

    public double getStandableRatio() {
        return standableRatio;
    }

    public double getCliffRatio() {
        return cliffRatio;
    }

    public int getCenterHeightSpread() {
        return centerHeightSpread;
    }

    public int getLowSectionCount() {
        return lowSectionCount;
    }

    public double getWaterScore() {
        return waterScore;
    }

    public double getTerrainScore() {
        return terrainScore;
    }

    public double getSectionBalanceScore() {
        return sectionBalanceScore;
    }

    public double getCenterScore() {
        return centerScore;
    }

    public double getForestScore() {
        return forestScore;
    }

    public List<CenterScoreReason> getReasons() {
        return reasons;
    }

    public boolean isRecommended() {
        return status == CenterSearchStatus.RECOMMENDED;
    }

    private boolean addHardReasons(List<CenterScoreReason> reasons) {
        if (oceanRatio > 0.35D)
            reasons.add(CenterScoreReason.OCEAN_RATIO_TOO_HIGH);
        if (waterRatio > 0.75D)
            reasons.add(CenterScoreReason.WATER_RATIO_TOO_HIGH);
        if (maxSectionLargeWaterRatio > 0.75D)
            reasons.add(CenterScoreReason.SECTION_WATER_TOO_HIGH);
        if (maxAdjacentSectionLargeWaterRatio > 0.70D)
            reasons.add(CenterScoreReason.ADJACENT_SECTION_WATER_TOO_HIGH);
        if (centerLargeWaterRatio > 0.35D)
            reasons.add(CenterScoreReason.CENTER_WATER_TOO_HIGH);
        if (standableRatio < 0.55D)
            reasons.add(CenterScoreReason.CENTER_STANDABLE_RATIO_TOO_LOW);
        if (centerHeightSpread > 110)
            reasons.add(CenterScoreReason.CENTER_HEIGHT_SPREAD_TOO_HIGH);
        if (cliffRatio > 0.60D)
            reasons.add(CenterScoreReason.CENTER_CLIFF_RATIO_TOO_HIGH);
        if (lowSectionCount >= 8)
            reasons.add(CenterScoreReason.TOO_MANY_LOW_SECTIONS);

        return !reasons.isEmpty();
    }

    private void addSoftReasons(List<CenterScoreReason> reasons) {
        if (forestRatio > 0.55D)
            reasons.add(CenterScoreReason.FOREST_RATIO_TOO_HIGH);
        if (denseForestRatio > 0.28D)
            reasons.add(CenterScoreReason.DENSE_FOREST_RATIO_TOO_HIGH);
        if (highlandRatio > 0.42D)
            reasons.add(CenterScoreReason.HIGHLAND_RATIO_TOO_HIGH);
        if (extremeHighlandRatio > 0.15D)
            reasons.add(CenterScoreReason.EXTREME_HIGHLAND_RATIO_TOO_HIGH);
    }

    private double calculateWaterScore() {
        double nonOceanWaterRatio = Math.max(0D, waterRatio - oceanRatio);
        double oceanScore = scoreLowerIsBetter(oceanRatio, 0.08D, 0.20D, 0.35D);
        double smallWaterScore = scoreLowerIsBetter(nonOceanWaterRatio, 0.25D, 0.45D, 0.65D);
        double riverScore = scoreLowerIsBetter(riverRatio, 0.15D, 0.30D, 0.45D);

        return (oceanScore * 0.75D) + (smallWaterScore * 0.20D) + (riverScore * 0.05D);
    }

    private double calculateTerrainScore() {
        return Math.min(
                Math.min(
                        scoreLowerIsBetter(highlandRatio, 0.18D, 0.30D, 0.42D),
                        scoreLowerIsBetter(extremeHighlandRatio, 0.04D, 0.08D, 0.15D)
                ),
                scoreLowerIsBetter(cliffRatio, 0.12D, 0.35D, 0.60D)
        );
    }

    private double calculateSectionBalanceScore() {
        return Math.min(
                scoreLowerIsBetter(lowSectionCount, 2D, 5D, 8D),
                Math.min(
                        scoreLowerIsBetter(maxSectionLargeWaterRatio, 0.35D, 0.55D, 0.75D),
                        scoreLowerIsBetter(maxAdjacentSectionLargeWaterRatio, 0.35D, 0.52D, 0.70D)
                )
        );
    }

    private double calculateCenterScore() {
        return Math.min(
                Math.min(
                        scoreLowerIsBetter(centerLargeWaterRatio, 0.08D, 0.20D, 0.35D),
                        scoreHigherIsBetter(standableRatio, 0.55D, 0.75D)
                ),
                Math.min(
                        scoreLowerIsBetter(centerHeightSpread, 40D, 70D, 110D),
                        scoreLowerIsBetter(cliffRatio, 0.12D, 0.35D, 0.60D)
                )
        );
    }

    private double calculateForestScore() {
        return Math.min(
                scoreRangeIsBest(forestRatio, 0.10D, 0.35D, 0.45D, 0.55D),
                scoreLowerIsBetter(denseForestRatio, 0.10D, 0.18D, 0.28D)
        );
    }

    private double calculateTotalScore() {
        return (waterScore * WATER_WEIGHT)
                + (terrainScore * TERRAIN_WEIGHT)
                + (sectionBalanceScore * SECTION_BALANCE_WEIGHT)
                + (centerScore * CENTER_WEIGHT)
                + (forestScore * FOREST_WEIGHT);
    }

    public static double waterWeight() {
        return WATER_WEIGHT;
    }

    public static double terrainWeight() {
        return TERRAIN_WEIGHT;
    }

    public static double sectionBalanceWeight() {
        return SECTION_BALANCE_WEIGHT;
    }

    public static double centerWeight() {
        return CENTER_WEIGHT;
    }

    public static double forestWeight() {
        return FOREST_WEIGHT;
    }

    private static CenterSearchStatus calculateStatus(boolean hardRejected, boolean hasRecommendationBlocker, double totalScore) {
        if (hardRejected || totalScore < POOR_SCORE)
            return CenterSearchStatus.REJECTED;
        if (!hasRecommendationBlocker && totalScore >= TARGET_SCORE)
            return CenterSearchStatus.RECOMMENDED;
        if (totalScore >= ACCEPTABLE_SCORE)
            return CenterSearchStatus.ACCEPTABLE;

        return CenterSearchStatus.POOR;
    }

    private static double scoreLowerIsBetter(double value, double goodLimit, double acceptableLimit, double badLimit) {
        if (value <= goodLimit)
            return 100D;
        if (value <= acceptableLimit)
            return interpolate(value, goodLimit, acceptableLimit, 100D, 75D);
        if (value <= badLimit)
            return interpolate(value, acceptableLimit, badLimit, 75D, 45D);

        return 0D;
    }

    private static double scoreHigherIsBetter(double value, double badLimit, double goodLimit) {
        if (value >= goodLimit)
            return 100D;
        if (value >= badLimit)
            return interpolate(value, badLimit, goodLimit, 45D, 100D);

        return 0D;
    }

    private static double scoreRangeIsBest(double value, double minGood, double maxGood, double acceptableLimit, double badLimit) {
        if (value >= minGood && value <= maxGood)
            return 100D;
        if (value < minGood)
            return scoreHigherIsBetter(value, 0D, minGood);
        if (value <= acceptableLimit)
            return interpolate(value, maxGood, acceptableLimit, 100D, 75D);
        if (value <= badLimit)
            return interpolate(value, acceptableLimit, badLimit, 75D, 45D);

        return 0D;
    }

    private static double interpolate(double value, double fromValue, double toValue, double fromScore, double toScore) {
        if (fromValue == toValue)
            return toScore;

        double progress = (value - fromValue) / (toValue - fromValue);
        return fromScore + (progress * (toScore - fromScore));
    }

    private static double ratio(double value) {
        if (value < 0D || value > 1D)
            throw new IllegalArgumentException("ratio must be between 0 and 1: " + value);

        return value;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0)
            throw new IllegalArgumentException(name + " cannot be negative.");

        return value;
    }

    public static final class Builder {
        private final MatchCenter center;
        private double oceanRatio;
        private double riverRatio;
        private double waterRatio;
        private double forestRatio;
        private double denseForestRatio;
        private double highlandRatio;
        private double extremeHighlandRatio;
        private double maxSectionWaterRatio;
        private double maxAdjacentSectionWaterRatio;
        private double maxSectionLargeWaterRatio;
        private double maxAdjacentSectionLargeWaterRatio;
        private double centerWaterRatio;
        private double centerLargeWaterRatio;
        private double centerStandableRatio = 1D;
        private double centerCliffRatio;
        private int centerHeightSpread;
        private int lowSectionCount;

        private Builder(MatchCenter center) {
            if (center == null)
                throw new IllegalArgumentException("center cannot be null.");

            this.center = center;
        }

        public Builder oceanRatio(double oceanRatio) {
            this.oceanRatio = oceanRatio;
            return this;
        }

        public Builder riverRatio(double riverRatio) {
            this.riverRatio = riverRatio;
            return this;
        }

        public Builder waterRatio(double waterRatio) {
            this.waterRatio = waterRatio;
            return this;
        }

        public Builder forestRatio(double forestRatio) {
            this.forestRatio = forestRatio;
            return this;
        }

        public Builder denseForestRatio(double denseForestRatio) {
            this.denseForestRatio = denseForestRatio;
            return this;
        }

        public Builder highlandRatio(double highlandRatio) {
            this.highlandRatio = highlandRatio;
            return this;
        }

        public Builder extremeHighlandRatio(double extremeHighlandRatio) {
            this.extremeHighlandRatio = extremeHighlandRatio;
            return this;
        }

        public Builder maxSectionWaterRatio(double maxSectionWaterRatio) {
            this.maxSectionWaterRatio = maxSectionWaterRatio;
            return this;
        }

        public Builder maxAdjacentSectionWaterRatio(double maxAdjacentSectionWaterRatio) {
            this.maxAdjacentSectionWaterRatio = maxAdjacentSectionWaterRatio;
            return this;
        }

        public Builder maxSectionLargeWaterRatio(double maxSectionLargeWaterRatio) {
            this.maxSectionLargeWaterRatio = maxSectionLargeWaterRatio;
            return this;
        }

        public Builder maxAdjacentSectionLargeWaterRatio(double maxAdjacentSectionLargeWaterRatio) {
            this.maxAdjacentSectionLargeWaterRatio = maxAdjacentSectionLargeWaterRatio;
            return this;
        }

        public Builder centerWaterRatio(double centerWaterRatio) {
            this.centerWaterRatio = centerWaterRatio;
            return this;
        }

        public Builder centerLargeWaterRatio(double centerLargeWaterRatio) {
            this.centerLargeWaterRatio = centerLargeWaterRatio;
            return this;
        }

        public Builder centerStandableRatio(double centerStandableRatio) {
            this.centerStandableRatio = centerStandableRatio;
            return this;
        }

        public Builder centerCliffRatio(double centerCliffRatio) {
            this.centerCliffRatio = centerCliffRatio;
            return this;
        }

        public Builder centerHeightSpread(int centerHeightSpread) {
            this.centerHeightSpread = centerHeightSpread;
            return this;
        }

        public Builder lowSectionCount(int lowSectionCount) {
            this.lowSectionCount = lowSectionCount;
            return this;
        }

        public CenterCandidateScore build() {
            return new CenterCandidateScore(this);
        }
    }
}
