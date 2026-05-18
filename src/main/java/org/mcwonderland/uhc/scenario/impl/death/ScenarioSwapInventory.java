package org.mcwonderland.uhc.scenario.impl.death;

import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.events.UHCGamingDeathEvent;
import org.mcwonderland.uhc.game.CombatRelog;
import org.mcwonderland.uhc.model.InventoryContent;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioSwapInventory extends ConfigBasedScenario implements Listener {

    public ScenarioSwapInventory(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void handleSwapInventory(UHCGamingDeathEvent e) {
        try {
            swapInventory(e);
        } catch (RuntimeException | LinkageError ex) {
            PluginConsole.error(
                    ex,
                    "Scenario 'Swap_Inventory' failed while handling a death event.",
                    "The scenario was disabled for this run, but the death flow will continue."
            );
            disableAfterRuntimeFailure();
        }
    }

    private void swapInventory(UHCGamingDeathEvent e) {
        Player player = e.getUhcPlayer().getPlayer();
        Player killer = e.getEntity().getKiller();

        if (killer == null)
            return;

        CombatRelog relog = CombatRelog.getByRelogEntity(e.getEntity());

        swapInventory(player, killer, e.getEntity(), e.getDrops(), relog);
    }

    private void swapInventory(Player deathPlayer, Player killer, LivingEntity deathEntity, List<ItemStack> drops, CombatRelog relog) {
        PlayerInventory deathPlayerInv = deathPlayer.getInventory();
        PlayerInventory killerInv = killer.getInventory();

        InventoryContent deathPlayerInvContent = new InventoryContent(deathPlayerInv);
        InventoryContent killerInvContent = new InventoryContent(killerInv);
        InventoryContent relogInvContent = relog == null ? null : relog.getInventoryContent();
        ItemStack[] deathDropItems = relogInvContent == null ? deathPlayerInvContent.getAllItems() : relogInvContent.getAllItems();
        List<ItemStack> originalDrops = new ArrayList<>(drops);
        List<ItemStack> swappedDrops = buildSwappedDrops(originalDrops, deathDropItems, killerInvContent.getAllItems());

        try {
            /*
             * SwapInventory 同時改玩家物品欄與死亡 drops 的意義：
             * - killer 取得 victim / relog 替身的物品欄。
             * - victim 的物品欄變成 killer 原本的物品欄。
             * - death drops 變成 killer 原本物品欄，加上不屬於 victim 物品欄的既有 drops。
             *
             * TimeBomb 開啟時，這批交換後的 drops 必須留在事件清單，讓 TimeBomb 放進死亡箱。
             * 沒有 TimeBomb 時，這裡會手動釋放並清空事件清單，避免 Bukkit 或後續 handler 重複掉落。
             */
            if (relog != null)
                relog.setInventoryContent(killerInvContent);

            killerInvContent.setContents(deathPlayerInv);
            deathPlayerInvContent.setContents(killerInv);

            replaceDrops(drops, swappedDrops);
            if (shouldReleaseDropsImmediately(isTimeBombEnabled()))
                releaseDrops(deathEntity, drops);
        } catch (RuntimeException | LinkageError ex) {
            rollbackSwap(deathPlayerInv, killerInv, relog, deathPlayerInvContent, killerInvContent, relogInvContent, originalDrops, drops);
            throw ex;
        }
    }

    static List<ItemStack> buildSwappedDrops(List<ItemStack> currentDrops, ItemStack[] deathPlayerItems, ItemStack[] killerItems) {
        return buildSwappedDrops(currentDrops, deathPlayerItems, killerItems, ScenarioSwapInventory::isDropItem);
    }

    static <T> List<T> buildSwappedDrops(List<T> currentDrops, T[] deathPlayerItems, T[] killerItems) {
        return buildSwappedDrops(currentDrops, deathPlayerItems, killerItems, item -> true);
    }

    static <T> List<T> buildSwappedDrops(List<T> currentDrops, T[] deathPlayerItems, T[] killerItems, Predicate<T> dropFilter) {
        List<T> swappedDrops = new ArrayList<>(currentDrops);
        // 先移除 victim / relog 物品欄，再加入 killer 原本物品欄，避免 victim 物品與交換後 drops 混在一起。
        swappedDrops.removeAll(Arrays.asList(deathPlayerItems));
        addDropItems(swappedDrops, killerItems, dropFilter);
        return swappedDrops;
    }

    private static <T> void addDropItems(List<T> drops, T[] items, Predicate<T> dropFilter) {
        for (T item : items) {
            if (dropFilter.test(item))
                drops.add(item);
        }
    }

    private static boolean isDropItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private void rollbackSwap(
            PlayerInventory deathPlayerInv,
            PlayerInventory killerInv,
            CombatRelog relog,
            InventoryContent deathPlayerInvContent,
            InventoryContent killerInvContent,
            InventoryContent relogInvContent,
            List<ItemStack> originalDrops,
            List<ItemStack> drops
    ) {
        try {
            deathPlayerInvContent.setContents(deathPlayerInv);
            killerInvContent.setContents(killerInv);

            if (relog != null)
                relog.setInventoryContent(relogInvContent);

            replaceDrops(drops, originalDrops);
        } catch (RuntimeException | LinkageError rollbackEx) {
            PluginConsole.error(
                    rollbackEx,
                    "Scenario 'Swap_Inventory' could not fully roll back a failed inventory swap."
            );
        }
    }

    private static void replaceDrops(List<ItemStack> drops, List<ItemStack> replacement) {
        drops.clear();
        drops.addAll(replacement);
    }

    private void releaseDrops(LivingEntity deathEntity, List<ItemStack> drops) {
        // 手動釋放後必須清空事件 drops；這裡清空就是防止複製的關鍵。
        if (deathEntity == null)
            return;

        List<ItemStack> releasedDrops = new ArrayList<>(drops);
        drops.clear();

        for (ItemStack item : releasedDrops) {
            if (isDropItem(item))
                deathEntity.getWorld().dropItemNaturally(deathEntity.getLocation(), item);
        }
    }

    static boolean shouldReleaseDropsImmediately(boolean timeBombEnabled) {
        return !timeBombEnabled;
    }

    private boolean isTimeBombEnabled() {
        Scenario scenario = WonderlandUHC.getInstance().getScenarioManager().getScenario(ScenarioName.TIME_BOMB);
        return scenario != null && scenario.isEnabled();
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            PluginConsole.error(
                    disableEx,
                    "Scenario 'Swap_Inventory' could not be disabled after a runtime failure."
            );
        }
    }
}
