package org.mcwonderland.uhc.application.world;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CenterBiomeClassifier {
    private static final Set<String> OCEAN = keys(
            "ocean",
            "deep_ocean",
            "warm_ocean",
            "lukewarm_ocean",
            "cold_ocean",
            "frozen_ocean",
            "deep_warm_ocean",
            "deep_lukewarm_ocean",
            "deep_cold_ocean",
            "deep_frozen_ocean"
    );

    private static final Set<String> WATER_LIKE = keys(
            "river",
            "frozen_river",
            "swamp",
            "mangrove_swamp"
    );

    private static final Set<String> FOREST = keys(
            "forest",
            "birch_forest",
            "flower_forest",
            "taiga",
            "cherry_grove"
    );

    private static final Set<String> DENSE_FOREST = keys(
            "dark_forest",
            "jungle",
            "bamboo_jungle",
            "old_growth_birch_forest",
            "old_growth_pine_taiga",
            "old_growth_spruce_taiga"
    );

    private static final Set<String> MOUNTAIN_HINT = keys(
            "windswept_hills",
            "windswept_forest",
            "windswept_gravelly_hills",
            "stony_peaks",
            "jagged_peaks",
            "frozen_peaks",
            "snowy_slopes"
    );

    private CenterBiomeClassifier() {
    }

    public static boolean isOcean(String biomeKey) {
        return OCEAN.contains(normalize(biomeKey));
    }

    public static boolean isWaterLike(String biomeKey) {
        return WATER_LIKE.contains(normalize(biomeKey));
    }

    public static boolean isForest(String biomeKey) {
        return FOREST.contains(normalize(biomeKey));
    }

    public static boolean isDenseForest(String biomeKey) {
        return DENSE_FOREST.contains(normalize(biomeKey));
    }

    public static boolean isMountainHint(String biomeKey) {
        return MOUNTAIN_HINT.contains(normalize(biomeKey));
    }

    private static Set<String> keys(String... keys) {
        return new HashSet<>(Arrays.asList(keys));
    }

    private static String normalize(String biomeKey) {
        if (biomeKey == null)
            return "";

        String normalized = biomeKey.trim().toLowerCase(Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');

        if (namespaceSeparator >= 0)
            return normalized.substring(namespaceSeparator + 1);

        return normalized;
    }
}
