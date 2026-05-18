package org.mcwonderland.uhc.model;

import org.mcwonderland.uhc.platform.text.PluginText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.settings.sub.UHCItemSettings;
import org.mcwonderland.uhc.game.settings.sub.UHCTimerSettings;
import org.mcwonderland.uhc.scenario.ScenarioManager;
import org.mcwonderland.uhc.settings.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 2019-11-21 下午 04:20
 */
public class GamePlaceholderReplacer {
    private static final String DELIMITER = "\n";

    private String messages;
    private final UHCGameSettings settings;

    private GamePlaceholderReplacer(List<String> messages, UHCGameSettings settings) {
        this.messages = String.join(DELIMITER, messages);
        this.settings = settings;
    }

    public static List<String> replace(List<String> list) {
        return replace(list, Game.getSettings());
    }

    public static List<String> replace(List<String> list, UHCGameSettings settings) {
        List<String> toReplace = new ArrayList<>(list);

        GamePlaceholderReplacer replacer = new GamePlaceholderReplacer(toReplace, settings);

        return replacer.replacePlaceholders();
    }

    public List<String> replacePlaceholders() {
        replaceCommons();
        replaceTimers();
        replaceGameSettings();
        replaceScenarios();
        replaceItems();

        return buildList();
    }


    private void replaceCommons() {
        replace("{host}", Game.getGame().getHost());
        replace("{title}", settings.getTitle());
        replace("{team-size}", settings.getTeamSettings().getTeamSize());
    }

    private void replaceTimers() {
        UHCTimerSettings timer = settings.getTimer();

        replace("{invisible-time}", formatTime(timer.getDamageTime()));
        replace("{finalheal-time}", formatTime(timer.getHealTime()));
        replace("{pvp-time}", formatTime(timer.getPvpTime()));
        replace("{disable-nether-time}", formatTime(timer.getDisableNetherTime()));
        replace("{firstborder-time}", formatTime(timer.getBorderShrinkTime()));
    }

    private void replaceGameSettings() {
        replace("{border-size}", settings.getBorderSettings().getInitialBorder());
        replace("{apple-rate}", settings.getAppleRate());
        replace("{nether-on}", settings.isUsingNether());
        replace("{start-level}", settings.getInitialExperience());
        replace("{friendly-fire}", settings.getTeamSettings().isAllowTeamFire());
        replace("{enderpearl-damage}", settings.isEnderPearlDamage());
        replace("{initial-xp}", settings.getInitialExperience());
    }

    private void replaceScenarios() {
        StringBuilder builder = new StringBuilder();
        ScenarioManager scenarioManager = WonderlandUHC.getInstance().getScenarioManager();

        settings.getScenarios().forEach(s -> {
            Scenario scenario = scenarioManager.getScenario(s);

            if (scenario != null)
                builder.append("\n  - " + scenario.getFancyName());
        });

        replace("{scenarios}", builder.toString());
    }

    private void replaceItems() {
        UHCItemSettings itemSettings = settings.getItemSettings();

        replace("{start-items}", formatItems(itemSettings.getCustomInventoryItems().getAllItems()));
        replace("{custom-drops}", formatItems(itemSettings.getCustomDrops()));
        replace("{disabled-items}", "disableitems");
    }

    private GamePlaceholderReplacer replace(String from, Object to) {
        Object replacement = to instanceof Boolean ? replaceBoolean(( Boolean ) to) : to;

        if (messages.contains(from))
            messages = messages.replace(from, replacement.toString());

        return this;
    }

    private List<String> buildList() {
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(messages.split(DELIMITER)));
        return list;
    }

    private String formatTime(Integer time) {
        return PluginText.formatTime(time);
    }

    private String formatItems(ItemStack[]... itemStacks) {
        List<ItemStack> list = new ArrayList<>();

        for (ItemStack[] itemStackArr : itemStacks) {
            if (itemStackArr != null)
                list.addAll(Arrays.asList(itemStackArr));
        }

        return formatItems(list);
    }

    private String formatItems(List<ItemStack> itemStacks) {
        return itemStacks.stream()
                .filter(itemStack -> itemStack != null && itemStack.getItemMeta() != null)
                .map(itemStack -> "\n   - " + formatItemStack(itemStack))
                .collect(Collectors.joining());
    }

    private String formatItemStack(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        String name = getDisplayName(meta);
        Material type = itemStack.getType();

        String finalName = (name == null || name.isEmpty()) ? PluginText.bountifyCapitalized(type) : name;

        return finalName + " x" + itemStack.getAmount();
    }

    private String getDisplayName(ItemMeta meta) {
        Component displayName = meta.displayName();
        return displayName == null ? "" : LegacyComponentSerializer.legacySection().serialize(displayName);
    }


    private String replaceBoolean(boolean bool) {
        return bool ? Messages.ENABLED : Messages.DISABLED;
    }
}
