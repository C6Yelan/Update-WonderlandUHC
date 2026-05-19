package org.mcwonderland.uhc.menu.impl.game;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.menu.model.ColorPickerMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginColor;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.Chat;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TeamSettingsMenu extends PluginMenu {
    private static final Map<UUID, TextInputSession> inputSessions = new ConcurrentHashMap<>();
    private static final String SECTION = "Team_Settings";
    private static final String NAME_BUTTON = "Name";
    private static final String COLOR_BUTTON = "Color";
    private static final String CHARACTER_BUTTON = "Character";
    private static final String OPEN_JOIN_BUTTON = "Open_Join";
    private static final String HELP_BUTTON = "Help";
    private static final Object ENABLED_STATUS = PluginText.formatted("<green>On</green>");
    private static final Object DISABLED_STATUS = PluginText.formatted("<red>Off</red>");

    private final UHCTeam team;

    public TeamSettingsMenu(UHCTeam team) {
        super(PluginMenuSection.of(SECTION));
        this.team = team;
    }

    public static boolean handleInput(Player player, String input) {
        TextInputSession session = inputSessions.get(player.getUniqueId());

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
        if (slot == getSection().getButtonSlot(NAME_BUTTON))
            return getSection().getButtonItem(NAME_BUTTON, "{name}", team.getName());

        if (slot == getSection().getButtonSlot(COLOR_BUTTON))
            return getSection().getButtonItem(COLOR_BUTTON, "{color}", team.getColor().name().toLowerCase(Locale.ROOT));

        if (slot == getSection().getButtonSlot(CHARACTER_BUTTON))
            return getSection().getButtonItem(CHARACTER_BUTTON, "{character}", team.getSymbol());

        if (slot == getSection().getButtonSlot(OPEN_JOIN_BUTTON))
            return getSection().getButtonItem(OPEN_JOIN_BUTTON, "{status}", team.isOpenJoin() ? ENABLED_STATUS : DISABLED_STATUS);

        if (slot == getSection().getButtonSlot(HELP_BUTTON))
            return getSection().getButtonItem(HELP_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getSection().getButtonSlot(NAME_BUTTON)) {
            startNameInput(player);
            return;
        }

        if (slot == getSection().getButtonSlot(COLOR_BUTTON)) {
            openColorPicker(player);
            return;
        }

        if (slot == getSection().getButtonSlot(CHARACTER_BUTTON)) {
            startCharacterInput(player);
            return;
        }

        if (slot == getSection().getButtonSlot(OPEN_JOIN_BUTTON)) {
            player.performCommand("team public");
            displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(HELP_BUTTON)) {
            player.closeInventory();
            player.performCommand("team ?");
        }
    }

    private void startNameInput(Player player) {
        if (!UHCPermission.TEAM_SETTINGS_NAME.checkPerms(player))
            return;

        startTextInput(
                player,
                Messages.Editor.Text.TeamName.MESSAGE,
                input -> {
                    team.setName(input);
                    Chat.sendConversing(player, PluginText.replaceToString(
                            Messages.Editor.Text.TeamName.SAVED,
                            "{player}", player.getName(),
                            "{name}", team.getName()
                    ));
                },
                input -> true
        );
    }

    private void openColorPicker(Player player) {
        if (!UHCPermission.TEAM_SETTINGS_COLOR.checkPerms(player))
            return;

        new ColorPickerMenu(returningPlayer -> new TeamSettingsMenu(team).displayTo(returningPlayer)) {
            @Override
            protected void onChooseColor(Player player, PluginColor color) {
                team.setColor(color);
            }
        }.displayTo(player);
    }

    private void startCharacterInput(Player player) {
        if (!UHCPermission.TEAM_SETTINGS_CHARACTER.checkPerms(player))
            return;

        startTextInput(
                player,
                PluginText.replaceToString(
                        Messages.Editor.Text.TeamCharacter.MESSAGE,
                        "{length}", Settings.Team.MAX_CHARACTER_LENGTH
                ),
                input -> {
                    team.setSymbol(StringUtils.left(input, Settings.Team.MAX_CHARACTER_LENGTH));
                    Chat.sendConversing(player, PluginText.replaceToString(
                            Messages.Editor.Text.TeamCharacter.SAVED,
                            "{player}", player.getName(),
                            "{character}", team.getSymbol()
                    ));
                },
                input -> isCharacterInputAvailable(player, input)
        );
    }

    private boolean isCharacterInputAvailable(Player player, String input) {
        boolean used = UHCTeam.getTeams().stream()
                .map(UHCTeam::getSymbol)
                .anyMatch(symbol -> symbol.equalsIgnoreCase(input));

        if (used)
            Chat.sendConversing(player, PluginText.replaceToString(
                    Messages.Editor.Text.TeamCharacter.ALREADY_USED,
                    "{symbol}", input
            ));

        return !used;
    }

    private void startTextInput(Player player, String message, Consumer<String> saveInput, Predicate<String> isInputValid) {
        if (inputSessions.containsKey(player.getUniqueId())) {
            Chat.send(player, "<red>目前已有正在等待的設定輸入。</red>");
            return;
        }

        inputSessions.put(player.getUniqueId(), new TextInputSession(saveInput, isInputValid));
        player.closeInventory();
        Chat.sendConversing(player, message);
    }

    private static final class TextInputSession {
        private final Consumer<String> saveInput;
        private final Predicate<String> isInputValid;

        private TextInputSession(Consumer<String> saveInput, Predicate<String> isInputValid) {
            this.saveInput = saveInput;
            this.isInputValid = isInputValid;
        }

        private void accept(Player player, String input) {
            if (isCancelInput(input)) {
                inputSessions.remove(player.getUniqueId());
                Chat.sendConversing(player, "<red>隊伍設定輸入已取消。</red>");
                return;
            }

            if (!isInputValid.test(input))
                return;

            inputSessions.remove(player.getUniqueId());
            saveInput.accept(input);
        }

        private boolean isCancelInput(String input) {
            return input.equalsIgnoreCase("quit")
                    || input.equalsIgnoreCase("cancel")
                    || input.equalsIgnoreCase("exit");
        }
    }
}
