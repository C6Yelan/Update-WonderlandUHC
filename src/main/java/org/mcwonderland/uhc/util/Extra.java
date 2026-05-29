package org.mcwonderland.uhc.util;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.platform.sound.PluginSound;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.settings.Settings;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Extra {
    private static final String DEFAULT_LEVEL_NAME = "world";
    private static final String PAPER_DIMENSION_NAMESPACE = "minecraft";
    private static final String SERVER_PROPERTIES = "server.properties";
    private static final Random r = new Random();

    public static ItemStack[] mergeArrays(ItemStack[]... arrays) {
        List<ItemStack> list = new ArrayList<>();

        for (ItemStack[] array : arrays)
            list.addAll(Arrays.asList(array));

        return list.toArray(new ItemStack[0]);
    }

    public static void deleteWorld(final String world) {
        deleteWorld(Bukkit.getWorldContainer(), world);
    }

    static void deleteWorld(final File worldContainer, final String world) {
        for (File worldPath : worldStoragePaths(worldContainer, world))
            deleteFiles(worldPath);
    }

    static List<File> worldStoragePaths(final File worldContainer, final String world) {
        Set<File> paths = new LinkedHashSet<>();
        paths.add(new File(worldContainer, world));

        for (String levelName : levelNames(worldContainer)) {
            paths.add(new File(new File(new File(new File(worldContainer, levelName), "dimensions"), PAPER_DIMENSION_NAMESPACE), world));
        }

        return new ArrayList<>(paths);
    }

    private static Set<String> levelNames(final File worldContainer) {
        Set<String> levelNames = new LinkedHashSet<>();
        levelNames.add(DEFAULT_LEVEL_NAME);

        String configuredLevelName = readConfiguredLevelName(worldContainer);
        if (configuredLevelName != null && !configuredLevelName.isBlank())
            levelNames.add(configuredLevelName);

        File[] files = worldContainer.listFiles();
        if (files == null)
            return levelNames;

        for (File file : files) {
            if (new File(new File(file, "dimensions"), PAPER_DIMENSION_NAMESPACE).isDirectory())
                levelNames.add(file.getName());
        }

        return levelNames;
    }

    private static String readConfiguredLevelName(final File worldContainer) {
        File serverProperties = new File(worldContainer, SERVER_PROPERTIES);
        if (!serverProperties.isFile())
            return null;

        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(serverProperties)) {
            properties.load(input);
            return properties.getProperty("level-name");
        } catch (IOException ex) {
            return null;
        }
    }

    private static boolean deleteFiles(final File path) {
        if (path.exists()) {
            final File[] files = path.listFiles();
            if (files == null)
                return path.delete();

            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFiles(file);
                } else {
                    file.delete();
                }
            }
        }
        return path.delete();
    }

    public static void createHead() {
        ItemStack goldenHead = PluginMaterials.itemOf("GOLDEN_APPLE");
        ItemMeta gMeta = goldenHead.getItemMeta();
        gMeta.displayName(PluginText.toItemComponent(Settings.Misc.GOLDEN_HEAD_NAME));
        goldenHead.setItemMeta(gMeta);

        ShapedRecipe goldenHeadRecipe = new ShapedRecipe(new NamespacedKey(WonderlandUHC.getInstance(), "golden_head"), goldenHead);
        goldenHeadRecipe.shape("@@@", "@#@", "@@@");

        goldenHeadRecipe.setIngredient('@', PluginMaterials.materialOf("GOLD_INGOT"));
        goldenHeadRecipe.setIngredient('#', PluginMaterials.materialOf("PLAYER_HEAD"));
        Bukkit.getServer().addRecipe(goldenHeadRecipe);
    }

    public static void sound(Player player, PluginSound sound) {
        sound.play(player);
    }

    public static void sound(Collection<Player> players, PluginSound sound) {
        sound.play(players);
    }

    public static void sound(Location location, PluginSound sound) {
        sound.play(location);
    }

    public static void sound(PluginSound sound) {
        sound.playOnlinePlayers();
    }

    public static void potion(Player p, PotionEffectType type, int duration, int amplifier, boolean displayEffect) {
        p.addPotionEffect(new PotionEffect(type, duration, amplifier));
    }

    public static void clearInventory(Player player) {
        player.getInventory().clear();
    }

    public static void comepleteClear(Player player) {
        setMaxHealthAttribute(player, 20.0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setLevel(0);
        player.setExp(0);
        player.getActivePotionEffects().forEach(ps -> player.removePotionEffect(ps.getType()));
        player.setFireTicks(0);
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.2F);
    }

    public static Integer getOnlinePlayers() {
        return PluginPlayers.onlinePlayers().size();
    }

    public static void copyHealth(LivingEntity entity, LivingEntity copyTo) {
        setMaxHealth(copyTo, getMaxHealth(entity));
        copyTo.setHealth(entity.getHealth());
    }

    public static void setMaxHealth(LivingEntity entity, double health) {
        health = Math.max(0, health);

        setMaxHealthAttribute(entity, health);
        entity.setHealth(health);
    }

    public static double getMaxHealth(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 20.0 : maxHealth.getBaseValue();
    }

    public static void noAIAndSilent(LivingEntity entity) {
        entity.setAI(false);
        entity.setSilent(true);
    }

    private static void setMaxHealthAttribute(LivingEntity entity, double health) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth != null)
            maxHealth.setBaseValue(health);
    }

    public static double formatHealth(double health) {
        return toTwoDecimals(health);
    }


    public static double toTwoDecimals(double d) {
        return ( int ) Math.round(d * 100) / 100.0;
    }

    public static Integer randomizar(int min, int max) {
        return r.nextInt((max - min) + 1) + min;
    }

    public static Integer randomNum(int max) {
        return r.nextInt(max);
    }

    public static boolean isBetween(int var, int min, int max) {
        return var >= min && var <= max;
    }

    public static Location getHighestSafeLocation(World w, int x, int z) {
        for (int run = 0; run < 20; run++) {
            final int y = getHighestPoint(w, x, z);
            final Block block = w.getBlockAt(x, y, z);
            final Block above = block.getRelative(BlockFace.UP);
            final Block down = block.getRelative(BlockFace.DOWN);
            if (y != -1 && PluginMaterials.isAir(block) && PluginMaterials.isAir(above)
                    && down.getType().isSolid())
                return new Location(w, x, y, z);
        }
        return null;
    }

    public static final int getHighestPoint(World w, int x, int z) {
        for (int y = w.getMaxHeight(); y >= 0; y--) {
            if (!PluginMaterials.isAir(w.getBlockAt(x, y, z)))
                return y + 1;
        }
        return -1;
    }

    public static void restartServer() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Settings.RESTART_CMD);
    }
}
