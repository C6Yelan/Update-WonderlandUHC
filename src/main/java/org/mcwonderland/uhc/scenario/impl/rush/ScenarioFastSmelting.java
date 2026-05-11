package org.mcwonderland.uhc.scenario.impl.rush;

import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioFastSmelting extends ConfigBasedScenario implements Listener {

    private static final short SPEED_UP = 2;
    private final Map<String, BukkitTask> boosted = new HashMap<>();

    public ScenarioFastSmelting(ScenarioName name) {
        super(name);
    }


    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        try {
            ensureBoost(event.getBlock());
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling a furnace burn event");
        }
    }

    @EventHandler
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        try {
            ensureBoost(event.getBlock());
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling a furnace start smelt event");
        }
    }

    @Override
    public void onDisable() {
        boosted.values().forEach(BukkitTask::cancel);
        boosted.clear();
    }

    private void ensureBoost(Block block) {
        String blockKey = getBlockKey(block);

        if (!(boosted.containsKey(blockKey)))
            increaseFurnaceSpeed(block, blockKey);
    }

    private void increaseFurnaceSpeed(Block block, String blockKey) {
        BukkitTask task = LegacyFoundationAdapter.runTimer(0, 1, new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (block.getType() != LegacyFoundationAdapter.materialOf("FURNACE")) {
                        stopBoost(blockKey);
                        return;
                    }

                    Furnace furnace = ( Furnace ) block.getState();
                    furnace.setBurnTime(calculateBoostedBurnTime(furnace.getBurnTime(), SPEED_UP));

                    if (needUpdate(furnace)) {
                        furnace.setCookTime(calculateBoostedCookTime(furnace.getCookTime(), SPEED_UP));
                    } else {
                        stopBoost(blockKey);
                        return;
                    }
                    furnace.update();
                } catch (RuntimeException | LinkageError ex) {
                    stopBoost(blockKey);
                    handleRuntimeFailure(ex, "boosting a furnace");
                }
            }
        });

        boosted.put(blockKey, task);
    }

    private boolean needUpdate(Furnace furnace) {
        ItemStack smelting = furnace.getInventory().getSmelting();
        boolean hasItemToSmelt = smelting != null && smelting.getType() != LegacyFoundationAdapter.materialOf("AIR");
        boolean isFueling = furnace.getInventory().getFuel() != null || furnace.getBurnTime() > 0;


        return hasItemToSmelt
                && isFueling
                && (furnace.getCookTime() > 0 || furnace.getBurnTime() > 0);
    }

    static short calculateBoostedBurnTime(short burnTime, short speedUp) {
        return ( short ) Math.max(0, burnTime - speedUp + speedUp / 10);
    }

    static short calculateBoostedCookTime(short cookTime, short speedUp) {
        return ( short ) Math.min(Short.MAX_VALUE, cookTime + speedUp);
    }

    private String getBlockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private void stopBoost(String blockKey) {
        BukkitTask task = boosted.remove(blockKey);
        if (task != null)
            task.cancel();
    }

    private void handleRuntimeFailure(Throwable throwable, String action) {
        LegacyFoundationAdapter.error(
                throwable,
                "Scenario 'Fast_Smelting' failed while " + action + ".",
                "The scenario was disabled for this run, but the game flow will continue."
        );
        disableAfterRuntimeFailure();
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            LegacyFoundationAdapter.error(
                    disableEx,
                    "Scenario 'Fast_Smelting' could not be disabled after a runtime failure."
            );
        }
    }
}
