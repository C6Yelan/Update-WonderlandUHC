package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.scenario.ScenarioManager;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 2019-12-10 上午 01:00
 */
public class ScenarioSettingsMenu extends PluginPagedMenu<Scenario> {
    private static final String SECTION = "Scenarios";
    private static final String CLEAR_SCENARIOS_BUTTON = "Clear_Scenarios";
    private static final int BACK_OFFSET = 1;
    private static final int CLEAR_SCENARIOS_OFFSET = 9;

    private final ScenarioManager manager;

    public ScenarioSettingsMenu(ScenarioManager manager) {
        super(PluginMenuSection.of(SECTION), manager.getScenarios());
        this.manager = manager;
    }

    @Override
    protected ItemStack convertToItemStack(Scenario scenario) {
        ItemStack item = new ItemStack(scenario.getIcon());
        appendStatusLore(item, scenario);

        if (scenario.isEnabled())
            applyGlow(item);

        return item;
    }

    @Override
    protected void onPageClick(Player player, Scenario scenario, ClickType clickType) {
        boolean toggled = manager.toggleScenario(scenario, !scenario.isEnabled());
        if (!toggled)
            return;

        Chat.broadcast((scenario.isEnabled() ?
                Messages.Host.SCENARIO_ENABLED_PLAYER : Messages.Host.SCENARIO_DISABLED_PLAYER)
                .replace("{player}", player.getName())
                .replace("{scenario}", scenario.getFancyName()));

        Extra.sound(Sounds.Host.SCENARIO_TOGGLED);
        refreshMenu(player);
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getBackButtonSlot())
            return PluginItems.fromConfig(UHCFiles.MENUS, "Leave");

        if (slot == getClearScenariosButtonSlot())
            return getSection().getButtonItem(CLEAR_SCENARIOS_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getBackButtonSlot()) {
            new MainSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getClearScenariosButtonSlot()) {
            clearScenarios(player);
            return;
        }

        super.onClick(player, slot, click, clicked);
    }

    private void clearScenarios(Player player) {
        manager.getEnabledScenarios().forEach(scenario -> manager.toggleScenario(scenario, false));
        Game.getSettings().getScenarios().clear();
        Extra.sound(player, Sounds.Host.CLEAR_ENABLED_SCENARIOS);
        refreshMenu(player);
    }

    private void refreshMenu(Player player) {
        new ScenarioSettingsMenu(manager).displayTo(player);
    }

    private int getClearScenariosButtonSlot() {
        return getSection().getSize() - CLEAR_SCENARIOS_OFFSET;
    }

    private int getBackButtonSlot() {
        return getSection().getSize() - BACK_OFFSET;
    }

    private void appendStatusLore(ItemStack item, Scenario scenario) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null)
            return;

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(PluginText.toItemComponent(scenario.isEnabled() ? Messages.ENABLED : Messages.DISABLED));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
    }

    private void applyGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null)
            return;

        meta.setEnchantmentGlintOverride(Boolean.TRUE);
        item.setItemMeta(meta);
    }
}
