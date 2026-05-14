package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.application.world.PreviewWorldGenerationService;
import org.mcwonderland.uhc.menu.UHCMenuSection;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.config.ConfigConfirmMenu;

public class CenterCleanerMenu extends ConfigConfirmMenu {

    private final PreviewWorldGenerationService previewWorldGeneration = new PreviewWorldGenerationService();
    private String seed;

    public CenterCleanerMenu(Menu parent) {
        this(parent, null);
    }

    public CenterCleanerMenu(Menu parent, String seed) {
        super(UHCMenuSection.of("Center_Cleaner"), parent);
        this.seed = seed;
    }

    @Override
    protected void onAgree(Player player, Menu menu) {
        createWorld(player, true);
    }

    @Override
    protected void onDisagree(Player player, Menu menu) {
        createWorld(player, false);
    }

    private void createWorld(Player player, boolean centerCleanerEnabled) {
        player.closeInventory();
        previewWorldGeneration.create(player, centerCleanerEnabled, seed);
    }

}
