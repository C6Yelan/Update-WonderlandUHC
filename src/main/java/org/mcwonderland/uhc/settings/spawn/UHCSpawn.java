package org.mcwonderland.uhc.settings.spawn;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.io.File;
import java.io.IOException;

@Getter
public class UHCSpawn {

    @Getter(AccessLevel.PRIVATE)
    private final String prefix;
    private final File file;
    private final YamlConfiguration configuration;
    private Location location;
    private boolean set;

    protected UHCSpawn(String prefix) {
        this.prefix = prefix;
        this.file = new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.SPAWNS);
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.location = getLocationSafe(prefix);
    }

    private Location getLocationSafe(String path) {
        String raw = configuration.getString(path);

        if (raw == null || raw.isBlank())
            return defaultLocation();

        try {
            Location location = parseLocation(raw);
            set = true;
            return location;
        } catch (RuntimeException e) {
            return defaultLocation();
        }
    }

    private Location defaultLocation(String... msg) {
        PluginConsole.log(msg);
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public void updateLocation(Location newOne) {
        location = newOne;
        set = true;

        configuration.set(prefix, serializeLocation(newOne));
        saveFile();
    }

    private Location parseLocation(String raw) {
        String value = raw.replace("\"", "").trim();
        String[] parts = value.contains(", ") ? value.split(", ") : value.split(" ");

        if (parts.length != 4 && parts.length != 6)
            throw new IllegalArgumentException("Invalid location: " + raw);

        World world = Bukkit.getWorld(parts[0]);
        if (world == null)
            throw new IllegalArgumentException("Invalid location world: " + parts[0]);

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        float yaw = parts.length == 6 ? Float.parseFloat(parts[4]) : 0F;
        float pitch = parts.length == 6 ? Float.parseFloat(parts[5]) : 0F;

        return new Location(world, x, y, z, yaw, pitch);
    }

    private String serializeLocation(Location location) {
        int yaw = Math.round(location.getYaw());
        int pitch = Math.round(location.getPitch());
        String value = location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();

        return yaw != 0 || pitch != 0 ? value + " " + yaw + " " + pitch : value;
    }

    private void saveFile() {
        try {
            configuration.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save " + UHCFiles.SPAWNS, ex);
        }
    }

}
