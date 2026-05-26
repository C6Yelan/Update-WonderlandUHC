package org.mcwonderland.uhc.command.uhc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.world.PreviewWorldGenerationService;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;

/**
 * 2019-11-24 下午 12:50
 */
public class RegenWorldCommand extends UHCSubCommand {

    private final PreviewWorldGenerationService previewWorldGeneration = new PreviewWorldGenerationService();

    protected RegenWorldCommand(String subLabel) {
        super(subLabel);

        setDescription("重載遊戲世界.");
        setUsage("{seed}");
        setPermission(UHCPermission.COMMAND_UHC_REGEN.toString());
        setPlayerOnly(true);
    }

    @Override
    protected void onCommand() {
        if (WorldLoadingCacheState.getLoadingStatus() == LoadingStatus.DONE) {
            tell(Messages.Host.NOT_GENERATING);
            return;
        }

        if (isConfirmation("confirm", "yes", "true")) {
            createWorld(true, getSeedAfterConfirmation());
            return;
        }

        if (isConfirmation("skip", "no", "false")) {
            createWorld(false, getSeedAfterConfirmation());
            return;
        }

        sendCenterCleanerPrompt(getSeed());
    }

    private String getSeed() {
        return args.length >= 1 ? args[0] : null;
    }

    private String getSeedAfterConfirmation() {
        return args.length >= 2 ? args[1] : null;
    }

    private boolean isConfirmation(String... values) {
        if (args.length == 0)
            return false;

        for (String value : values)
            if (args[0].equalsIgnoreCase(value))
                return true;

        return false;
    }

    private void sendCenterCleanerPrompt(String seed) {
        String suffix = seed == null ? "" : " " + seed;

        Chat.send(getPlayer(),
                " ",
                "<gray>[</gray><green>中心搜尋</green><gray>]</gray> <white>是否啟用CenterCleaner來搜尋較適合UHC的中心點？</white>",
                "<gray>啟用後會在同一張世界中評估候選中心，不會自動換seed。</gray>");
        getPlayer().sendMessage(runCommandComponent(
                "<green>[啟用CenterCleaner]</green>",
                "/uhc regen confirm" + suffix,
                "<gray>點擊後建立預覽世界並搜尋中心</gray>"));
        getPlayer().sendMessage(runCommandComponent(
                "<red>[不啟用，直接建立]</red>",
                "/uhc regen skip" + suffix,
                "<gray>點擊後沿用舊預覽建立流程</gray>"));
    }

    private void createWorld(boolean centerCleaner, String seed) {
        previewWorldGeneration.create(getPlayer(), centerCleaner, seed);
    }

    private Component runCommandComponent(String message, String command, String hover) {
        return PluginText.toComponent(message)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(PluginText.toComponent(hover)));
    }
}
