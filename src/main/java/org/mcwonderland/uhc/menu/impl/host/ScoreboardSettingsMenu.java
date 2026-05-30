package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.sub.UHCScoreboardSettings;
import org.mcwonderland.uhc.menu.model.ColorPickerMenu;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginColor;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.util.Extra;

import java.util.Locale;

public class ScoreboardSettingsMenu extends PluginMenu {
    private static final String SECTION = "Scoreboard";
    private static final String UPDATE_TICKS_BUTTON = "Update_Ticks";
    private static final String HEART_COLOR_BUTTON = "Heart_Color";
    private static final int BACK_OFFSET = 1;

    private final UHCScoreboardSettings scoreboardSettings;

    public ScoreboardSettingsMenu() {
        super(PluginMenuSection.of(SECTION));
        this.scoreboardSettings = Game.getSettings().getScoreboardSettings();
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getBackButtonSlot())
            return PluginItems.fromConfig(UHCFiles.MENUS, "Leave");

        if (slot == getSection().getButtonSlot(UPDATE_TICKS_BUTTON))
            return getSection().getButtonItem(UPDATE_TICKS_BUTTON, "{count}", scoreboardSettings.getScoreboardUpdateTick());

        if (slot == getSection().getButtonSlot(HEART_COLOR_BUTTON))
            return getSection().getButtonItem(HEART_COLOR_BUTTON, "{color}", scoreboardSettings.getHeartColor().name().toLowerCase(Locale.ROOT));

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getBackButtonSlot()) {
            new MainSettingsMenu().displayTo(player);
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

        Extra.sound(player, Sounds.Host.SCENARIO_TOGGLED);
        saveCurrentSettings();
        displayTo(player);
    }

    private void openChooseHeartColorMenu(Player player) {
        new ColorPickerMenu(returningPlayer -> new ScoreboardSettingsMenu().displayTo(returningPlayer)) {
            @Override
            protected void onChooseColor(Player player, PluginColor color) {
                scoreboardSettings.setHeartColor(color);
                saveCurrentSettings();
                Extra.sound(player, Sounds.Host.SCENARIO_TOGGLED);
            }
        }.displayTo(player);
    }

    private int getBackButtonSlot() {
        return getSection().getSize() - BACK_OFFSET;
    }

    private static void saveCurrentSettings() {
        WorldLoadingCacheState.saveCache();
    }
}
