package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.game.settings.sub.UHCTimerSettings;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class TimeSettingsMenu extends PluginMenu {
    private static final Map<UUID, TimeInputSession> inputSessions = new ConcurrentHashMap<>();
    private static final String SECTION = "Times";
    private static final String DAMAGE_BUTTON = "Damage";
    private static final String FINAL_HEAL_BUTTON = "Final_Heal";
    private static final String PVP_BUTTON = "Pvp";
    private static final String BORDER_SHRINK_BUTTON = "Border_Shrink";
    private static final String DISABLE_NETHER_BUTTON = "Disable_Nether";

    public TimeSettingsMenu() {
        super(PluginMenuSection.of(SECTION));
    }

    public static boolean handleInput(Player player, String input) {
        TimeInputSession session = inputSessions.get(player.getUniqueId());

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
        UHCTimerSettings timer = Game.getSettings().getTimer();

        if (slot == getSection().getButtonSlot(DAMAGE_BUTTON))
            return getSection().getButtonItem(DAMAGE_BUTTON, "{time}", PluginText.formatTime(timer.getDamageTime()));

        if (slot == getSection().getButtonSlot(FINAL_HEAL_BUTTON))
            return getSection().getButtonItem(FINAL_HEAL_BUTTON, "{time}", PluginText.formatTime(timer.getHealTime()));

        if (slot == getSection().getButtonSlot(PVP_BUTTON))
            return getSection().getButtonItem(PVP_BUTTON, "{time}", PluginText.formatTime(timer.getPvpTime()));

        if (slot == getSection().getButtonSlot(BORDER_SHRINK_BUTTON))
            return getSection().getButtonItem(BORDER_SHRINK_BUTTON, "{time}", PluginText.formatTime(timer.getBorderShrinkTime()));

        if (slot == getSection().getButtonSlot(DISABLE_NETHER_BUTTON))
            return getSection().getButtonItem(DISABLE_NETHER_BUTTON, "{time}", PluginText.formatTime(timer.getDisableNetherTime()));

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        UHCTimerSettings timer = Game.getSettings().getTimer();

        if (slot == getSection().getButtonSlot(DAMAGE_BUTTON)) {
            startTimeInput(player, Messages.Editor.Time.Damage.MESSAGE, Messages.Editor.Time.Damage.SAVED,
                    timer::setDamageTime, timer::getDamageTime);
            return;
        }

        if (slot == getSection().getButtonSlot(FINAL_HEAL_BUTTON)) {
            startTimeInput(player, Messages.Editor.Time.FinalHeal.MESSAGE, Messages.Editor.Time.FinalHeal.SAVED,
                    timer::setHealTime, timer::getHealTime);
            return;
        }

        if (slot == getSection().getButtonSlot(PVP_BUTTON)) {
            startTimeInput(player, Messages.Editor.Time.Pvp.MESSAGE, Messages.Editor.Time.Pvp.SAVED,
                    timer::setPvpTime, timer::getPvpTime);
            return;
        }

        if (slot == getSection().getButtonSlot(BORDER_SHRINK_BUTTON)) {
            startTimeInput(player, Messages.Editor.Time.BorderShrink.MESSAGE, Messages.Editor.Time.BorderShrink.SAVED,
                    timer::setBorderShrinkTime, timer::getBorderShrinkTime);
            return;
        }

        if (slot == getSection().getButtonSlot(DISABLE_NETHER_BUTTON))
            startTimeInput(player, Messages.Editor.Time.DisableNether.MESSAGE, Messages.Editor.Time.DisableNether.SAVED,
                    timer::setDisableNetherTime, timer::getDisableNetherTime);
    }

    private void startTimeInput(Player player, String prompt, String savedMessage, IntConsumer saveInput,
                                IntSupplier currentTime) {
        if (inputSessions.containsKey(player.getUniqueId())) {
            Chat.send(player, "<red>目前已有正在等待的設定輸入。</red>");
            return;
        }

        inputSessions.put(player.getUniqueId(), new TimeInputSession(saveInput, currentTime, savedMessage));
        player.closeInventory();
        Chat.sendConversing(player, prompt);
    }

    private static void saveCurrentSettings() {
        CacheSaver.saveCache();
    }

    private static final class TimeInputSession {
        private final IntConsumer saveInput;
        private final IntSupplier currentTime;
        private final String savedMessage;

        private TimeInputSession(IntConsumer saveInput, IntSupplier currentTime, String savedMessage) {
            this.saveInput = saveInput;
            this.currentTime = currentTime;
            this.savedMessage = savedMessage;
        }

        private void accept(Player player, String input) {
            Integer seconds = parseToSeconds(input);

            if (seconds == null) {
                Chat.sendConversing(player, Messages.Editor.Time.INVALID_TIME);
                return;
            }

            inputSessions.remove(player.getUniqueId());
            saveInput.accept(seconds);
            saveCurrentSettings();
            Chat.sendConversing(player, PluginText.replaceTimeToString(savedMessage, currentTime.getAsInt()));
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
    }
}
