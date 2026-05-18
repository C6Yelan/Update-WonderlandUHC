package org.mcwonderland.uhc.menu.impl.host;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.application.match.MatchStartRequestService;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.platform.PlayerHand;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.InventorySaver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

public class MainSettingsMenu extends PluginMenu {
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final Map<UUID, InputSession> inputSessions = new ConcurrentHashMap<>();
    private static final String SECTION = "Main";
    private static final String TEAM_BUTTON = "Team";
    private static final String BORDER_BUTTON = "Border";
    private static final String TIME_BUTTON = "Time";
    private static final String SCOREBOARD_BUTTON = "Scoreboard";
    private static final String BROADCAST_BUTTON = "Broadcast";
    private static final String SCENARIOS_BUTTON = "Scenarios";
    private static final String WHITELIST_BUTTON = "Whitelist";
    private static final String PLAYERS_BUTTON = "Players";
    private static final String CUSTOM_INVENTORY_BUTTON = "Custom_Inventory";
    private static final String PRACTICE_INVENTORY_BUTTON = "Practice_Inventory";
    private static final String CUSTOM_DROPS_BUTTON = "Custom_Drops";
    private static final String APPLE_RATE_BUTTON = "Apple_Rate";
    private static final String EXPERIENCE_BUTTON = "Experience";
    private static final String TITLE_BUTTON = "Title";
    private static final String NETHER_BUTTON = "Nether";
    private static final String DISABLE_ITEMS_BUTTON = "Disable_Items";
    private static final String ENDER_PEARL_DAMAGE_BUTTON = "Ender_Pearl_Damage";
    private static final String SAVES_BUTTON = "Saves";
    private static final String GENERATE_MAP_BUTTON = "Generate_Map";
    private static final String START_BUTTON = "Start";
    private static final String ENABLED_STATUS = "&aOn";
    private static final String DISABLED_STATUS = "&cOff";
    private static final String FINISH_INPUT = "finish";
    private static final String TO_HEAD_INPUT = "tohead";

    private final MatchStartRequestService matchStartRequest = new MatchStartRequestService();

    public MainSettingsMenu() {
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
        UHCGameSettings settings = Game.getSettings();

        if (slot == getSection().getButtonSlot(TEAM_BUTTON))
            return getSection().getButtonItem(TEAM_BUTTON);

        if (slot == getSection().getButtonSlot(BORDER_BUTTON))
            return getSection().getButtonItem(BORDER_BUTTON);

        if (slot == getSection().getButtonSlot(TIME_BUTTON))
            return getSection().getButtonItem(TIME_BUTTON);

        if (slot == getSection().getButtonSlot(SCOREBOARD_BUTTON))
            return getSection().getButtonItem(SCOREBOARD_BUTTON);

        if (slot == getSection().getButtonSlot(BROADCAST_BUTTON))
            return getSection().getButtonItem(BROADCAST_BUTTON);

        if (slot == getSection().getButtonSlot(SCENARIOS_BUTTON))
            return getSection().getButtonItem(SCENARIOS_BUTTON);

        if (slot == getSection().getButtonSlot(WHITELIST_BUTTON))
            return getSection().getButtonItem(WHITELIST_BUTTON, "{status}", settings.isWhitelistOn() ? ENABLED_STATUS : DISABLED_STATUS);

        if (slot == getSection().getButtonSlot(PLAYERS_BUTTON))
            return getSection().getButtonItem(PLAYERS_BUTTON, "{number}", settings.getMaxPlayers());

        if (slot == getSection().getButtonSlot(CUSTOM_INVENTORY_BUTTON))
            return getSection().getButtonItem(CUSTOM_INVENTORY_BUTTON);

        if (slot == getSection().getButtonSlot(PRACTICE_INVENTORY_BUTTON))
            return getSection().getButtonItem(PRACTICE_INVENTORY_BUTTON);

        if (slot == getSection().getButtonSlot(CUSTOM_DROPS_BUTTON))
            return getSection().getButtonItem(CUSTOM_DROPS_BUTTON);

        if (slot == getSection().getButtonSlot(APPLE_RATE_BUTTON))
            return getSection().getButtonItem(APPLE_RATE_BUTTON, "{count}", settings.getAppleRate());

        if (slot == getSection().getButtonSlot(EXPERIENCE_BUTTON))
            return getSection().getButtonItem(EXPERIENCE_BUTTON, "{count}", settings.getInitialExperience());

        if (slot == getSection().getButtonSlot(TITLE_BUTTON))
            return getSection().getButtonItem(TITLE_BUTTON, "{title}", settings.getTitle());

        if (slot == getSection().getButtonSlot(NETHER_BUTTON))
            return getSection().getButtonItem(NETHER_BUTTON, "{status}", settings.isUsingNether() ? ENABLED_STATUS : DISABLED_STATUS);

        if (slot == getSection().getButtonSlot(DISABLE_ITEMS_BUTTON))
            return getSection().getButtonItem(DISABLE_ITEMS_BUTTON);

        if (slot == getSection().getButtonSlot(ENDER_PEARL_DAMAGE_BUTTON))
            return getSection().getButtonItem(ENDER_PEARL_DAMAGE_BUTTON, "{status}", settings.isEnderPearlDamage() ? ENABLED_STATUS : DISABLED_STATUS);

        if (slot == getSection().getButtonSlot(SAVES_BUTTON))
            return getSection().getButtonItem(SAVES_BUTTON);

        if (CacheSaver.getLoadingStatus() != LoadingStatus.DONE) {
            if (slot == getSection().getButtonSlot(GENERATE_MAP_BUTTON))
                return getSection().getButtonItem(GENERATE_MAP_BUTTON);
        } else if (slot == getSection().getButtonSlot(START_BUTTON))
            return getSection().getButtonItem(START_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        UHCGameSettings settings = Game.getSettings();

        if (slot == getSection().getButtonSlot(TEAM_BUTTON)) {
            new TeamModeSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(BORDER_BUTTON)) {
            new BorderSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(TIME_BUTTON)) {
            new TimeSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(SCOREBOARD_BUTTON)) {
            new ScoreboardSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(BROADCAST_BUTTON)) {
            new BroadcastSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(SCENARIOS_BUTTON)) {
            new ScenarioSettingsMenu(WonderlandUHC.getInstance().getScenarioManager()).displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(WHITELIST_BUTTON)) {
            toggleWhitelist(player, settings);
            return;
        }

        if (slot == getSection().getButtonSlot(PLAYERS_BUTTON)) {
            startIntegerInput(player, Messages.Editor.Number.MaxPlayers.MESSAGE, Messages.Editor.Number.MaxPlayers.SAVED,
                    settings::setMaxPlayers);
            return;
        }

        if (slot == getSection().getButtonSlot(CUSTOM_INVENTORY_BUTTON)) {
            startInventoryEdit(player, InventorySaver.SaveType.CUSTOM_INVENTORY, Messages.Editor.Inventory.CustomInventory.MESSAGE,
                    Messages.Editor.Inventory.CustomInventory.SAVED);
            return;
        }

        if (slot == getSection().getButtonSlot(PRACTICE_INVENTORY_BUTTON)) {
            startInventoryEdit(player, InventorySaver.SaveType.PRACTICE_INVENTORY, Messages.Editor.Inventory.PracticeInventory.MESSAGE,
                    Messages.Editor.Inventory.PracticeInventory.SAVED);
            return;
        }

        if (slot == getSection().getButtonSlot(CUSTOM_DROPS_BUTTON)) {
            startInventoryEdit(player, InventorySaver.SaveType.CUSTOM_DROPS, Messages.Editor.Inventory.CustomDrops.MESSAGE,
                    Messages.Editor.Inventory.CustomDrops.SAVED);
            return;
        }

        if (slot == getSection().getButtonSlot(APPLE_RATE_BUTTON)) {
            handleAppleRateClick(player, click, settings);
            return;
        }

        if (slot == getSection().getButtonSlot(EXPERIENCE_BUTTON)) {
            handleExperienceClick(player, click, settings);
            return;
        }

        if (slot == getSection().getButtonSlot(TITLE_BUTTON)) {
            startTitleInput(player, settings);
            return;
        }

        if (slot == getSection().getButtonSlot(NETHER_BUTTON)) {
            toggleNether(player, settings);
            return;
        }

        if (slot == getSection().getButtonSlot(DISABLE_ITEMS_BUTTON)) {
            startInventoryEdit(player, InventorySaver.SaveType.DISABLE_ITEMS, Messages.Editor.Inventory.DisableItems.MESSAGE,
                    Messages.Editor.Inventory.DisableItems.SAVED);
            return;
        }

        if (slot == getSection().getButtonSlot(ENDER_PEARL_DAMAGE_BUTTON)) {
            settings.setEnderPearlDamage(!settings.isEnderPearlDamage());
            displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(SAVES_BUTTON)) {
            new SavedSettingsMenu(player).displayTo(player);
            return;
        }

        if (CacheSaver.getLoadingStatus() != LoadingStatus.DONE) {
            if (slot == getSection().getButtonSlot(GENERATE_MAP_BUTTON))
                new CenterCleanerMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(START_BUTTON)) {
            matchStartRequest.requestStart(player);
            player.closeInventory();
        }
    }

    private void toggleWhitelist(Player player, UHCGameSettings settings) {
        boolean newStatus = !settings.isWhitelistOn();

        settings.setWhitelistOn(newStatus);
        Chat.broadcast((newStatus ? Messages.Host.WHITELIST_ON : Messages.Host.WHITELIST_OFF)
                .replace("{player}", player.getName()));
        displayTo(player);
    }

    private void startIntegerInput(Player player, String prompt, String savedMessage, IntConsumer saveInput) {
        startInput(player, prompt, (inputPlayer, input) -> {
            Integer number = parseInteger(input);

            if (number == null) {
                Chat.sendConversing(inputPlayer, Messages.Editor.Number.INVALID_NUMBER);
                return;
            }

            inputSessions.remove(inputPlayer.getUniqueId());
            saveInput.accept(number.intValue());
            Chat.sendConversing(inputPlayer, savedMessage.replace("{number}", number + ""));
        });
    }

    private void startTitleInput(Player player, UHCGameSettings settings) {
        startInput(player, Messages.Editor.Text.Title.MESSAGE, (inputPlayer, input) -> {
            String newTitle = PluginText.colorize(input);
            String broadcastMessage = Messages.Editor.Text.Title.SAVED.replace("{title}", newTitle);

            inputSessions.remove(inputPlayer.getUniqueId());
            settings.setTitle(newTitle);
            Chat.broadcast(broadcastMessage.replace("{player}", inputPlayer.getName()));
            Chat.sendConversing(inputPlayer, broadcastMessage);
        });
    }

    private void startInventoryEdit(Player player, InventorySaver.SaveType saveType, String prompt, String savedMessage) {
        if (inputSessions.containsKey(player.getUniqueId())) {
            Chat.send(player, "&c目前已有正在等待的設定輸入。");
            return;
        }

        InventoryEditSession session = new InventoryEditSession(player, saveType, savedMessage);

        inputSessions.put(player.getUniqueId(), session);
        player.closeInventory();
        session.start(prompt);
    }

    private void handleAppleRateClick(Player player, ClickType click, UHCGameSettings settings) {
        if (click == ClickType.LEFT) {
            settings.setAppleRate(Math.max(0, settings.getAppleRate() - 1));
            displayTo(player);
            return;
        }

        if (click == ClickType.RIGHT) {
            settings.setAppleRate(Math.min(100, settings.getAppleRate() + 1));
            displayTo(player);
        }
    }

    private void handleExperienceClick(Player player, ClickType click, UHCGameSettings settings) {
        if (click == ClickType.LEFT) {
            settings.setInitialExperience(Math.max(0, settings.getInitialExperience() - 1));
            displayTo(player);
            return;
        }

        if (click == ClickType.RIGHT) {
            settings.setInitialExperience(settings.getInitialExperience() + 1);
            displayTo(player);
        }
    }

    private void toggleNether(Player player, UHCGameSettings settings) {
        boolean newStatus = !settings.isUsingNether();

        settings.setUsingNether(newStatus);
        Game.changeSettings(settings);
        CacheSaver.saveCache();
        Chat.broadcast((newStatus ? Messages.Host.NETHER_ENABLED_PLAYER : Messages.Host.NETHER_DISABLED_PLAYER)
                .replace("{player}", player.getName()));
        displayTo(player);
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

    private Integer parseInteger(String input) {
        try {
            return Integer.valueOf(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Component runCommandComponent(String message, String command) {
        return LEGACY_AMPERSAND.deserialize(message)
                .clickEvent(ClickEvent.runCommand(command));
    }

    private interface InputSession {
        void accept(Player player, String input);
    }

    private static final class InventoryEditSession implements InputSession {
        private final UUID playerId;
        private final InventorySaver.SaveType saveType;
        private final String savedMessage;
        private final ItemStack[] inventoryBackup;
        private final GameMode gameModeBackup;

        private InventoryEditSession(Player player, InventorySaver.SaveType saveType, String savedMessage) {
            this.playerId = player.getUniqueId();
            this.saveType = saveType;
            this.savedMessage = savedMessage;
            this.inventoryBackup = cloneContents(player.getInventory().getContents());
            this.gameModeBackup = player.getGameMode();
        }

        private void start(String prompt) {
            Player player = Bukkit.getPlayer(playerId);

            if (player == null)
                return;

            InventorySaver.setContents(player, saveType);
            player.setGameMode(GameMode.CREATIVE);
            Chat.sendConversing(player, prompt);
            Chat.sendConversing(player, "");
            player.sendMessage(runCommandComponent(Messages.Editor.Inventory.CLICK_TO_HEAD, TO_HEAD_INPUT));
            player.sendMessage(runCommandComponent(Messages.Editor.CLICK_FINISH, FINISH_INPUT));
        }

        @Override
        public void accept(Player player, String input) {
            if (TO_HEAD_INPUT.equalsIgnoreCase(input)) {
                changeHandItemToHead(player);
                return;
            }

            if (!FINISH_INPUT.equalsIgnoreCase(input))
                return;

            inputSessions.remove(player.getUniqueId());
            Chat.sendConversing(player, savedMessage);
            InventorySaver.saveInventoryData(player, saveType);
            Extra.sound(player, Sounds.Host.INVENTORY_EDITED);
            player.getInventory().setContents(cloneContents(inventoryBackup));
            player.setGameMode(gameModeBackup);
        }

        private void changeHandItemToHead(Player player) {
            ItemStack mainHand = PlayerHand.getMainHandItem(player);

            if (mainHand.getType() != Material.GOLDEN_APPLE) {
                Chat.sendConversing(player, Messages.Editor.Inventory.TO_HEAD_FAILED);
                return;
            }

            ItemMeta meta = mainHand.getItemMeta();

            meta.displayName(LEGACY_SECTION.deserialize(Settings.Misc.GOLDEN_HEAD_NAME));
            mainHand.setItemMeta(meta);
            Extra.sound(player, Sounds.Host.GOLDEN_HEAD_CREATED);
        }

        private static ItemStack[] cloneContents(ItemStack[] contents) {
            ItemStack[] copy = new ItemStack[contents.length];

            for (int i = 0; i < contents.length; i++)
                copy[i] = contents[i] == null ? null : contents[i].clone();

            return copy;
        }
    }
}
