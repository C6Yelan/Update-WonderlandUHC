package org.mcwonderland.uhc.scenario.impl.special;

import lombok.Getter;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.events.UHCGamingDeathEvent;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 2019-12-07 下午 01:18
 */
@Getter
public class ScenarioBackPack extends ConfigBasedScenario implements Listener {
    @FilePath(name = "Cant_Use_Msg")
    private String cantUseMsg;
    @FilePath(name = "Size")
    private Integer size;

    private Map<UHCTeam, Inventory> backpacks = new HashMap<>();

    public ScenarioBackPack(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onGamingEntityDeath(UHCGamingDeathEvent e) {
        UHCPlayer uhcPlayer = e.getUhcPlayer();

        UHCTeam team = uhcPlayer.getTeam();

        if (!team.isEliminate())
            return;

        Inventory backpack = team.getBackpack();
        Collection<ItemStack> backpackItems = collectDropItems(backpack.getContents());
        Player killer = e.getEntity().getKiller();
        boolean swapInventoryEnabled = isScenarioEnabled(ScenarioName.SWAP_INVENTORY);
        boolean timeBombEnabled = isScenarioEnabled(ScenarioName.TIME_BOMB);

        /*
         * 死亡掉落物歸屬規則：
         * - SwapInventory 有 killer 時，被淘汰隊伍的背包物品視為 killer 的戰利品，直接塞進 killer 物品欄。
         * - TimeBomb 會在 HIGHEST priority 消耗 e.getDrops() 並放進死亡箱，所以背包物品必須留在 drops。
         * - 若沒有其他 scenario 會接手這批背包物品，BackPack 需要自己在死亡位置釋放，避免單開時物品消失。
         *
         * 這段分流是為了保留已驗證的組合行為，同時修正 BackPack 單開時隊伍背包被清空但不掉落的問題。
         */
        if (killer != null && swapInventoryEnabled)
            giveBackpackItemsToKiller(killer, backpackItems);
        else if (shouldReleaseBackpackItemsAtDeathLocation(timeBombEnabled, swapInventoryEnabled, killer != null))
            dropBackpackItems(e.getEntity(), backpackItems);
        else
            e.getDrops().addAll(backpackItems);

        backpack.clear();
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list, "{cmd}", "backpack");
    }

    private boolean isScenarioEnabled(ScenarioName scenarioName) {
        Scenario scenario = WonderlandUHC.getInstance().getScenarioManager().getScenario(scenarioName);
        return scenario != null && scenario.isEnabled();
    }

    private void giveBackpackItemsToKiller(Player killer, Collection<ItemStack> backpackItems) {
        // SwapInventory 組合下，只有 killer 物品欄塞不下的背包物品才掉在 killer 位置。
        Map<Integer, ItemStack> overflow = killer.getInventory().addItem(backpackItems.toArray(new ItemStack[0]));

        for (ItemStack item : overflow.values()) {
            if (isDropItem(item))
                killer.getWorld().dropItemNaturally(killer.getLocation(), item);
        }
    }

    private void dropBackpackItems(LivingEntity deathEntity, Collection<ItemStack> backpackItems) {
        // BackPack 單開時，這些 clone 出來的物品沒有後續消費者，需要在這裡直接釋放。
        if (deathEntity == null)
            return;

        for (ItemStack item : backpackItems) {
            if (isDropItem(item))
                deathEntity.getWorld().dropItemNaturally(deathEntity.getLocation(), item);
        }
    }

    static boolean shouldReleaseBackpackItemsAtDeathLocation(boolean timeBombEnabled, boolean swapInventoryEnabled, boolean hasKiller) {
        return !timeBombEnabled && !(swapInventoryEnabled && hasKiller);
    }

    static List<ItemStack> collectDropItems(ItemStack[] items) {
        List<ItemStack> drops = new ArrayList<>();

        if (items == null)
            return drops;

        for (ItemStack item : items) {
            if (isDropItem(item))
                drops.add(item.clone());
        }

        return drops;
    }

    private static boolean isDropItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
}
