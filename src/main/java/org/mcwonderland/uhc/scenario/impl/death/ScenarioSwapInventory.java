package org.mcwonderland.uhc.scenario.impl.death;

import org.mcwonderland.uhc.events.UHCGamingDeathEvent;
import org.mcwonderland.uhc.game.CombatRelog;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.model.InventoryContent;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            LegacyFoundationAdapter.error(
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

        swapInventory(player, killer, e.getDrops(), relog);
    }

    private void swapInventory(Player deathPlayer, Player killer, List<ItemStack> drops, CombatRelog relog) {
        PlayerInventory deathPlayerInv = deathPlayer.getInventory();
        PlayerInventory killerInv = killer.getInventory();

        InventoryContent deathPlayerInvContent = new InventoryContent(deathPlayerInv);
        InventoryContent killerInvContent = new InventoryContent(killerInv);
        InventoryContent relogInvContent = relog == null ? null : relog.getInventoryContent();
        ItemStack[] deathDropItems = relogInvContent == null ? deathPlayerInvContent.getAllItems() : relogInvContent.getAllItems();
        List<ItemStack> originalDrops = new ArrayList<>(drops);
        List<ItemStack> swappedDrops = buildSwappedDrops(originalDrops, deathDropItems, killerInvContent.getAllItems());

        try {
            if (relog != null)
                relog.setInventoryContent(killerInvContent);

            killerInvContent.setContents(deathPlayerInv);
            deathPlayerInvContent.setContents(killerInv);

            replaceDrops(drops, swappedDrops);
        } catch (RuntimeException | LinkageError ex) {
            rollbackSwap(deathPlayerInv, killerInv, relog, deathPlayerInvContent, killerInvContent, relogInvContent, originalDrops, drops);
            throw ex;
        }
    }

    static <T> List<T> buildSwappedDrops(List<T> currentDrops, T[] deathPlayerItems, T[] killerItems) {
        List<T> swappedDrops = new ArrayList<>(currentDrops);
        swappedDrops.removeAll(Arrays.asList(deathPlayerItems));
        swappedDrops.addAll(Arrays.asList(killerItems));
        return swappedDrops;
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
            LegacyFoundationAdapter.error(
                    rollbackEx,
                    "Scenario 'Swap_Inventory' could not fully roll back a failed inventory swap."
            );
        }
    }

    private static void replaceDrops(List<ItemStack> drops, List<ItemStack> replacement) {
        drops.clear();
        drops.addAll(replacement);
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            LegacyFoundationAdapter.error(
                    disableEx,
                    "Scenario 'Swap_Inventory' could not be disabled after a runtime failure."
            );
        }
    }
}
