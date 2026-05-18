package org.mcwonderland.uhc.command.impl.host;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.enums.RoleName;
import org.mcwonderland.uhc.game.CombatRelog;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.GameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 2019-11-17 下午 03:58
 */
public class GiveAllCommand implements CommandExecutor, TabCompleter {

    public static final String NAME = "giveall";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        GiveAllCommand executor = new GiveAllCommand();
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = UHCPermission.COMMAND_GIVEALL.toString();

        if (!sender.hasPermission(permission)) {
            Chat.send(sender, Messages.NO_PERMISSION.replace("{permission}", permission));
            return true;
        }

        if (!GameUtils.isGameStarted()) {
            Chat.send(sender, Messages.NOT_YET_STARTED);
            return true;
        }

        if (args.length < 2)
            return false;

        ItemStack item = getItem(sender, args);

        if (item == null)
            return true;

        giveItemToPlayerAndRelogs(item);

        Chat.broadcast(CommandSettings.GiveAll.GIVEN
                .replace("{amount}", item.getAmount() + "")
                .replace("{material}", item.getType().toString()));
        return true;
    }

    private ItemStack getItem(CommandSender sender, String[] args) {
        Material material = Material.getMaterial(args[0].toUpperCase(Locale.ROOT));

        if (material == null || !material.isItem()) {
            Chat.send(sender, CommandSettings.GiveAll.INVALID_ITEM.replace("{item}", args[0]));
            return null;
        }

        Integer amount = parseAmount(sender, args[1]);

        if (amount == null)
            return null;

        return new ItemStack(material, amount);
    }

    private Integer parseAmount(CommandSender sender, String input) {
        try {
            int amount = Integer.parseInt(input);

            if (amount >= 1)
                return amount;
        } catch (NumberFormatException ignored) {
        }

        Chat.send(sender, CommandSettings.GiveAll.INVALID_AMOUNT);
        return null;
    }

    private void giveItemToPlayerAndRelogs(ItemStack item) {
        for (UHCPlayer uhcPlayer : UHCPlayers.getStatusIs(RoleName.PLAYER)) {
            ItemStack toAdd = item.clone();
            if (uhcPlayer.isOnline())
                uhcPlayer.getPlayer().getInventory().addItem(toAdd);
            else
                CombatRelog.get(uhcPlayer).addInventoryItem(toAdd);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1)
            return List.of();

        String prefix = args[0].toUpperCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.isItem() && material.name().startsWith(prefix))
                completions.add(material.name().toLowerCase(Locale.ROOT));
        }

        return completions;
    }
}
