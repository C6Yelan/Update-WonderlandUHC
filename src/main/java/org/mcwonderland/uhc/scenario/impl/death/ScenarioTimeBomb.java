package org.mcwonderland.uhc.scenario.impl.death;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mcwonderland.uhc.events.UHCGamingDeathEvent;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.model.SimpleSound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 2019-12-07 下午 03:12
 */
@Getter
public class ScenarioTimeBomb extends ConfigBasedScenario implements Listener {
    private static final Set<TimeBombChest> timeBombs = new HashSet<>();
    private static final BlockFace TIME_BOMB_CHEST_FACING = BlockFace.WEST;

    @FilePath(name = "Exploded")
    private String explodedMessage;
    @FilePath(name = "Explode_Blocks")
    private Boolean explodedBlocks;
    @FilePath(name = "Explosion_Power")
    private Integer explosionPower;
    @FilePath(name = "Explode_After_Seconds")
    private Integer explodeAfterSeconds;
    @FilePath(name = "Warn_Sound")
    private SimpleSound warnSound;
    @FilePath(name = "Warn_Sound_Danger")
    private SimpleSound warnSoundDanger;


    public ScenarioTimeBomb(ScenarioName name) {
        super(name);

    }

    @Override
    protected void onConfigReload() {
        PluginScheduler.runTimer(20, new ChestExplodeTicker());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    protected void handleTimeBomb(UHCGamingDeathEvent e) {
        int originalDropCount = e.getDrops().size();

        try {
            /*
             * TimeBomb 是死亡 drops 的最後消費者之一：
             * - 前面的 scenario 可以先改寫 e.getDrops()，例如 SwapInventory 或 BackPack。
             * - TimeBomb 只把死亡箱容量內的 drops 放進箱子。
             * - 超出死亡箱容量的 drops 由這裡手動掉在死亡位置，不能依賴事件後續自動掉落。
             *
             * 放進箱子與 overflow 都處理完後會清空事件 drops，避免同一批物品又被 Bukkit 掉一次。
             */
            int storedDropCount = new TimeBombRunner(e).run();
            List<ItemStack> overflowDrops = extractOverflowDrops(e.getDrops(), storedDropCount);
            dropOverflowDrops(e.getEntity(), overflowDrops);
            logOverflowDrops(originalDropCount, storedDropCount);
        } catch (RuntimeException | LinkageError ex) {
            PluginConsole.error(
                    ex,
                    "Scenario 'Time_Bomb' failed while creating a death chest.",
                    "The scenario was disabled for this run, but regular death drops will continue."
            );
            disableAfterRuntimeFailure();
        }
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLoreTime(list, explodeAfterSeconds);
    }

    class TimeBombRunner {
        private final Entity entity;
        private Block leftSideChest, RightSideChest;
        private final List<ItemStack> drops;

        private TimeBombRunner(UHCGamingDeathEvent event) {
            this.entity = event.getEntity();
            this.drops = event.getDrops();
        }

        private int run() {
            spawnLargeChest();
            clearUpperBlocks();
            int storedDropCount = putItemsIn();

            addToTimebombs();
            return storedDropCount;
        }


        private void spawnLargeChest() {
            Location chestSpawnLoc = entity.getLocation().clone().add(0, 1, 0);
            leftSideChest = chestSpawnLoc.getBlock().getRelative(BlockFace.DOWN);
            RightSideChest = leftSideChest.getRelative(BlockFace.NORTH);
            placeChestPart(leftSideChest, org.bukkit.block.data.type.Chest.Type.LEFT);
            placeChestPart(RightSideChest, org.bukkit.block.data.type.Chest.Type.RIGHT);
        }

        private void placeChestPart(Block block, org.bukkit.block.data.type.Chest.Type type) {
            org.bukkit.block.data.type.Chest data = ( org.bukkit.block.data.type.Chest ) Material.CHEST.createBlockData();
            data.setFacing(TIME_BOMB_CHEST_FACING);
            data.setType(type);
            block.setBlockData(data, false);
        }

        private void clearUpperBlocks() {
            leftSideChest.getRelative(BlockFace.UP).setType(Material.AIR);
            RightSideChest.getRelative(BlockFace.UP).setType(Material.AIR);
        }

        private void addToTimebombs() {
            TimeBombChest chest = new TimeBombChest(UHCPlayer.getFromEntity(entity), leftSideChest.getLocation());
            timeBombs.add(chest);
        }

        private int putItemsIn() {
            Chest chestState = ( Chest ) leftSideChest.getState();
            Inventory inventory = chestState.getInventory();
            // 先建立 snapshot，避免邊讀 drops 邊清 drops 時影響死亡箱內容。
            List<ItemStack> storedDrops = snapshotStorableDrops(drops, inventory.getSize());

            for (int i = 0; i < storedDrops.size(); i++)
                inventory.setItem(i, storedDrops.get(i));

            return storedDrops.size();
        }
    }

    static int countStoredDrops(int dropCount, int inventorySize) {
        return Math.max(0, Math.min(dropCount, inventorySize));
    }

    static <T> List<T> extractOverflowDrops(List<T> drops, int storedDropCount) {
        List<T> overflowDrops = new ArrayList<>();

        if (drops == null)
            return overflowDrops;

        int removableCount = Math.max(0, Math.min(storedDropCount, drops.size()));
        overflowDrops.addAll(drops.subList(removableCount, drops.size()));
        // TimeBomb 已經接手本次死亡 drops；清空事件清單可避免已入箱或 overflow 的物品重複掉落。
        drops.clear();
        return overflowDrops;
    }

    private static void dropOverflowDrops(Entity entity, List<ItemStack> overflowDrops) {
        if (entity == null || overflowDrops == null)
            return;

        for (ItemStack item : overflowDrops) {
            if (isStorableDrop(item))
                entity.getWorld().dropItemNaturally(entity.getLocation(), item);
        }
    }

    static List<ItemStack> snapshotStorableDrops(List<ItemStack> drops, int inventorySize) {
        List<ItemStack> snapshot = new ArrayList<>();
        int limit = Math.max(0, inventorySize);

        if (drops == null || limit == 0)
            return snapshot;

        for (ItemStack drop : drops) {
            if (snapshot.size() >= limit)
                break;

            if (isStorableDrop(drop))
                snapshot.add(drop);
        }

        return snapshot;
    }

    static boolean isStorableDrop(ItemStack itemStack) {
        return itemStack != null && isStorableMaterial(itemStack.getType());
    }

    static boolean isStorableMaterial(Material material) {
        return material != null && material != Material.AIR;
    }

    private static void logOverflowDrops(int originalDropCount, int storedDropCount) {
        if (storedDropCount >= originalDropCount)
            return;

        try {
            PluginConsole.logNoPrefix(
                    "&e[WonderlandUHC] Scenario 'Time_Bomb' stored " + storedDropCount + " of " + originalDropCount + " death drops.",
                    "&e[WonderlandUHC] Overflow drops were released at the death location."
            );
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    class ChestExplodeTicker extends BukkitRunnable {

        @Override
        public void run() {
            Set<TimeBombChest> timeBombs = new HashSet<>(ScenarioTimeBomb.timeBombs);
            timeBombs.forEach(this::tick);
        }

        private void tick(TimeBombChest chest) {
            try {
                chest.tick();
            } catch (RuntimeException | LinkageError ex) {
                ScenarioTimeBomb.timeBombs.remove(chest);
                PluginConsole.error(
                        ex,
                        "Scenario 'Time_Bomb' failed while ticking a death chest.",
                        "The failed chest was removed; other Time_Bomb chests will continue ticking."
                );
            }
        }
    }

    @RequiredArgsConstructor
    public class TimeBombChest {
        private final UHCPlayer owner;
        private final Location location;
        private int time;

        public void tick() {
            time++;
            int secondUntilExplode = explodeAfterSeconds - time;

            if (secondUntilExplode <= 0)
                explode();
            else if (Extra.isBetween(secondUntilExplode, 3, 5))
                Extra.sound(location, warnSound);
            else if (Extra.isBetween(secondUntilExplode, 1, 2))
                Extra.sound(location, warnSoundDanger);
        }

        public void explode() {
            Block block = location.getBlock();
            World world = location.getWorld();

            try {
                createExplosion(location);
                world.strikeLightning(location);

                block.setType(Material.AIR);
                block.getRelative(BlockFace.NORTH).setType(Material.AIR);

                Chat.broadcast(explodedMessage.replace("{player}", owner.getName()));
            } finally {
                timeBombs.remove(this);
            }
        }

        private void createExplosion(Location location) {
            location = WorldUtils.centerOfBlock(location);
            final boolean setFire = false;

            // 只有使用 x,y,z 作為參數的方法才能支援 1.8 ~ 最新版本
            // createExplosion(Location loc, float power, boolean setFire) 只在 1.14+ 才有
            location.getWorld().createExplosion(
                    location.getX(), location.getY(), location.getZ(),
                    explosionPower,
                    setFire,
                    explodedBlocks
            );
        }
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            PluginConsole.error(
                    disableEx,
                    "Scenario 'Time_Bomb' could not be disabled after a runtime failure."
            );
        }
    }

}
