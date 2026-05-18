package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.application.world.PreviewWorldGenerationService;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;

public class CenterCleanerMenu extends PluginMenu {
    private static final String SECTION = "Center_Cleaner";
    private static final String AGREE_BUTTON = "Agree";
    private static final String DISAGREE_BUTTON = "Disagree";

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
        if (slot == getSection().getButtonSlot(AGREE_BUTTON))
            return getSection().getButtonItem(AGREE_BUTTON);

        if (slot == getSection().getButtonSlot(DISAGREE_BUTTON))
            return getSection().getButtonItem(DISAGREE_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
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
}
