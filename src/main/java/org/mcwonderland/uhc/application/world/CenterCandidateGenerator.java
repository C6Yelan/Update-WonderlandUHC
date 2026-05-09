package org.mcwonderland.uhc.application.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CenterCandidateGenerator {
    private static final int CHUNK_SIZE = 16;
    private static final int MIN_OFFSET = 384;
    private static final int MAX_OFFSET = 2000;

    private CenterCandidateGenerator() {
    }

    public static List<MatchCenter> firstLayer(int initialBorderSize) {
        return firstLayer(new MatchCenter(0, 0, initialBorderSize));
    }

    public static List<MatchCenter> firstLayer(MatchCenter origin) {
        requireOrigin(origin);

        int initialBorderSize = origin.getBorderSize();
        int originX = roundToChunk(origin.getX());
        int originZ = roundToChunk(origin.getZ());
        int offset = offset(initialBorderSize);
        List<MatchCenter> centers = new ArrayList<>(9);

        centers.add(new MatchCenter(originX, originZ, initialBorderSize));
        centers.add(new MatchCenter(originX + offset, originZ, initialBorderSize));
        centers.add(new MatchCenter(originX - offset, originZ, initialBorderSize));
        centers.add(new MatchCenter(originX, originZ + offset, initialBorderSize));
        centers.add(new MatchCenter(originX, originZ - offset, initialBorderSize));
        centers.add(new MatchCenter(originX + offset, originZ + offset, initialBorderSize));
        centers.add(new MatchCenter(originX + offset, originZ - offset, initialBorderSize));
        centers.add(new MatchCenter(originX - offset, originZ + offset, initialBorderSize));
        centers.add(new MatchCenter(originX - offset, originZ - offset, initialBorderSize));

        return Collections.unmodifiableList(centers);
    }

    public static List<MatchCenter> withSecondLayer(int initialBorderSize) {
        int offset = offset(initialBorderSize);
        int farOffset = offset * 2;
        List<MatchCenter> centers = new ArrayList<>(25);

        centers.addAll(firstLayer(initialBorderSize));
        centers.add(new MatchCenter(farOffset, 0, initialBorderSize));
        centers.add(new MatchCenter(-farOffset, 0, initialBorderSize));
        centers.add(new MatchCenter(0, farOffset, initialBorderSize));
        centers.add(new MatchCenter(0, -farOffset, initialBorderSize));
        centers.add(new MatchCenter(farOffset, farOffset, initialBorderSize));
        centers.add(new MatchCenter(farOffset, -farOffset, initialBorderSize));
        centers.add(new MatchCenter(-farOffset, farOffset, initialBorderSize));
        centers.add(new MatchCenter(-farOffset, -farOffset, initialBorderSize));
        centers.add(new MatchCenter(farOffset, offset, initialBorderSize));
        centers.add(new MatchCenter(farOffset, -offset, initialBorderSize));
        centers.add(new MatchCenter(-farOffset, offset, initialBorderSize));
        centers.add(new MatchCenter(-farOffset, -offset, initialBorderSize));
        centers.add(new MatchCenter(offset, farOffset, initialBorderSize));
        centers.add(new MatchCenter(-offset, farOffset, initialBorderSize));
        centers.add(new MatchCenter(offset, -farOffset, initialBorderSize));
        centers.add(new MatchCenter(-offset, -farOffset, initialBorderSize));

        return Collections.unmodifiableList(centers);
    }

    public static List<MatchCenter> expandingLandSearch(MatchCenter origin) {
        return expandingLandSearch(origin, 25);
    }

    public static List<MatchCenter> expandingLandSearch(MatchCenter origin, int candidateLimit) {
        requireOrigin(origin);
        if (candidateLimit <= 0)
            throw new IllegalArgumentException("candidateLimit must be positive.");

        List<MatchCenter> centers = new ArrayList<>(candidateLimit);
        int originX = roundToChunk(origin.getX());
        int originZ = roundToChunk(origin.getZ());

        centers.add(new MatchCenter(originX, originZ, origin.getBorderSize()));

        for (int distance = 256; centers.size() < candidateLimit; distance += 256)
            addRing(centers, originX, originZ, origin.getBorderSize(), distance, candidateLimit);

        return Collections.unmodifiableList(centers);
    }

    public static int offset(int initialBorderSize) {
        if (initialBorderSize <= 0)
            throw new IllegalArgumentException("initialBorderSize must be positive.");

        double radius = initialBorderSize / 2D;
        double rawOffset = Math.min(MAX_OFFSET, Math.max(MIN_OFFSET, radius * 0.75D));
        return roundToChunk(rawOffset);
    }

    private static int roundToChunk(double value) {
        return (int) Math.round(value / CHUNK_SIZE) * CHUNK_SIZE;
    }

    private static void addRing(List<MatchCenter> centers, int originX, int originZ, int borderSize, int distance, int candidateLimit) {
        int offset = roundToChunk(distance);

        addCenter(centers, new MatchCenter(originX + offset, originZ, borderSize), candidateLimit);
        addCenter(centers, new MatchCenter(originX - offset, originZ, borderSize), candidateLimit);
        addCenter(centers, new MatchCenter(originX, originZ + offset, borderSize), candidateLimit);
        addCenter(centers, new MatchCenter(originX, originZ - offset, borderSize), candidateLimit);
        addCenter(centers, new MatchCenter(originX + offset, originZ + offset, borderSize), candidateLimit);
        addCenter(centers, new MatchCenter(originX + offset, originZ - offset, borderSize), candidateLimit);
        addCenter(centers, new MatchCenter(originX - offset, originZ + offset, borderSize), candidateLimit);
        addCenter(centers, new MatchCenter(originX - offset, originZ - offset, borderSize), candidateLimit);
    }

    private static void addCenter(List<MatchCenter> centers, MatchCenter center, int candidateLimit) {
        if (centers.size() < candidateLimit)
            centers.add(center);
    }

    private static void requireOrigin(MatchCenter origin) {
        if (origin == null)
            throw new IllegalArgumentException("origin cannot be null.");
    }
}
