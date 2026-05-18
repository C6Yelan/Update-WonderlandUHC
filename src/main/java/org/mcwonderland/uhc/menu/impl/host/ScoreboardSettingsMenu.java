package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.sub.UHCScoreboardSettings;
import org.mcwonderland.uhc.menu.model.ColorPickerMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginColor;

public class ScoreboardSettingsMenu extends PluginMenu {
    private static final String SECTION = "Scoreboard";
    private static final String THEMES_BUTTON = "Themes";
    private static final String UPDATE_TICKS_BUTTON = "Update_Ticks";
    private static final String HEART_COLOR_BUTTON = "Heart_Color";

    private final UHCScoreboardSettings scoreboardSettings;

    public ScoreboardSettingsMenu() {
        super(PluginMenuSection.of(SECTION));
        this.scoreboardSettings = Game.getSettings().getScoreboardSettings();
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getSection().getButtonSlot(THEMES_BUTTON))
            return getSection().getButtonItem(THEMES_BUTTON, "{theme}", scoreboardSettings.getSidebarTheme().getName());

        if (slot == getSection().getButtonSlot(UPDATE_TICKS_BUTTON))
            return getSection().getButtonItem(UPDATE_TICKS_BUTTON, "{count}", scoreboardSettings.getScoreboardUpdateTick());

        if (slot == getSection().getButtonSlot(HEART_COLOR_BUTTON))
            return getSection().getButtonItem(HEART_COLOR_BUTTON, "{color}", scoreboardSettings.getHeartColor());

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getSection().getButtonSlot(THEMES_BUTTON)) {
            new SidebarThemeSettingsMenu(player).displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(UPDATE_TICKS_BUTTON)) {
            updateTicks(player, click);
            return;
        }

        if (slot == getSection().getButtonSlot(HEART_COLOR_BUTTON))
            openChooseHeartColorMenu(player);
    }

    private void updateTicks(Player player, ClickType click) {
        if (click == ClickType.LEFT)
            scoreboardSettings.setScoreboardUpdateTick(Math.max(1, scoreboardSettings.getScoreboardUpdateTick() - 1));
        else if (click == ClickType.RIGHT)
            scoreboardSettings.setScoreboardUpdateTick(scoreboardSettings.getScoreboardUpdateTick() + 1);
        else
            return;

        displayTo(player);
    }

    private void openChooseHeartColorMenu(Player player) {
        new ColorPickerMenu(returningPlayer -> new ScoreboardSettingsMenu().displayTo(returningPlayer)) {
            @Override
            protected void onChooseColor(Player player, PluginColor color) {
                scoreboardSettings.setHeartColor(color);
            }
        }.displayTo(player);
    }
}
