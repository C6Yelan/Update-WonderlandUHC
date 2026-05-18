package org.mcwonderland.uhc.command.impl.host.whitelist;

import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.CommandSettings;

class ListCommand extends WhitelistSubCommand {

    protected ListCommand(WhitelistCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setDescription("Show whitelisted players.");
    }

    @Override
    protected void onCommand() {
        tell(PluginText.replaceToList(
                CommandSettings.Whitelist.LIST,
                "{players}", getWhitelist()));
    }
}
