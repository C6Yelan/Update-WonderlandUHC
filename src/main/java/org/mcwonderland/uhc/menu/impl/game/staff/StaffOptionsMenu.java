package org.mcwonderland.uhc.menu.impl.game.staff;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.staff.OreAlert;
import org.mcwonderland.uhc.game.player.staff.StaffOptions;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginText;

public class StaffOptionsMenu extends PluginMenu {
    private static final String SECTION = "Staff_Options";
    private static final String GOLD_ALERT_BUTTON = "Gold_Alert";
    private static final String DIAMOND_ALERT_BUTTON = "Diamond_Alert";
    private static final String TOGGLE_SPEC_SHOW_BUTTON = "Toggle_Spec_Show";
    private static final String TOGGLE_STAFF_SHOW_BUTTON = "Toggle_Staff_Show";
    private static final String MOVING_SPEED_BUTTON = "Moving_Speed";
    private static final Object ENABLED_STATUS = PluginText.formatted("<green>On</green>");
    private static final Object DISABLED_STATUS = PluginText.formatted("<red>Off</red>");

    private final UHCPlayer uhcPlayer;
    private final StaffOptions staffOptions;

    public StaffOptionsMenu(Player player) {
        super(PluginMenuSection.of(SECTION));
        this.uhcPlayer = UHCPlayer.getUHCPlayer(player);
        this.staffOptions = uhcPlayer.getStaffOptions();
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getSection().getButtonSlot(GOLD_ALERT_BUTTON))
            return getSection().getButtonItem(GOLD_ALERT_BUTTON, "{status}", getStatus(staffOptions.hasOreAlert(OreAlert.GOLD_ORE)));

        if (slot == getSection().getButtonSlot(DIAMOND_ALERT_BUTTON))
            return getSection().getButtonItem(DIAMOND_ALERT_BUTTON, "{status}", getStatus(staffOptions.hasOreAlert(OreAlert.DIAMOND_ORE)));

        if (slot == getSection().getButtonSlot(TOGGLE_SPEC_SHOW_BUTTON))
            return getSection().getButtonItem(TOGGLE_SPEC_SHOW_BUTTON, "{status}", getStatus(staffOptions.isShowSpectator()));

        if (slot == getSection().getButtonSlot(TOGGLE_STAFF_SHOW_BUTTON))
            return getSection().getButtonItem(TOGGLE_STAFF_SHOW_BUTTON, "{status}", getStatus(staffOptions.isShowStaff()));

        if (slot == getSection().getButtonSlot(MOVING_SPEED_BUTTON))
            return getSection().getButtonItem(MOVING_SPEED_BUTTON, "{count}", staffOptions.getSpeed());

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getSection().getButtonSlot(GOLD_ALERT_BUTTON)) {
            toggleOreAlert(player, OreAlert.GOLD_ORE);
            return;
        }

        if (slot == getSection().getButtonSlot(DIAMOND_ALERT_BUTTON)) {
            toggleOreAlert(player, OreAlert.DIAMOND_ORE);
            return;
        }

        if (slot == getSection().getButtonSlot(TOGGLE_SPEC_SHOW_BUTTON)) {
            staffOptions.setShowSpectator(!staffOptions.isShowSpectator());
            uhcPlayer.checkHide();
            displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(TOGGLE_STAFF_SHOW_BUTTON)) {
            staffOptions.setShowStaff(!staffOptions.isShowStaff());
            uhcPlayer.checkHide();
            displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(MOVING_SPEED_BUTTON))
            updateSpeed(player, click);
    }

    private void toggleOreAlert(Player player, OreAlert oreAlert) {
        staffOptions.toggleOreAlert(oreAlert);
        displayTo(player);
    }

    private void updateSpeed(Player player, ClickType click) {
        if (click == ClickType.LEFT)
            staffOptions.setSpeed(Math.max(1, staffOptions.getSpeed() - 1));
        else if (click == ClickType.RIGHT)
            staffOptions.setSpeed(Math.min(5, staffOptions.getSpeed() + 1));
        else
            return;

        player.setWalkSpeed(staffOptions.getMCSpeed());
        player.setFlySpeed(staffOptions.getMCSpeed());
        displayTo(player);
    }

    private Object getStatus(boolean enabled) {
        return enabled ? ENABLED_STATUS : DISABLED_STATUS;
    }
}
