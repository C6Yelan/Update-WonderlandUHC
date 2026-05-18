package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.model.tutorial.UHCConfigTutorial;
import org.mcwonderland.uhc.model.tutorial.UHCHostTutorial;
import org.mcwonderland.uhc.model.tutorial.model.Tutorial;

import java.util.List;

public class TutorialCommand extends UHCSubCommand {

    protected TutorialCommand(String sublabel) {
        super(sublabel);

        setUsage("<config|host>");
        setMinArguments(1);
        setDescription("學習如何主持與設定UHC。");
        setPermission(UHCPermission.COMMAND_UHC_TUTORIAL.toString());
        setPlayerOnly(true);
    }

    @Override
    protected void onCommand() {
        getTutorial().start();
    }

    private Tutorial getTutorial() {
        switch (args[0]) {
            case "config":
                return new UHCConfigTutorial(getPlayer());
            case "host":
                return new UHCHostTutorial(getPlayer());
        }

        returnTell("&c找不到此教學。");
        return null;
    }

    @Override
    List<String> tabComplete(String[] args) {
        if (args.length == 1)
            return completeLastWord(args, "config", "host");

        return List.of();
    }
}
