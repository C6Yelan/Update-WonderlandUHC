package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.border.BorderType;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.game.settings.sub.UHCBorderSettings;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.Chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BorderSettingsMenu extends PluginMenu {
    private static final Map<UUID, InputSession> inputSessions = new ConcurrentHashMap<>();
    private static final String SECTION = "Border";
    private static final String SIZE_BUTTON = "Size";
    private static final String NETHER_SIZE_BUTTON = "Nether_Size";
    private static final String BORDER_TYPE_BUTTON = "Border_Type";
    private static final String FINAL_SIZE_BUTTON = "Final_Size_Of_Shrink_Mode_Border";
    private static final String BORDER_SHRINK_SPEED_BUTTON = "Border_Shrink_Speed";
    private static final String SHRINK_CALCULATOR_BUTTON = "Shrink_Calculator";

    public BorderSettingsMenu() {
        super(PluginMenuSection.of(SECTION));
    }

    public static boolean handleInput(Player player, String input) {
        InputSession session = inputSessions.get(player.getUniqueId());

        if (session == null)
            return false;

        Bukkit.getScheduler().runTask(WonderlandUHC.getInstance(), () -> session.accept(player, input));
        return true;
    }

    public static void clear(Player player) {
        inputSessions.remove(player.getUniqueId());
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        UHCBorderSettings borderSettings = Game.getSettings().getBorderSettings();

        if (slot == getSection().getButtonSlot(SIZE_BUTTON))
            return getSection().getButtonItem(SIZE_BUTTON, "{number}", borderSettings.getInitialBorder());

        if (slot == getSection().getButtonSlot(NETHER_SIZE_BUTTON))
            return getSection().getButtonItem(NETHER_SIZE_BUTTON, "{number}", borderSettings.getInitialNetherBorder());

        if (slot == getSection().getButtonSlot(BORDER_TYPE_BUTTON))
            return getSection().getButtonItem(BORDER_TYPE_BUTTON, "{type}", borderSettings.getBorderType().fancyName());

        if (slot == getSection().getButtonSlot(FINAL_SIZE_BUTTON))
            return getSection().getButtonItem(FINAL_SIZE_BUTTON, "{number}", borderSettings.getFinalSizeOfShrinkModeBorder());

        if (slot == getSection().getButtonSlot(BORDER_SHRINK_SPEED_BUTTON))
            return getSection().getButtonItem(
                    BORDER_SHRINK_SPEED_BUTTON,
                    "{number}", borderSettings.getBorderShrinkSpeed(),
                    "{fancy-time}", PluginText.replaceTimeToString("{fancy-time}", BorderUtil.getShrinkSecondsCost())
            );

        if (slot == getSection().getButtonSlot(SHRINK_CALCULATOR_BUTTON))
            return getSection().getButtonItem(SHRINK_CALCULATOR_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        UHCBorderSettings borderSettings = Game.getSettings().getBorderSettings();

        if (slot == getSection().getButtonSlot(SIZE_BUTTON)) {
            startIntegerInput(player, Messages.Editor.Number.BorderSize.MESSAGE,
                    Messages.Editor.Number.BorderSize.SAVED, borderSettings::setInitialBorder);
            return;
        }

        if (slot == getSection().getButtonSlot(NETHER_SIZE_BUTTON)) {
            startIntegerInput(player, Messages.Editor.Number.NetherBorderSize.MESSAGE,
                    Messages.Editor.Number.NetherBorderSize.SAVED, borderSettings::setInitialNetherBorder);
            return;
        }

        if (slot == getSection().getButtonSlot(BORDER_TYPE_BUTTON)) {
            handleBorderTypeClick(player, click, borderSettings);
            return;
        }

        if (slot == getSection().getButtonSlot(FINAL_SIZE_BUTTON)) {
            startIntegerInput(player, Messages.Editor.Number.FinalSizeOrShrinkModeBorder.MESSAGE,
                    Messages.Editor.Number.FinalSizeOrShrinkModeBorder.SAVED, borderSettings::setFinalSizeOfShrinkModeBorder);
            return;
        }

        if (slot == getSection().getButtonSlot(BORDER_SHRINK_SPEED_BUTTON)) {
            startDoubleInput(player, Messages.Editor.Number.BorderShrinkSpeed.MESSAGE,
                    Messages.Editor.Number.BorderShrinkSpeed.SAVED, borderSettings::setBorderShrinkSpeed);
            return;
        }

        if (slot == getSection().getButtonSlot(SHRINK_CALCULATOR_BUTTON))
            startShrinkCalculatorInput(player, borderSettings);
    }

    private void handleBorderTypeClick(Player player, ClickType click, UHCBorderSettings borderSettings) {
        if (click == ClickType.LEFT) {
            setBorderType(player, borderSettings, BorderType.MOVING);
            return;
        }

        if (click == ClickType.RIGHT)
            setBorderType(player, borderSettings, BorderType.TIMER);
    }

    private void setBorderType(Player player, UHCBorderSettings borderSettings, BorderType type) {
        borderSettings.setBorderType(type);
        saveCurrentSettings();
        Chat.broadcast(Messages.Host.BORDER_TYPE_CHANGED
                .replace("{player}", player.getName())
                .replace("{type}", type.fancyName()));
        displayTo(player);
    }

    private void startIntegerInput(Player player, String prompt, String savedMessage, Consumer<Integer> saveInput) {
        startInput(player, prompt, (inputPlayer, input) -> {
            Integer number = parseInteger(input);

            if (number == null) {
                Chat.sendConversing(inputPlayer, Messages.Editor.Number.INVALID_NUMBER);
                return;
            }

            inputSessions.remove(inputPlayer.getUniqueId());
            saveInput.accept(number);
            saveCurrentSettings();
            Chat.sendConversing(inputPlayer, savedMessage.replace("{number}", number + ""));
        });
    }

    private void startDoubleInput(Player player, String prompt, String savedMessage, Consumer<Double> saveInput) {
        startInput(player, prompt, (inputPlayer, input) -> {
            Double number = parseDouble(input);

            if (number == null) {
                Chat.sendConversing(inputPlayer, Messages.Editor.Number.INVALID_NUMBER);
                return;
            }

            inputSessions.remove(inputPlayer.getUniqueId());
            saveInput.accept(number);
            saveCurrentSettings();
            Chat.sendConversing(inputPlayer, savedMessage.replace("{number}", number + ""));
        });
    }

    private void startShrinkCalculatorInput(Player player, UHCBorderSettings borderSettings) {
        String prompt = Messages.Editor.Time.ShrinkCalculator.MESSAGE
                .replace("{init}", borderSettings.getInitialBorder() + "")
                .replace("{final}", borderSettings.getFinalSizeOfShrinkModeBorder() + "");

        startInput(player, prompt, (inputPlayer, input) -> {
            Integer seconds = parseToSeconds(input);

            if (seconds == null) {
                Chat.sendConversing(inputPlayer, Messages.Editor.Time.INVALID_TIME);
                return;
            }

            double speed = BorderUtil.getShrinkSpeedPerSecond(seconds);

            inputSessions.remove(inputPlayer.getUniqueId());
            borderSettings.setBorderShrinkSpeed(speed);
            saveCurrentSettings();
            Chat.sendConversing(inputPlayer, Messages.Editor.Time.ShrinkCalculator.SAVED
                    .replace("{speed}", speed + ""));
        });
    }

    private void startInput(Player player, String prompt, InputSession session) {
        if (inputSessions.containsKey(player.getUniqueId())) {
            Chat.send(player, "&c目前已有正在等待的設定輸入。");
            return;
        }

        inputSessions.put(player.getUniqueId(), session);
        player.closeInventory();
        Chat.sendConversing(player, prompt);
    }

    private static void saveCurrentSettings() {
        CacheSaver.saveCache();
    }

    private Integer parseInteger(String input) {
        try {
            return Integer.valueOf(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseDouble(String input) {
        try {
            return Double.valueOf(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseToSeconds(String input) {
        String[] parts = input.split(":");

        if (parts.length > 3)
            return null;

        int totalSeconds = 0;
        int multiple = 1;

        for (int i = parts.length - 1; i >= 0; i--) {
            int value;

            try {
                value = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                return null;
            }

            totalSeconds += value * multiple;
            multiple *= 60;
        }

        return totalSeconds <= 0 ? null : totalSeconds;
    }

    private interface InputSession {
        void accept(Player player, String input);
    }
}
