package org.mcwonderland.uhc.command.impl.host.whitelist;

import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.CommandSettings;

class ListCommand extends WhitelistSubCommand {

    protected ListCommand(WhitelistCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setDescription("Show whitelisted players.");
    }

    @Override
    protected void onCommand() {
        tell(LegacyFoundationAdapter.replaceToList(
                CommandSettings.Whitelist.LIST,
                "{players}", getWhitelist()));
    }
}
