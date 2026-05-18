package org.mcwonderland.uhc.platform.sound;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public final class PluginSound {

    private static final PluginSound NONE = new PluginSound(null, 0F, 1F, false, false);

    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final boolean randomPitch;
    private final boolean enabled;

    private PluginSound(Sound sound, float volume, float pitch, boolean randomPitch, boolean enabled) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.randomPitch = randomPitch;
        this.enabled = enabled;
    }

    public static PluginSound of(Sound sound, float volume, float pitch) {
        return new PluginSound(sound, volume, pitch, false, true);
    }

    public static PluginSound ofRandomPitch(Sound sound, float volume) {
        return new PluginSound(sound, volume, 1F, true, true);
    }

    public static PluginSound none() {
        return NONE;
    }

    public void play(Player player) {
        if (!enabled || player == null)
            return;

        player.playSound(player.getLocation(), sound, volume, pitch());
    }

    public void play(Collection<Player> players) {
        if (!enabled || players == null)
            return;

        for (Player player : players)
            play(player);
    }

    public void play(Location location) {
        if (!enabled || location == null || location.getWorld() == null)
            return;

        location.getWorld().playSound(location, sound, volume, pitch());
    }

    public void playOnlinePlayers() {
        play(new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    private float pitch() {
        return randomPitch ? ThreadLocalRandom.current().nextFloat() : pitch;
    }

    @Override
    public String toString() {
        if (!enabled)
            return "none";

        return sound + " " + volume + " " + (randomPitch ? "random" : pitch);
    }
}
