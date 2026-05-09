package org.mcwonderland.uhc.util;

import org.bukkit.Bukkit;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

public class RuntimeUtil {

    private static final DecimalFormat format = new DecimalFormat("00.00");
    private static final double DEFAULT_TPS = 20D;
    private static Class<?> clazz = null;
    private static Object si = null;
    private static Field tpsField = null;
    private static final Runtime rt = Runtime.getRuntime();
    private static final int fillMemoryTolerance = 500;

    static {
        try {
            clazz = Class.forName("net.minecraft.server." + LegacyFoundationAdapter.getServerVersion() + "." + "MinecraftServer");
            si = clazz.getMethod("getServer").invoke(null);
            tpsField = si.getClass().getField("recentTps");
        } catch (Exception e) {
            tpsField = null;
        }
    }

    public static double getTPS(int time) {
        double paperTps = getPaperTPS(time);
        if (paperTps > 0D)
            return paperTps;

        if (tpsField == null || si == null)
            return DEFAULT_TPS;

        try {
            double[] tps = (double[]) tpsField.get(si);
            return getTPSAt(tps, time);
        } catch (Exception e) {
            return DEFAULT_TPS;
        }
    }

    public static DecimalFormat getTPSFormat() {
        return format;
    }

    public static long Now() {
        return System.currentTimeMillis();
    }

    public static int AvailableMemory() {
        return (int) ((rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576);
    }

    public static boolean AvailableMemoryTooLow() {
        return AvailableMemory() < fillMemoryTolerance;
    }

    private static double getPaperTPS(int time) {
        try {
            Method getTps = Bukkit.getServer().getClass().getMethod("getTPS");
            double[] tps = (double[]) getTps.invoke(Bukkit.getServer());
            return getTPSAt(tps, time);
        } catch (Exception ex) {
            return -1D;
        }
    }

    private static double getTPSAt(double[] tps, int time) {
        if (tps == null || tps.length == 0)
            return DEFAULT_TPS;

        int index = Math.max(0, Math.min(time, tps.length - 1));
        return tps[index];
    }

}
