package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.application.world.PreviewWorldGenerationService;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.settings.UHCFiles;

public class CenterCleanerMenu extends PluginMenu {
    private static final String SECTION = "Center_Cleaner";
    private static final String AGREE_BUTTON = "Agree";
    private static final String DISAGREE_BUTTON = "Disagree";
    private static final int BACK_OFFSET = 1;

    private final PreviewWorldGenerationService previewWorldGeneration = new PreviewWorldGenerationService();
    private final String seed;

    public CenterCleanerMenu() {
        this(null);
    }

    public CenterCleanerMenu(String seed) {
        super(PluginMenuSection.of(SECTION));
        this.seed = seed;
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getBackButtonSlot())
            return PluginItems.fromConfig(UHCFiles.MENUS, "Leave");

        if (slot == getSection().getButtonSlot(AGREE_BUTTON))
            return getSection().getButtonItem(AGREE_BUTTON);

        if (slot == getSection().getButtonSlot(DISAGREE_BUTTON))
            return getSection().getButtonItem(DISAGREE_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getBackButtonSlot()) {
            new MainSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(AGREE_BUTTON)) {
            createWorld(player, true);
            return;
        }

        if (slot == getSection().getButtonSlot(DISAGREE_BUTTON))
            createWorld(player, false);
    }

    private void createWorld(Player player, boolean centerCleanerEnabled) {
        player.closeInventory();
        previewWorldGeneration.create(player, centerCleanerEnabled, seed);
    }

    private int getBackButtonSlot() {
        return getSection().getSize() - BACK_OFFSET;
    }
}
