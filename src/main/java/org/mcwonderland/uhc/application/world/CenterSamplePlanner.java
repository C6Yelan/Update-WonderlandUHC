package org.mcwonderland.uhc.application.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CenterSamplePlanner {
    private static final int COARSE_GRID_SIZE = 10;
    private static final int DETAILED_GRID_SIZE = 17;
    private static final int CENTER_GRID_SIZE = 11;
    private static final int RUNTIME_COARSE_GRID_SIZE = 5;
    private static final int RUNTIME_DETAILED_GRID_SIZE = 7;
    private static final int RUNTIME_CENTER_GRID_SIZE = 5;
    private static final int MIN_CENTER_RADIUS = 96;
    private static final int MAX_CENTER_RADIUS = 192;

    private CenterSamplePlanner() {
    }

    public static List<CenterSamplePoint> coarseSamples(MatchCenter center) {
        requireCenter(center);
        return squareCellSamples(center, COARSE_GRID_SIZE, center.getBorderSize() / 2D);
    }

    public static List<CenterSamplePoint> detailedSamples(MatchCenter center) {
        requireCenter(center);
        return squareCellSamples(center, DETAILED_GRID_SIZE, center.getBorderSize() / 2D);
    }

    public static List<CenterSamplePoint> centerRefinementSamples(MatchCenter center) {
        requireCenter(center);
        return squareInclusiveSamples(center, CENTER_GRID_SIZE, centerRadius(center.getBorderSize()));
    }

    public static List<CenterSamplePoint> runtimeCoarseSamples(MatchCenter center) {
        requireCenter(center);
        return squareCellSamples(center, RUNTIME_COARSE_GRID_SIZE, center.getBorderSize() / 2D);
    }

    public static List<CenterSamplePoint> runtimeDetailedSamples(MatchCenter center) {
        requireCenter(center);
        return squareCellSamples(center, RUNTIME_DETAILED_GRID_SIZE, center.getBorderSize() / 2D);
    }

    public static List<CenterSamplePoint> runtimeCenterRefinementSamples(MatchCenter center) {
        requireCenter(center);
        return squareInclusiveSamples(center, RUNTIME_CENTER_GRID_SIZE, centerRadius(center.getBorderSize()));
    }

    public static int centerRadius(int initialBorderSize) {
        if (initialBorderSize <= 0)
            throw new IllegalArgumentException("initialBorderSize must be positive.");

        double radius = initialBorderSize * 0.06D;
        return (int) Math.round(Math.min(MAX_CENTER_RADIUS, Math.max(MIN_CENTER_RADIUS, radius)));
    }

    private static List<CenterSamplePoint> squareCellSamples(MatchCenter center, int gridSize, double radius) {
        requireCenter(center);

        double step = (radius * 2D) / gridSize;
        List<CenterSamplePoint> points = new ArrayList<>(gridSize * gridSize);

        for (int xIndex = 0; xIndex < gridSize; xIndex++) {
            int x = (int) Math.round(center.getX() - radius + (step / 2D) + (step * xIndex));

            for (int zIndex = 0; zIndex < gridSize; zIndex++) {
                int z = (int) Math.round(center.getZ() - radius + (step / 2D) + (step * zIndex));
                points.add(new CenterSamplePoint(x, z));
            }
        }

        return Collections.unmodifiableList(points);
    }

    private static List<CenterSamplePoint> squareInclusiveSamples(MatchCenter center, int gridSize, int radius) {
        requireCenter(center);

        double step = (radius * 2D) / (gridSize - 1D);
        List<CenterSamplePoint> points = new ArrayList<>(gridSize * gridSize);

        for (int xIndex = 0; xIndex < gridSize; xIndex++) {
            int x = (int) Math.round(center.getX() - radius + (step * xIndex));

            for (int zIndex = 0; zIndex < gridSize; zIndex++) {
                int z = (int) Math.round(center.getZ() - radius + (step * zIndex));
                points.add(new CenterSamplePoint(x, z));
            }
        }

        return Collections.unmodifiableList(points);
    }

    private static void requireCenter(MatchCenter center) {
        if (center == null)
            throw new IllegalArgumentException("center cannot be null.");
    }
}
