package org.mcwonderland.uhc.util;

import org.mcwonderland.uhc.platform.text.PluginText;
import lombok.AllArgsConstructor;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.application.border.BorderService;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.border.BorderType;
import org.mcwonderland.uhc.game.settings.sub.UHCBorderSettings;
import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.platform.paper.PaperWorldBorderAdapter;
import org.mcwonderland.uhc.settings.Settings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 2019-12-11 下午 07:37
 */
public class BorderUtil {
    private static final int VISIBLE_BORDER_WARNING_DISTANCE = 5;
    private static final int VISIBLE_BORDER_WARNING_SECONDS = 5;
    private static final BorderService BORDER_SERVICE = new BorderService(new PaperWorldBorderAdapter());
    public static HashMap<World, Boolean> preBlocksPlaced = new HashMap<>();
    public static HashMap<World, Integer> preBlocksNumber = new HashMap<>();
    public static Map<World, List<Location>> preBorderBlocks = new HashMap<>();

    public static void generateBorder(World world, int size) {
        int radius = getRadius(size);
        MatchCenter center = UHCWorldUtils.getBorderCenter(world, size);

        if (!preBlocksPlaced.containsKey(world))
            preBlocksPlaced.put(world, false);
        if (!preBlocksNumber.containsKey(world))
            preBlocksNumber.put(world, 0);
        if (!preBorderBlocks.containsKey(world))
            preBorderBlocks.put(world, new ArrayList<>());

        if (preBlocksPlaced.get(world) == false) {
            preBorderBlocks.get(world).clear();

            /**
             * Minecraft的邊界系統頗奇怪
             * 輸入 /tp 25 90 25 會傳送到 25.5 90 25.5
             * 然而輸入 /tp -25 90 -25 則會傳送到 -24.5 90 -24.5
             * 或許是因為座標會自動「+0.5」的關係吧
             *
             * 因為這個原因，放基岩方塊時，可能要讓「負值」的方塊「再減一」，這樣才能真正放置到想要的位置
             * (例如: 想要放到 -25.5，則座標要為 -26.0)
             */

            int bedrockRadius = radius;
            int bedrockRadiusNative = -radius - 1;
            new BukkitRunnable() {
                boolean northLeftToRight = false;
                boolean southLeftToRight = false;
                boolean eastTopToDown = false;
                boolean westTopToDown = false;

                @Override
                public void run() {
                    if (!northLeftToRight) { // -x,-z 到 +x,-z
                        setFirstBedrocks(new CoordsData(bedrockRadiusNative, bedrockRadiusNative, bedrockRadius, bedrockRadiusNative));
                        northLeftToRight = true;
                        return;
                    }
                    if (!southLeftToRight) {// -x,+z 到 +x,+z
                        setFirstBedrocks(new CoordsData(bedrockRadiusNative, bedrockRadius, bedrockRadius, bedrockRadius));
                        southLeftToRight = true;
                        return;
                    }

                    if (!eastTopToDown) { // -x,-z+1 到 -x,+z-1
                        setFirstBedrocks(new CoordsData(bedrockRadiusNative, bedrockRadiusNative + 1, bedrockRadiusNative, bedrockRadius - 1));
                        eastTopToDown = true;
                        return;
                    }
                    if (!westTopToDown) {// +x,-z+1 到 +x,+z-1
                        setFirstBedrocks(new CoordsData(bedrockRadius, bedrockRadiusNative + 1, bedrockRadius, bedrockRadius - 1));
                        westTopToDown = true;
                        return;
                    }
                    preBlocksPlaced.put(world, true);
                    this.cancel();
                    return;
                }

                private void setFirstBedrocks(CoordsData coordsData) {
                    for (int x = coordsData.xFrom; x <= coordsData.xTo; x++) {
                        for (int z = coordsData.zFrom; z <= coordsData.zTo; z++) {
                            Block block = world.getHighestBlockAt(center.getX() + x, center.getZ() + z);
                            for (int a = 0; a < 30; a++) {//debug 最多嘗試30次
                                if (isNeedToGoDown(block))
                                    block = block.getRelative(BlockFace.DOWN);
                                else
                                    break;
                            }
                            block = block.getRelative(BlockFace.UP);
                            block.setType(Material.BEDROCK, false);
                            preBorderBlocks.get(world).add(block.getLocation());
                        }
                    }
                }

                private boolean isNeedToGoDown(Block block) {
                    return isContainAbleMaterials(block.getType()) || isNetherTopBedrock(block);
                }

                private boolean isNetherTopBedrock(Block block) {
                    String blockWorldName = block.getWorld().getName();
                    return blockWorldName.equalsIgnoreCase(world + "_nether")
                            && block.getType() == Material.BEDROCK;
                }

                private boolean isContainAbleMaterials(Material material) {
                    return PluginMaterials.isAir(material)
                            || PluginMaterials.isLongGrass(material)
                            || material.toString().contains("LEAVES")
                            || material.toString().contains("LOG")
                            || material.toString().contains("GRASS")
                            || material == Material.SUGAR_CANE
                            || material == Material.SNOW
                            || material == Material.WATER; // prevent nether border gen in the top
                }

                @AllArgsConstructor
                class CoordsData {
                    final int xFrom;
                    final int zFrom;
                    final int xTo;
                    final int zTo;
                }
            }.runTaskTimer(WonderlandUHC.getInstance(), 0, 5L);
        } else {
            preBlocksNumber.put(world, preBlocksNumber.get(world) + 1);
            for (Location loc : preBorderBlocks.get(world)) {
                loc.clone().add(0, preBlocksNumber.get(world), 0).getBlock().setType(Material.BEDROCK, false);
            }
        }
    }

    public static void clearBorderCaches() {
        preBlocksNumber.clear();
        preBorderBlocks.clear();
        preBlocksPlaced.clear();
    }

    public static boolean isInBorder(Location location) {
        World world = location.getWorld();
        int size;

        if (world.getEnvironment() == World.Environment.NETHER)
            size = GameUtils.getCurrentNetherBorder();
        else
            size = Game.getGame().getCurrentBorder();

        return isInBorder(location, size);
    }

    public static boolean isInBorder(Location loc, int borderSize) {
        return isInBorder(loc, borderSize, UHCWorldUtils.getBorderCenter(loc.getWorld(), borderSize));
    }

    public static boolean isInBorder(Location loc, int borderSize, MatchCenter center) {
        int radius = getRadius(borderSize) + 1;

        return Math.abs(loc.getX() - center.getX()) < radius && Math.abs(loc.getZ() - center.getZ()) < radius;
    }

    private static void setNativeBorder(World world, int size) {
        if (world == null)
            return;

        if (!UHCWorldUtils.isUhcWorld(world))
            return;

        if (!shouldApplyNativeBorder())
            return;

        BORDER_SERVICE.setFixedBorder(world.getName(), size, UHCWorldUtils.getBorderCenter(world, size));
    }

    private static boolean shouldApplyNativeBorder() {
        return Game.getSettings().getBorderSettings().getBorderType() != BorderType.TIMER
                || Settings.Border.INCLUDE_18_BORDER;
    }

    public static void moverBorder18(double time) {
        World world = UHCWorldUtils.getWorld();

        if (world == null)
            return;

        String worldName = world.getName();
        int finalSize = Game.getSettings().getBorderSettings().getFinalSizeOfShrinkModeBorder();
        BORDER_SERVICE.setWarning(worldName, VISIBLE_BORDER_WARNING_DISTANCE, VISIBLE_BORDER_WARNING_SECONDS);
        BORDER_SERVICE.shrinkBorder(worldName, finalSize, ( long ) time, UHCWorldUtils.getBorderCenter(world, finalSize));
    }

    public static double getShrinkSpeedPerSecond(int time) {
        UHCBorderSettings settings = Game.getSettings().getBorderSettings();
        return getShrinkSpeedPerSecond(settings.getInitialBorder(), settings.getFinalSizeOfShrinkModeBorder(), time);
    }

    public static double getShrinkSpeedPerSecond(int from, int to, int seconds) {
        if (seconds <= 0)
            return 0D;

        return PluginText.formatFiveDigits((from - to) / (seconds * 2D));
    }

    public static int getShrinkSecondsCost() {
        UHCBorderSettings settings = Game.getSettings().getBorderSettings();
        return getShrinkSecondsCost(settings.getInitialBorder(),
                settings.getFinalSizeOfShrinkModeBorder(),
                settings.getBorderShrinkSpeed());
    }

    public static int getShrinkSecondsCost(int from, int to, double shrinkBlocksPerSecond) {
        if (shrinkBlocksPerSecond <= 0D)
            return 0;

        return (int) ((from - to) / (shrinkBlocksPerSecond * 2D));
    }

    public static void removeUHCWorldWBBorders() {
        for (String worldName : UHCWorldUtils.getUhcWorldNames())
            removeWBBorder(worldName);
    }

    public static void removeWBBorder(World world) {
        if (world == null)
            return;

        removeWBBorder(world.getName());
    }

    public static void removeWBBorder(String worldName) {
        BORDER_SERVICE.reset(worldName);
    }

    public static void setInitialBorders() {
        for (World uhcWorld : UHCWorldUtils.getUhcWorlds()) {
            if (uhcWorld == null)
                continue;

            BORDER_SERVICE.setWarning(uhcWorld.getName(), VISIBLE_BORDER_WARNING_DISTANCE, VISIBLE_BORDER_WARNING_SECONDS);
        }

        UHCBorderSettings borderSettings = Game.getSettings().getBorderSettings();
        setBorders(UHCWorldUtils.getWorld(), borderSettings.getInitialBorder());
        setBorders(UHCWorldUtils.getNether(), borderSettings.getInitialNetherBorder());
    }

    public static void setBorders(World world, int size) {
        setNativeBorder(world, size);
    }

    public static int getRadius(int size) {
        return size / 2;
    }


    public static int getMoveBorder(World world) {
        if (world == null)
            return 0;

        return BORDER_SERVICE.getSize(world.getName());
    }
}
