package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.settings.SavedGameSettingsCache;
import org.mcwonderland.uhc.model.GamePlaceholderReplacer;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.util.List;

public class SavedSettingsMenu extends PluginPagedMenu<UHCGameSettings> {
    private static final String SECTION = "Saves";
    private static final String SAVE_AS_BUTTON = "Save_As";
    private static final String SAVED_BUTTON = "Saved";
    private static final int SAVE_AS_OFFSET = 9;
    private static final int BACK_OFFSET = 1;

    protected SavedSettingsMenu(Player player) {
        super(PluginMenuSection.of(SECTION), SavedGameSettingsCache.getSavedSettings(player));
    }


    @Override
    protected ItemStack convertToItemStack(UHCGameSettings settings) {
        ItemStack baseItem = getSection().getButtonItem(
                SAVED_BUTTON,
                "{saved_game_title}", settings.getTitle()
        );

        return PluginItems.create(
                baseItem,
                PluginText.replaceToString(getSection().getButtonName(SAVED_BUTTON), "{saved_game_title}", settings.getTitle()),
                GamePlaceholderReplacer.replace(getSection().getButtonLore(SAVED_BUTTON), settings),
                true
        );
    }

    @Override
    protected void onPageClick(Player player, UHCGameSettings settings, ClickType clickType) {
        switch (clickType) {
            case LEFT:
                loadSettings(settings);
                refreshAndOpenMainMenu(player);
                break;
            case MIDDLE:
                replaceSettings(player, settings);
                break;
            case RIGHT:
                deleteSavedSettings(player, settings);
                refreshMenu(player);
                break;
            default:
                break;
        }
    }

    private void replaceSettings(Player player, UHCGameSettings settings) {
        List<UHCGameSettings> savedSettings = SavedGameSettingsCache.getSavedSettings(player);

        savedSettings.set(savedSettings.indexOf(settings), Game.getSettings());
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getSaveAsButtonSlot())
            return getSection().getButtonItem(SAVE_AS_BUTTON);

        if (slot == getBackButtonSlot())
            return PluginItems.fromConfig(UHCFiles.MENUS, "Leave");

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getSaveAsButtonSlot()) {
            saveCurrentSettings(player);
            refreshMenu(player);
            return;
        }

        if (slot == getBackButtonSlot()) {
            refreshAndOpenMainMenu(player);
            return;
        }

        super.onClick(player, slot, click, clicked);
    }

    private void refreshAndOpenMainMenu(Player player) {
        new MainSettingsMenu().displayTo(player);
    }


    private void loadSettings(UHCGameSettings settings) {
        Game.changeSettings(settings.clone());
    }

    private void deleteSavedSettings(Player player, UHCGameSettings settings) {
        SavedGameSettingsCache.getSavedSettings(player).remove(settings);
        SavedGameSettingsCache.saveGameSettings(player);
    }

    private void saveCurrentSettings(Player player) {
        SavedGameSettingsCache.getSavedSettings(player).add(Game.getSettings().clone());
        SavedGameSettingsCache.saveGameSettings(player);
    }

    private void refreshMenu(Player player) {
        new SavedSettingsMenu(player).displayTo(player);
    }

    private int getSaveAsButtonSlot() {
        return getSection().getSize() - SAVE_AS_OFFSET;
    }

    private int getBackButtonSlot() {
        return getSection().getSize() - BACK_OFFSET;
    }
}
