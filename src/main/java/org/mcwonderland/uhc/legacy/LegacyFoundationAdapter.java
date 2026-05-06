package org.mcwonderland.uhc.legacy;

import org.bukkit.command.CommandSender;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permissible;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.ChunkKeeper;
import org.mineacademy.fo.model.SimpleReplacer;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.ObjectCreator;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LegacyFoundationAdapter {

    @FunctionalInterface
    public interface CommandGroupRegistrar {

        void register(Object commandGroup);
    }

    private LegacyFoundationAdapter() {
    }

    public static void extractFile(String path) {
        FileUtil.extract(path);
    }

    public static void extractFile(String from, String to) {
        FileUtil.extract(from, to);
    }

    public static void log(String... messages) {
        Common.log(messages);
    }

    public static void logFramed(String... messages) {
        Common.logFramed(messages);
    }

    public static void logReplacing(List<String> messages, String placeholder, Object value) {
        Common.log(new SimpleReplacer(messages).replace(placeholder, value).toArray());
    }

    public static void error(Throwable throwable, String... messages) {
        Common.error(throwable, messages);
    }

    public static void logNoPrefix(String... messages) {
        Common.logNoPrefix(messages);
    }

    public static void runLater(int delayTicks, Runnable task) {
        Common.runLater(delayTicks, task);
    }

    public static BukkitTask runAsync(Runnable task) {
        return Common.runAsync(task);
    }

    public static BukkitTask runLaterAsync(Runnable task) {
        return Common.runLaterAsync(task);
    }

    public static BukkitTask runTimer(int repeatTicks, Runnable task) {
        return Common.runTimer(repeatTicks, task);
    }

    public static BukkitTask runTimer(int delayTicks, int repeatTicks, Runnable task) {
        return Common.runTimer(delayTicks, repeatTicks, task);
    }

    public static BukkitTask runTimerAsync(int repeatTicks, Runnable task) {
        return Common.runTimerAsync(repeatTicks, task);
    }

    public static void dispatchCommand(CommandSender senderReplacement, String command) {
        Common.dispatchCommand(senderReplacement, command);
    }

    public static void dispatchCommandAsPlayer(Player player, String command) {
        Common.dispatchCommandAsPlayer(player, command);
    }

    public static Collection<? extends Player> getOnlinePlayers() {
        return Remain.getOnlinePlayers();
    }

    public static Player getPlayerByUUID(UUID uuid) {
        return Remain.getPlayerByUUID(uuid);
    }

    public static Player getPlayerByNick(String name, boolean ignoreVanished) {
        return PlayerUtil.getPlayerByNick(name, ignoreVanished);
    }

    public static List<String> getPlayerNames() {
        return Common.getPlayerNames();
    }

    public static boolean hasPermission(Permissible sender, String permission) {
        return PlayerUtil.hasPerm(sender, permission);
    }

    public static boolean checkPermission(Player player, String permission) {
        return Valid.checkPermission(player, permission);
    }

    public static void kickPlayer(Player player, String... message) {
        PlayerUtil.kick(player, message);
    }

    public static void sendActionBar(Player player, String message) {
        Remain.sendActionBar(player, message);
    }

    public static String colorize(String message) {
        return Common.colorize(message);
    }

    public static String stripColors(String message) {
        return Common.stripColors(message);
    }

    public static String formatTime(int seconds) {
        return TimeUtil.formatTime(seconds);
    }

    public static String replaceTimePlaceholders(String message, int seconds) {
        return TimeUtil.replacePlaceholders(message, seconds);
    }

    public static void configureTimeSymbols(String second, String seconds, String minute, String minutes) {
        TimeUtil.SECOND_SYMBOL = second;
        TimeUtil.SECONDS_SYMBOL = seconds;
        TimeUtil.MINUTE_SYMBOL = minute;
        TimeUtil.MINUTES_SYMBOL = minutes;
    }

    public static ItemStack getFirstItem(Player player, ItemStack item) {
        return PlayerUtil.getFirstItem(player, item);
    }

    public static File getFile(String path) {
        return FileUtil.getFile(path);
    }

    public static File getOrMakeFile(String path) {
        return FileUtil.getOrMakeFile(path);
    }

    public static <T> T nextItem(Iterable<T> items) {
        return RandomUtil.nextItem(items);
    }

    public static boolean chance(int percent) {
        return RandomUtil.chance(percent);
    }

    public static boolean nextBoolean() {
        return RandomUtil.nextBoolean();
    }

    public static double range(double value, double min, double max) {
        return MathUtil.range(value, min, max);
    }

    public static int range(int value, int min, int max) {
        return MathUtil.range(value, min, max);
    }

    public static long ceiling(double value) {
        return MathUtil.ceiling(value);
    }

    public static double formatFiveDigits(double value) {
        return MathUtil.formatFiveDigitsD(value);
    }

    public static boolean isSimilar(ItemStack first, ItemStack second) {
        return ItemUtil.isSimilar(first, second);
    }

    public static String bountifyCapitalized(Enum<?> value) {
        return ItemUtil.bountifyCapitalized(value);
    }

    public static String consoleLineSmooth() {
        return Common.consoleLineSmooth();
    }

    public static void setTellPrefix(String prefix) {
        Common.setTellPrefix(prefix);
    }

    public static void callEvent(Event event) {
        Common.callEvent(event);
    }

    public static void registerEvents(Listener listener) {
        Common.registerEvents(listener);
    }

    public static CommandGroupRegistrar commandGroupRegistrar(Consumer<SimpleCommandGroup> registerCommandGroup) {
        return commandGroup -> registerCommandGroup.accept((SimpleCommandGroup) commandGroup);
    }

    public static boolean isAtLeastMinecraft1_13() {
        return MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);
    }

    public static boolean isAtLeastMinecraft1_9() {
        return MinecraftVersion.atLeast(MinecraftVersion.V.v1_9);
    }

    public static boolean isAtLeastMinecraft1_11() {
        return MinecraftVersion.atLeast(MinecraftVersion.V.v1_11);
    }

    public static boolean isAtLeastMinecraft1_14() {
        return MinecraftVersion.atLeast(MinecraftVersion.V.v1_14);
    }

    public static boolean isOlderThanMinecraft1_9() {
        return MinecraftVersion.olderThan(MinecraftVersion.V.v1_9);
    }

    public static boolean isOlderThanMinecraft1_14() {
        return MinecraftVersion.olderThan(MinecraftVersion.V.v1_14);
    }

    public static String getServerVersion() {
        return MinecraftVersion.getServerVersion();
    }

    public static boolean isPaperServer() {
        return Remain.isPaper();
    }

    public static double getMaxHealth(Player player) {
        return Remain.getMaxHealth(player);
    }

    public static Object getHandleEntity(Entity entity) {
        return Remain.getHandleEntity(entity);
    }

    public static MetadataValue getTempMetadata(Entity entity, String tag) {
        return CompMetadata.getTempMetadata(entity, tag);
    }

    public static boolean hasTempMetadata(Entity entity, String tag) {
        return CompMetadata.hasTempMetadata(entity, tag);
    }

    public static void setTempMetadata(Entity entity, String tag) {
        CompMetadata.setTempMetadata(entity, tag);
    }

    public static void setTempMetadata(Entity entity, String tag, Object value) {
        CompMetadata.setTempMetadata(entity, tag, value);
    }

    public static void removeTempMetadata(Entity entity, String tag) {
        CompMetadata.removeTempMetadata(entity, tag);
    }

    public static void setChunkForceLoaded(Chunk chunk, boolean forceLoaded) {
        ChunkKeeper.setForceLoaded(chunk, forceLoaded);
    }

    public static Object newBlockPosition(int x, int y, int z) {
        return ObjectCreator.NMS_BLOCKPOSITION.getInstance(x, y, z);
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return ReflectionUtil.getMethod(clazz, methodName, parameterTypes);
    }

    public static Object getFieldContent(Object instance, String fieldName) {
        return ReflectionUtil.getFieldContent(instance, fieldName);
    }

    public static <T> T getStaticFieldContent(Class<?> clazz, String fieldName) {
        return ReflectionUtil.getStaticFieldContent(clazz, fieldName);
    }

    public static Object invoke(Method method, Object instance, Object... parameters) {
        return ReflectionUtil.invoke(method, instance, parameters);
    }

    public static Object invoke(String methodName, Object instance, Object... parameters) {
        return ReflectionUtil.invoke(methodName, instance, parameters);
    }

    public static Location getLocationOrDefault(Supplier<Location> locationSupplier, Supplier<Location> defaultSupplier) {
        try {
            return locationSupplier.get();
        } catch (FoException | NullPointerException e) {
            return defaultSupplier.get();
        }
    }

    public static void checkBoolean(boolean expression, String message) {
        Valid.checkBoolean(expression, message);
    }

    public static RuntimeException failure(String message) {
        return new FoException(message);
    }

    public static boolean isFailure(Throwable throwable) {
        return throwable instanceof FoException;
    }

    public static void configureMenuClickSound() {
        Menu.setSound(new SimpleSound(CompSound.NOTE_STICKS.getSound(), 0, 0));
    }
}
