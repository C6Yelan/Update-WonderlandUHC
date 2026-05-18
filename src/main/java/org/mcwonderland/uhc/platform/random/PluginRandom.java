package org.mcwonderland.uhc.platform.random;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public final class PluginRandom {

    private PluginRandom() {
    }

    public static <T> T nextItem(Collection<T> items) {
        if (items == null || items.isEmpty())
            return null;

        int index = ThreadLocalRandom.current().nextInt(items.size());
        int current = 0;
        for (T item : items) {
            if (current == index)
                return item;

            current++;
        }

        return null;
    }

    public static boolean chance(int percent) {
        if (percent <= 0)
            return false;
        if (percent >= 100)
            return true;

        return ThreadLocalRandom.current().nextInt(100) < percent;
    }

    public static boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }
}
