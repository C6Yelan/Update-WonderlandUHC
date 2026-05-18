package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Chat;

public class ReloadCommand extends UHCSubCommand {

    public ReloadCommand(String label) {
        super(label);

        setDescription("重載插件。");
        setPermission(UHCPermission.COMMAND_UHC_RELOAD.toString());
    }

    @Override
    protected void onCommand() {
        try {
            WonderlandUHC plugin = WonderlandUHC.getInstance();
            plugin.reload();
            Chat.send(sender, CommandSettings.RELOAD_SUCCESS
                    .replace("{plugin_name}", plugin.getName())
                    .replace("{plugin_version}", plugin.getPluginMeta().getVersion()));
        } catch (final Throwable t) {
            Chat.send(sender, CommandSettings.RELOAD_FAIL.replace("{error}", t.getMessage() != null ? t.getMessage() : "unknown"));
            t.printStackTrace();
        }
    }
}
