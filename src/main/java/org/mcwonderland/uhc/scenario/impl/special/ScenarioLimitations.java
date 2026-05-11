package org.mcwonderland.uhc.scenario.impl.special;

import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.model.SimpleSound;

import java.util.List;
import java.util.UUID;

/**
 * 2019-12-12 上午 11:35
 */
public class ScenarioLimitations extends ConfigBasedScenario implements Listener {

    private static final StrictMap<UUID, OreMined> blockMines = new StrictMap<>();
    private final StrictMap<Material, Integer> limitedBlocks = new StrictMap<>();

    @FilePath(name = "Reached_Limit")
    private String reachedLimitMsg;
    @FilePath(name = "Cant_Mine_More")
    private String cantMineMore;

    @FilePath(name = "Reached_Limit_Sound")
    private SimpleSound reachedLimitSound;
    @FilePath(name = "Cant_Mine_More_Sound")
    private SimpleSound cantMineMoreSound;

    @FilePath(name = "Limits.Diamond")
    private Integer diomandMax;
    @FilePath(name = "Limits.Gold")
    private Integer goldMax;
    @FilePath(name = "Limits.Iron")
    private Integer ironMax;


    public ScenarioLimitations(ScenarioName name) {
        super(name);
    }

    @Override
    protected void onConfigReload() {
        limitedBlocks.clear();
        limitedBlocks.put(Material.DIAMOND_ORE, diomandMax);
        limitedBlocks.put(Material.GOLD_ORE, goldMax);
        limitedBlocks.put(Material.IRON_ORE, ironMax);
    }

    @EventHandler
    protected void onBlockBreak(UHCBlockBreakEvent e) {
        try {
            Material blockType = OreRuleSupport.canonicalLimitedOre(e.getBlockType());

            if (blockHasLimit(blockType)) {
                int limit = limitedBlocks.get(blockType);
                int playerMined = getMineAmount(e.getPlayer(), blockType);

                if (playerMined >= limit)
                    dropNothing(e);
                else
                    addBlockMines(e.getPlayer(), blockType);
            }
        } catch (RuntimeException | LinkageError ex) {
            LegacyFoundationAdapter.error(
                    ex,
                    "Scenario 'Limitations' failed while handling a block break event.",
                    "The scenario was disabled for this run, but the block break flow will continue."
            );
            disableAfterRuntimeFailure();
        }
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            LegacyFoundationAdapter.error(
                    disableEx,
                    "Scenario 'Limitations' could not be disabled after a runtime failure."
            );
        }
    }

    private boolean blockHasLimit(Material blockType) {
        return limitedBlocks.containsKey(blockType);
    }

    private void dropNothing(UHCBlockBreakEvent e) {
        e.getDrops().clear();
        e.setExpToDrop(0);

        tellCantMine(e.getPlayer(), OreRuleSupport.canonicalLimitedOre(e.getBlockType()));
    }

    private void tellCantMine(Player player, Material blockType) {
        Chat.send(player, reachedLimitMsg
                .replace("{amount}", limitedBlocks.get(blockType) + "")
                .replace("{block}", blockType.name())
        );

        Extra.sound(player, cantMineMoreSound);
    }

    private void addBlockMines(Player player, Material blockType) {
        blockMines.get(player.getUniqueId()).addMinedAmount(blockType);

        if (getMineAmount(player, blockType) == limitedBlocks.get(blockType))
            tellReachLimit(player, blockType);

    }

    private void tellReachLimit(Player player, Material blockType) {
        Chat.send(player, cantMineMore
                .replace("{block}", blockType.name())
                .replace("{amount}", limitedBlocks.get(blockType) + ""));

        Extra.sound(player, reachedLimitSound);
    }


    private int getMineAmount(Player player, Material blockType) {
        UUID uuid = player.getUniqueId();

        OreMined oreMined = blockMines.getOrPut(uuid, new OreMined());

        return oreMined.getBlockMined(blockType);
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list,
                "{diamond-limit}", getLimit(Material.DIAMOND_ORE),
                "{gold-limit}", getLimit(Material.GOLD_ORE),
                "{iron-limit}", getLimit(Material.IRON_ORE));
    }

    private int getLimit(Material material) {
        return limitedBlocks.get(material);
    }

    private class OreMined {
        private final StrictMap<Material, Integer> mined = new StrictMap<>();

        public int getBlockMined(Material material) {
            return mined.getOrDefault(material, 0);
        }

        public void addMinedAmount(Material material) {
            int newAmount = getBlockMined(material) + 1;

            mined.override(material, newAmount);
        }
    }
}
