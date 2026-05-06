package org.mcwonderland.uhc.menu.model;

import lombok.RequiredArgsConstructor;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.InventorySaver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.menu.button.config.conversation.ConfigInventoryEditorButton;
import org.mineacademy.fo.menu.config.ItemPath;

public abstract class InventoryEditButton extends ConfigInventoryEditorButton {
    private final String TO_HEAD_INPUT = "tohead";

    private final InventorySaver.SaveType saveType;

    protected InventoryEditButton(ItemPath path) {
        super(path);

        this.saveType = getSaveType();
    }

    protected abstract InventorySaver.SaveType getSaveType();

    @Override
    protected void loadInventory(PlayerInventory playerInventory) {
        InventorySaver.setContents(getPlayer(), saveType);
    }

    @Override
    protected void onStart() {
        Player player = getPlayer();

        Chat.sendConversing(player, getMessage());
        Chat.sendConversing(player, "");
        tellComponents(player);
    }

    protected abstract String getMessage();

    protected void tellComponents(Player player) {
        LegacyFoundationAdapter.sendRunCommandComponent(player, Messages.Editor.Inventory.CLICK_TO_HEAD, TO_HEAD_INPUT);
        LegacyFoundationAdapter.sendRunCommandComponent(player, Messages.Editor.CLICK_FINISH, FINISH_INPUT);
    }

    @Override
    protected boolean isInputValid(String input) {
        switch (input) {
            case TO_HEAD_INPUT:
                GoldenHeadChanger.changeHandItemToHead(getPlayer());
                return false;
        }

        return super.isInputValid(input);
    }


    @Override
    public void onSave(PlayerInventory playerInventory) {
        Player player = getPlayer();

        Chat.sendConversing(player, getSavedMessage());
        InventorySaver.saveInventoryData(player, saveType);
        Extra.sound(player, Sounds.Host.INVENTORY_EDITED);
    }

    protected abstract String getSavedMessage();

    @RequiredArgsConstructor
    private static class GoldenHeadChanger {
        private final Player player;

        private static void changeHandItemToHead(Player player) {
            new GoldenHeadChanger(player).change();
        }

        private void change() {
            if (player.getItemInHand().getType() == Material.GOLDEN_APPLE)
                changeGoldenAppleToGoldenHead();
            else
                Chat.sendConversing(player, Messages.Editor.Inventory.TO_HEAD_FAILED);
        }

        private void changeGoldenAppleToGoldenHead() {
            ItemMeta meta = player.getItemInHand().getItemMeta();
            meta.setDisplayName(Settings.Misc.GOLDEN_HEAD_NAME);
            player.getItemInHand().setItemMeta(meta);
            Extra.sound(player, Sounds.Host.GOLDEN_HEAD_CREATED);
        }
    }
}
