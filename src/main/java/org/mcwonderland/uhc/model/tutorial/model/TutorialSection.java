package org.mcwonderland.uhc.model.tutorial.model;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.util.Chat;

public abstract class TutorialSection {

    public static final TutorialSection END_TUTORIAL = null;
    private static final String BOX_LINE = "<dark_gray>-----------------------------------------------------</dark_gray>";

    protected void show(Player player) {
        Chat.send(player, BOX_LINE);

        for (String message : getMessages())
            for (String part : message.split("\n"))
                Chat.send(player, part);

        Chat.send(player, BOX_LINE);
    }

    public boolean isLastOne() {
        return nextSection() == END_TUTORIAL;
    }

    protected abstract String[] getMessages();

    protected abstract TutorialSection nextSection();
}
