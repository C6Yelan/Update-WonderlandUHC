package org.mcwonderland.uhc.game;

import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.application.world.CenterBiomeClassifier;
import org.mcwonderland.uhc.application.world.CenterCandidateGenerator;
import org.mcwonderland.uhc.application.world.CenterCandidateScore;
import org.mcwonderland.uhc.application.world.CenterSamplePlanner;
import org.mcwonderland.uhc.application.world.CenterSamplePoint;
import org.mcwonderland.uhc.application.world.CenterScoreReason;
import org.mcwonderland.uhc.application.world.CenterSearchResult;
import org.mcwonderland.uhc.application.world.CenterSearchStatus;
import org.mcwonderland.uhc.application.world.CenterTerrainSample;
import org.mcwonderland.uhc.application.world.CenterValidationService;
import org.mcwonderland.uhc.application.world.CenterWorldSampleReader;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.UHCWorldUtils;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

public class CenterCleaner {

    public static void createWorld(String worldName, Player player, @Nullable String seed) {
        createWorld(worldName, player, Game.getGame().isCenterCleaner(), seed);
    }

    public static void createWorld(String worldName, Player player, boolean centerCleanerEnabled, @Nullable String seed) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorlds().contains(Bukkit.getWorld(worldName))) {
                    player.teleport(UHCWorldUtils.getLobbySpawn());
                    Bukkit.unloadWorld(worldName, false);
                    Chat.send(player, Messages.Host.WORLD_DELETED);
                }

                Extra.deleteWorld(worldName);

                WorldCreator creator = new WorldCreator(worldName);
                applySeed(creator, seed);
                applyConfiguredGeneratorSettings(creator, centerCleanerEnabled);

                World uhcWorld = creator.createWorld();

                if (centerCleanerEnabled) {
                    startCenterSearch(player, uhcWorld);
                    return;
                }

                Chat.send(player, Messages.Host.WORLD_CREATED.replace("{generator}", "停用"));
                MatchCenter matchCenter = spawnCenter(uhcWorld, initialBorderSize());
                Game.getGame().setMatchCenter(matchCenter);
                saveWorldReadyCache(player);
                BorderUtil.setBorders(uhcWorld, matchCenter.getBorderSize());
                player.teleport(previewLocation(uhcWorld, matchCenter));
                player.setGameMode(GameMode.CREATIVE);
                Extra.sound(player, Sounds.Host.WORLD_CREATED);
            }
        }.runTask(WonderlandUHC.getInstance());
    }

    private static void applySeed(WorldCreator creator, @Nullable String seed) {
        if (seed == null)
            return;

        try {
            creator.seed(Long.parseLong(seed));
        } catch (Exception ex) {
            creator.seed(seed.hashCode());
        }
    }

    private static void applyConfiguredGeneratorSettings(WorldCreator creator, boolean centerCleanerEnabled) {
        String generatorSettings = Settings.CenterCleaner.GENERATOR_SETTINGS;

        if (!centerCleanerEnabled
                && generatorSettings != null
                && !generatorSettings.isEmpty())
            creator.generatorSettings(generatorSettings);
    }

    private static void startCenterSearch(Player player, World world) {
        Chat.send(player, message(Messages.CenterCleaner.SEARCH_STARTED,
                "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <white>正在評估同一張世界中的候選中心...</white>"));
        Location spawn = world.getSpawnLocation();
        Chat.send(player, "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <white>搜尋基準: 世界重生點X:</white><green>" + spawn.getBlockX() + "</green><white>Z:</white><green>" + spawn.getBlockZ()
                + "</green> <gray>候選數</gray><green>" + searchCandidateCount() + "</green>");

        new CenterSearchTask(player, world, initialBorderSize()).runTaskTimer(WonderlandUHC.getInstance(), 1L, 1L);
    }

    private static int initialBorderSize() {
        UHCGameSettings settings = Game.getSettings();

        if (settings == null || settings.getBorderSettings() == null || settings.getBorderSettings().getInitialBorder() == null)
            return 2000;

        return settings.getBorderSettings().getInitialBorder();
    }

    private static MatchCenter spawnCenter(World world, int initialBorderSize) {
        Location spawn = world.getSpawnLocation();
        return new MatchCenter(spawn.getBlockX(), spawn.getBlockZ(), initialBorderSize);
    }

    private static Location previewLocation(World world, MatchCenter center) {
        int y = world.getHighestBlockYAt(center.getX(), center.getZ()) + 2;
        return new Location(world, center.getX() + 0.5D, y, center.getZ() + 0.5D);
    }

    private static void saveWorldReadyCache(Player player) {
        WorldLoadingCacheState.setLoadingStatus(LoadingStatus.WORLD_READY);
        WorldLoadingCacheState.setHost(player.getName());
        WorldLoadingCacheState.saveCache();
    }

    private static boolean previewDuringSearch() {
        return !Boolean.FALSE.equals(Settings.CenterCleaner.PREVIEW_DURING_SEARCH);
    }

    private static boolean debugSearchOutput() {
        return !Boolean.FALSE.equals(Settings.CenterCleaner.DEBUG_SEARCH_OUTPUT);
    }

    private static int searchCandidateCount() {
        Integer candidateCount = Settings.CenterCleaner.SEARCH_CANDIDATE_COUNT;
        return candidateCount == null || candidateCount <= 0 ? 25 : candidateCount;
    }

    private static void sendSearchResult(Player player, CenterSearchResult result) {
        CenterCandidateScore score = result.getBestCandidate();

        if (score == null) {
            Chat.send(player, message(Messages.CenterCleaner.SEARCH_CANCELLED,
                    "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <red>搜尋已取消，沒有可用結果。</red>"));
            return;
        }

        MatchCenter center = score.getCenter();
        Chat.send(player, message(Messages.CenterCleaner.SEARCH_RESULT,
                "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <white>結果: </white><green>{status}</green> <gray>分數: </gray><white>{score}</white> <gray>中心: </gray><white>X {x}, Z {z}</white> <gray>原因: </gray><white>{reasons}</white>")
                .replace("{status}", result.getStatus().name())
                .replace("{score}", formatScore(score.getTotalScore()))
                .replace("{x}", center.getX() + "")
                .replace("{z}", center.getZ() + "")
                .replace("{reasons}", formatReasons(score.getReasons())));

        if (result.getStatus() == CenterSearchStatus.TIME_LIMITED)
            Chat.send(player, message(Messages.CenterCleaner.SEARCH_TIME_LIMITED,
                    "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <yellow>搜尋達到時間限制，已使用目前最佳結果。</yellow>"));

        Chat.send(player, result.shouldPregenerate()
                ? message(Messages.CenterCleaner.SEARCH_RECOMMENDED, "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <green>此中心可作為目前預覽地圖。</green>")
                : message(Messages.CenterCleaner.SEARCH_NOT_RECOMMENDED, "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <yellow>此中心不建議直接跑圖，但已保留目前最佳結果供主持人預覽。</yellow>"));
        Chat.send(player, message(Messages.CenterCleaner.SEARCH_REGEN_HINT,
                "<gray>如果不滿意目前預覽世界，可使用</gray><gold>/uhc regen</gold><gray>重新搜尋，或使用</gray><gold>/uhc regen <seed></gold><gray>手動指定seed。</gray>"));
    }

    private static String formatScore(double score) {
        return String.format(Locale.ROOT, "%.1f", score);
    }

    private static String formatPercent(double ratio) {
        return String.format(Locale.ROOT, "%.1f%%", ratio * 100D);
    }

    private static String formatWeighted(double score, double weight) {
        return formatScore(score) + "x" + formatPercent(weight) + "=" + formatScore(score * weight);
    }

    private static String message(String configuredMessage, String fallback) {
        return configuredMessage == null || configuredMessage.isEmpty() ? fallback : configuredMessage;
    }

    private static String formatReasons(List<CenterScoreReason> reasons) {
        if (reasons.isEmpty())
            return "無";

        StringBuilder builder = new StringBuilder();

        for (CenterScoreReason reason : reasons) {
            if (builder.length() > 0)
                builder.append(", ");

            builder.append(formatReason(reason));
        }

        return builder.toString();
    }

    private static String formatReason(CenterScoreReason reason) {
        switch (reason) {
            case OCEAN_RATIO_TOO_HIGH:
                return "海洋比例過高";
            case WATER_RATIO_TOO_HIGH:
                return "水域比例過高";
            case SECTION_WATER_TOO_HIGH:
                return "局部大片水域過高";
            case ADJACENT_SECTION_WATER_TOO_HIGH:
                return "相鄰區塊大片水域過高";
            case CENTER_WATER_TOO_HIGH:
                return "中心大片水域過高";
            case CENTER_STANDABLE_RATIO_TOO_LOW:
                return "中心可站立比例過低";
            case CENTER_HEIGHT_SPREAD_TOO_HIGH:
                return "中心高度差過大";
            case CENTER_CLIFF_RATIO_TOO_HIGH:
                return "中心斷崖比例過高";
            case TOO_MANY_LOW_SECTIONS:
                return "低品質區塊過多";
            case FOREST_RATIO_TOO_HIGH:
                return "森林比例過高";
            case DENSE_FOREST_RATIO_TOO_HIGH:
                return "密林比例過高";
            case HIGHLAND_RATIO_TOO_HIGH:
                return "高地比例過高";
            case EXTREME_HIGHLAND_RATIO_TOO_HIGH:
                return "極端高地比例過高";
            default:
                return reason.name();
        }
    }

    private static final class CenterCleanerProgress implements CenterValidationService.ProgressListener {
        private final Player player;
        private long lastActionBarMillis;

        private CenterCleanerProgress(Player player) {
            this.player = player;
        }

        @Override
        public void onStage(MatchCenter center, CenterValidationService.CenterValidationStage stage, int candidateIndex, int candidateCount) {
            String progressMessage = progressMessage(candidateIndex, candidateCount, formatStage(stage));
            PluginPlayers.sendActionBar(player, progressMessage);
            Chat.send(player, progressMessage);
        }

        private void onStageProgress(CenterValidationService.CenterValidationStage stage, int candidateIndex, int candidateCount, int sampledPoints, int totalPoints) {
            long now = System.currentTimeMillis();

            if (now - lastActionBarMillis < 1000L)
                return;

            lastActionBarMillis = now;
            PluginPlayers.sendActionBar(player, progressMessage(candidateIndex, candidateCount,
                    formatStage(stage) + " " + sampledPoints + "/" + totalPoints));
        }

        private static String progressMessage(int candidateIndex, int candidateCount, String stageText) {
            return message(Messages.CenterCleaner.SEARCH_PROGRESS,
                    "<gray>中心搜尋: </gray><white>{current}</white><gray>/</gray><white>{total}</white> <dark_gray>-</dark_gray> <green>{stage}</green>")
                    .replace("{current}", candidateIndex + "")
                    .replace("{total}", candidateCount + "")
                    .replace("{stage}", stageText);
        }

        @Override
        public void onCandidateScored(CenterCandidateScore score, CenterCandidateScore bestScore, int candidateIndex, int candidateCount) {
            String progressMessage = progressMessage(candidateIndex, candidateCount, "目前最佳 " + formatScore(bestScore.getTotalScore()));
            PluginPlayers.sendActionBar(player, progressMessage);
            Chat.send(player, progressMessage);
        }

        private static String formatStage(CenterValidationService.CenterValidationStage stage) {
            switch (stage) {
                case COARSE_SCAN:
                    return "粗掃";
                case DETAILED_SCAN:
                    return "詳掃";
                case CENTER_SCAN:
                    return "中心精掃";
                default:
                    return stage.name();
            }
        }
    }

    private static final class CenterSearchTask extends BukkitRunnable {
        private static final int SAMPLES_PER_TICK = 2;

        private final Player player;
        private final World world;
        private final List<MatchCenter> candidates;
        private final CenterWorldSampleReader sampleReader;
        private final CenterCleanerProgress progress;
        private final long startMillis = System.currentTimeMillis();

        private int candidateIndex;
        private CenterValidationService.CenterValidationStage stage;
        private Queue<CenterSamplePoint> stagePoints;
        private int stageTotalPoints;
        private int sampledStagePoints;
        private boolean currentCenterOceanRejected;
        private final List<CenterTerrainSample> detailedSamples = new ArrayList<>();
        private final List<CenterTerrainSample> centerSamples = new ArrayList<>();
        private final Set<Long> sampledChunks = new HashSet<>();
        private final Set<Long> stageSampledChunks = new HashSet<>();
        private CenterCandidateScore bestScore;

        private CenterSearchTask(Player player, World world, int initialBorderSize) {
            this.player = player;
            this.world = world;
            this.candidates = CenterCandidateGenerator.expandingLandSearch(spawnCenter(world, initialBorderSize), searchCandidateCount());
            this.sampleReader = new CenterWorldSampleReader(world);
            this.progress = new CenterCleanerProgress(player);
            startStage(CenterValidationService.CenterValidationStage.CENTER_SCAN);
        }

        private static MatchCenter spawnCenter(World world, int initialBorderSize) {
            return CenterCleaner.spawnCenter(world, initialBorderSize);
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cancel();
                return;
            }

            for (int i = 0; i < SAMPLES_PER_TICK; i++) {
                if (stagePoints.isEmpty()) {
                    advanceStage();

                    if (isCancelled())
                        return;
                }

                if (!stagePoints.isEmpty())
                    sample(stagePoints.poll());
            }
        }

        private void startStage(CenterValidationService.CenterValidationStage nextStage) {
            stage = nextStage;
            currentCenterOceanRejected = false;
            stagePoints = new ArrayDeque<>(pointsForCurrentStage());

            if (stage == CenterValidationService.CenterValidationStage.CENTER_SCAN && currentCenterIsOceanBiome()) {
                currentCenterOceanRejected = true;
                stagePoints.clear();
            }

            stageTotalPoints = stagePoints.size();
            sampledStagePoints = 0;
            progress.onStage(currentCenter(), stage, candidateIndex + 1, candidates.size());
            previewCurrentCandidate();
        }

        private List<CenterSamplePoint> pointsForCurrentStage() {
            switch (stage) {
                case COARSE_SCAN:
                    return CenterSamplePlanner.runtimeCoarseSamples(currentCenter());
                case DETAILED_SCAN:
                    return CenterSamplePlanner.runtimeDetailedSamples(currentCenter());
                case CENTER_SCAN:
                    return CenterSamplePlanner.runtimeCenterRefinementSamples(currentCenter());
                default:
                    throw new IllegalStateException("Unsupported center validation stage: " + stage);
            }
        }

        private void sample(CenterSamplePoint point) {
            rememberChunk(point);
            CenterTerrainSample sample = sampleReader.sample(point);

            if (stage == CenterValidationService.CenterValidationStage.COARSE_SCAN
                    || stage == CenterValidationService.CenterValidationStage.DETAILED_SCAN)
                detailedSamples.add(sample);
            else if (stage == CenterValidationService.CenterValidationStage.CENTER_SCAN)
                centerSamples.add(sample);

            sampledStagePoints++;
            progress.onStageProgress(stage, candidateIndex + 1, candidates.size(), sampledStagePoints, stageTotalPoints);
        }

        private void previewCurrentCandidate() {
            if (stage != CenterValidationService.CenterValidationStage.CENTER_SCAN
                    || currentCenterOceanRejected
                    || !previewDuringSearch())
                return;

            MatchCenter center = currentCenter();
            player.teleport(previewLocation(world, center));
            player.setGameMode(GameMode.CREATIVE);
            Chat.send(player, message(Messages.CenterCleaner.SEARCH_PREVIEW,
                    "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <white>已傳送到候選中心 </white><green>{current}</green><gray>/</gray><green>{total}</green><gray>: </gray><white>X {x}, Z {z}</white>")
                    .replace("{current}", (candidateIndex + 1) + "")
                    .replace("{total}", candidates.size() + "")
                    .replace("{x}", center.getX() + "")
                    .replace("{z}", center.getZ() + ""));
        }

        private void advanceStage() {
            if (stage == CenterValidationService.CenterValidationStage.CENTER_SCAN) {
                if (quickCenterRejected()) {
                    scoreCurrentCandidate(true);
                    nextCandidate();
                    return;
                }

                releaseStageResources();
                startStage(CenterValidationService.CenterValidationStage.COARSE_SCAN);
                return;
            }

            if (stage == CenterValidationService.CenterValidationStage.COARSE_SCAN) {
                releaseStageResources();
                startStage(CenterValidationService.CenterValidationStage.DETAILED_SCAN);
                return;
            }

            scoreCurrentCandidate(false);
            releaseStageResources();
            nextCandidate();
        }

        private void nextCandidate() {
            List<Long> chunksToRelease = new ArrayList<>(sampledChunks);
            candidateIndex++;

            if (candidateIndex >= candidates.size()) {
                sampleReader.clearCache();
                sampledChunks.clear();
                stageSampledChunks.clear();
                finish(CenterSearchResult.completed(bestScore, elapsedMillis()));
                releaseChunks(chunksToRelease);
                return;
            }

            detailedSamples.clear();
            centerSamples.clear();
            sampleReader.clearCache();
            sampledChunks.clear();
            stageSampledChunks.clear();
            startStage(CenterValidationService.CenterValidationStage.CENTER_SCAN);
            releaseChunks(chunksToRelease);
        }

        private boolean quickCenterRejected() {
            return currentCenterOceanRejected;
        }

        private boolean currentCenterIsOceanBiome() {
            CenterSamplePoint centerPoint = new CenterSamplePoint(currentCenter().getX(), currentCenter().getZ());
            rememberChunk(centerPoint);
            CenterTerrainSample sample = sampleReader.sample(centerPoint);

            if (!CenterBiomeClassifier.isOcean(sample.getBiomeKey()))
                return false;

            centerSamples.add(sample);
            return true;
        }

        private void rememberChunk(CenterSamplePoint point) {
            long chunkKey = chunkKey(point.getX() >> 4, point.getZ() >> 4);
            sampledChunks.add(chunkKey);
            stageSampledChunks.add(chunkKey);
        }

        private void releaseStageResources() {
            List<Long> chunksToRelease = new ArrayList<>(stageSampledChunks);
            sampledChunks.removeAll(stageSampledChunks);
            stageSampledChunks.clear();
            sampleReader.clearCache();
            releaseChunks(chunksToRelease);
        }

        private void releaseChunks(List<Long> chunkKeys) {
            for (Long chunkKey : chunkKeys)
                world.unloadChunk(chunkX(chunkKey), chunkZ(chunkKey), false);
        }

        private static long chunkKey(int chunkX, int chunkZ) {
            return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        }

        private static int chunkX(long chunkKey) {
            return (int) (chunkKey >> 32);
        }

        private static int chunkZ(long chunkKey) {
            return (int) chunkKey;
        }

        private void scoreCurrentCandidate(boolean quickRejected) {
            List<CenterTerrainSample> scoreSamples = detailedSamples.isEmpty() ? centerSamples : detailedSamples;
            CenterCandidateScore score = CenterValidationService.scoreCandidate(currentCenter(), scoreSamples, centerSamples);

            if (bestScore == null || score.getTotalScore() > bestScore.getTotalScore())
                bestScore = score;

            progress.onCandidateScored(score, bestScore, candidateIndex + 1, candidates.size());
            sendCandidateDebug(score, quickRejected);
        }

        private void sendCandidateDebug(CenterCandidateScore score, boolean quickRejected) {
            if (!debugSearchOutput())
                return;

            MatchCenter center = score.getCenter();
            Chat.send(player, "<dark_gray><strikethrough>--------------------------------------------------</strikethrough></dark_gray>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>候選 </white><green>" + (candidateIndex + 1) + "</green><gray>/</gray><green>" + candidates.size()
                    + "</green> <gray>X </gray><white>" + center.getX() + "</white> <gray>Z </gray><white>" + center.getZ()
                    + "</white> <gray>狀態 </gray><white>" + score.getStatus().name()
                    + "</white> <gray>總分 </gray><white>" + formatScore(score.getTotalScore()) + "</white>");
            if (quickRejected)
                Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <yellow>正中心點為海洋生態域，已略過外圍詳掃。</yellow>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>加權1 </white><gray>水域 </gray><aqua>" + formatWeighted(score.getWaterScore(), CenterCandidateScore.waterWeight())
                    + "</aqua> <gray>地形 </gray><gold>" + formatWeighted(score.getTerrainScore(), CenterCandidateScore.terrainWeight())
                    + "</gold> <gray>區塊 </gray><yellow>" + formatWeighted(score.getSectionBalanceScore(), CenterCandidateScore.sectionBalanceWeight()) + "</yellow>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>加權2 </white><gray>中心 </gray><green>" + formatWeighted(score.getCenterScore(), CenterCandidateScore.centerWeight())
                    + "</green> <gray>森林 </gray><dark_green>" + formatWeighted(score.getForestScore(), CenterCandidateScore.forestWeight()) + "</dark_green>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>水域 </white><gray>海洋 </gray><white>" + formatPercent(score.getOceanRatio())
                    + "</white> <gray>河流/沼澤 </gray><white>" + formatPercent(score.getRiverRatio())
                    + "</white> <gray>總水域 </gray><white>" + formatPercent(score.getWaterRatio())
                    + "</white> <gray>中心水域 </gray><white>" + formatPercent(score.getCenterWaterRatio())
                    + "</white> <gray>中心大片水域 </gray><white>" + formatPercent(score.getCenterLargeWaterRatio()) + "</white>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>水域分布 </white><gray>單區總水最高 </gray><white>" + formatPercent(score.getMaxSectionWaterRatio())
                    + "</white> <gray>相鄰總水最高 </gray><white>" + formatPercent(score.getMaxAdjacentSectionWaterRatio())
                    + "</white> <gray>單區大片水域 </gray><white>" + formatPercent(score.getMaxSectionLargeWaterRatio())
                    + "</white> <gray>相鄰大片水域 </gray><white>" + formatPercent(score.getMaxAdjacentSectionLargeWaterRatio()) + "</white>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>地形 </white><gray>森林 </gray><white>" + formatPercent(score.getForestRatio())
                    + "</white> <gray>密林 </gray><white>" + formatPercent(score.getDenseForestRatio())
                    + "</white> <gray>高地 </gray><white>" + formatPercent(score.getHighlandRatio())
                    + "</white> <gray>極端高地 </gray><white>" + formatPercent(score.getExtremeHighlandRatio()) + "</white>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>中心 </white><gray>可站立 </gray><white>" + formatPercent(score.getStandableRatio())
                    + "</white> <gray>斷崖 </gray><white>" + formatPercent(score.getCliffRatio())
                    + "</white> <gray>高度差 </gray><white>" + score.getCenterHeightSpread()
                    + "</white> <gray>低品質區塊 </gray><white>" + score.getLowSectionCount() + "</white>");
            Chat.send(player, "<gray>[</gray><green>中心計算</green><gray>]</gray> <white>扣分原因 </white><gray>" + formatReasons(score.getReasons()) + "</gray>");
        }

        private MatchCenter currentCenter() {
            return candidates.get(candidateIndex);
        }

        private void finish(CenterSearchResult result) {
            MatchCenter previewCenter = result.getBestCandidate() == null
                    ? CenterCleaner.spawnCenter(world, initialBorderSize())
                    : result.getBestCandidate().getCenter();

            Game.getGame().setMatchCenter(previewCenter);
            saveWorldReadyCache(player);
            BorderUtil.setBorders(world, previewCenter.getBorderSize());
            Chat.send(player, Messages.Host.WORLD_CREATED.replace("{generator}", "停用"));
            player.teleport(previewLocation(world, previewCenter));
            player.setGameMode(GameMode.CREATIVE);
            Extra.sound(player, Sounds.Host.WORLD_CREATED);
            sendSearchResult(player, result);
            cancel();
        }

        private long elapsedMillis() {
            return Math.max(0L, System.currentTimeMillis() - startMillis);
        }
    }
}
