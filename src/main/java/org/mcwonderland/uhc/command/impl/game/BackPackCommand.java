package org.mcwonderland.uhc.command.impl.game;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.GameUtils;

/**
 * 2019-11-27 下午 01:05
 */
public class BackPackCommand implements CommandExecutor {

    public static final String NAME = "backpack";

    private static final String SEE_BACKPACK_PERMISSION = "wonderland.uhc.host.seebackpack";
    private static final String PLAYER_NOT_ONLINE = "<red>Player {player} is not online on this server.</red>";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new BackPackCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!GameUtils.isGameStarted()) {
            Chat.send(player, Messages.NOT_YET_STARTED);
            return true;
        }

        if (!isBackPackEnabled())
            return true;

        if (args.length == 1)
            openOthersBackPack(player, args[0]);
        else
            openOwnBackPack(player);

        return true;
    }

    private void openOthersBackPack(Player viewer, String targetName) {
        if (!viewer.hasPermission(SEE_BACKPACK_PERMISSION)) {
            Chat.send(viewer, Messages.NO_PERMISSION.replace("{permission}", SEE_BACKPACK_PERMISSION));
            return;
        }

        Player toOpen = PluginPlayers.getByName(targetName, true);

        if (toOpen == null) {
            Chat.send(viewer, PLAYER_NOT_ONLINE.replace("{player}", targetName));
            return;
        }

        openOnesBackPack(viewer, toOpen);
    }

    private void openOwnBackPack(Player player) {
        if (!GameUtils.isGamingPlayer(player)) {
            Chat.send(player, Messages.NOT_GAMING_PLAYER);
            return;
        }

        openOnesBackPack(player, player);
    }

    private boolean isBackPackEnabled() {
        Scenario bpMode = WonderlandUHC.getInstance().getScenarioManager().getScenario(ScenarioName.BACKPACK);
        return bpMode.isEnabled();
    }

    private void openOnesBackPack(Player viewer, Player toCheck) {
        viewer.openInventory(UHCTeam.getTeam(toCheck).getBackpack());
    }
}
