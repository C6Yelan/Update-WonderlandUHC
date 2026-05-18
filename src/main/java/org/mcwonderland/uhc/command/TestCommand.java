package org.mcwonderland.uhc.command;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.model.InvinciblePlayer;
import org.mcwonderland.uhc.tools.spectator.OverworldPlayersItem;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.Bukkit;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.settings.YamlConfig;

public class TestCommand extends SimpleCommand {

    public TestCommand(String label) {
        super(label);
    }

    @Override
    protected void onCommand() {
        String param = args[0];

        if ("pvp".equalsIgnoreCase(param))
            Game.getGame().setPvpEnabled(!Game.getGame().isPvpEnabled());

        else if ("damage".equalsIgnoreCase(param))
            Bukkit.getPlayer("LU__LU").setHealth(5);
        else if ("tf".equalsIgnoreCase(param))
            Game.getSettings().getTeamSettings().setAllowTeamFire(!Game.getSettings().getTeamSettings().isAllowTeamFire());

        else if ("kill".equalsIgnoreCase(param))
            getPlayer().setHealth(0);
        else if ("item".equalsIgnoreCase(param)) {
        } else if ("potion".equalsIgnoreCase(param)) {
//            final ItemStack result = BrewResultGetter.getBrewResult(getPlayer().getInventory().getItemInMainHand(), CompMaterial.BLAZE_POWDER.toItem());
//            getPlayer().getInventory().addItem(result);
        } else if ("half".equalsIgnoreCase(param)) {
            getPlayer().setHealth(1);
        } else if ("invin".equalsIgnoreCase(param)) {
            InvinciblePlayer.addInvincible(UHCPlayer.getUHCPlayer(getPlayer()), 20);
        } else if ("expcool".equalsIgnoreCase(param)) {
//            System.out.println((( CraftPlayer ) getPlayer()).getHandle().bC);
        } else if ("perm".equalsIgnoreCase(param)) {
            System.out.println(UHCPermission.BYPASS_KICK_DEATH.hasPerm(findPlayer("LU__LU")));
        } else if ("menu".equalsIgnoreCase(param)) {
            OverworldPlayersItem.getInstance().give(getPlayer());
        } else if ("bb".equalsIgnoreCase(param)) {
            getPlayer().sendMessage("Hi, it's working!");
        } else if ("inv".equalsIgnoreCase(param)) {
            Chat.broadcast(getPlayer().getInventory().getStorageContents().length + "");
        } else if ("files".equalsIgnoreCase(param)) {
//            YamlConfig.clearLoadedFiles();
            final StrictSet<Object> files = LegacyFoundationAdapter.getStaticFieldContent(YamlConfig.class, "loadedFiles");
            files.forEach(s -> {
                System.out.println(s.getClass());
            });
        } else if ("saveinv".equals(param)) {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("state".equalsIgnoreCase(param)) {
            System.out.println(Game.getGame().getCurrentStateName());
        }

        tell(
                " ",
                "&7PvP: " + Game.getGame().isPvpEnabled(),
                "&7Damage: " + Game.getGame().isDamageEnabled(),
                "&7TeamFire: " + Game.getSettings().getTeamSettings().isAllowTeamFire(),
                " "
        );
    }
}
