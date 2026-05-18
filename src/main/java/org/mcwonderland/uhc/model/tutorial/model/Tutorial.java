package org.mcwonderland.uhc.model.tutorial.model;

import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Tutorial {
    public static final String CANCELLER = "exit";
    private static final Map<UUID, Tutorial> tutorials = new HashMap<>();

    public static Tutorial getCurrentTutorial(Player player) {
        return tutorials.get(player.getUniqueId());
    }

    public static boolean isInTutorial(Player player) {
        return tutorials.containsKey(player.getUniqueId());
    }

    public static void exit(Player player) {
        tutorials.remove(player.getUniqueId());
    }

    private TutorialSection currentSection;
    private final Player player;

    public Tutorial(Player player) {
        this.player = player;
    }

    protected abstract TutorialSection getFirstSection();

    public final void start() {
        tutorials.put(player.getUniqueId(), this);

        showSection(getFirstSection());
    }

    public final void showNextSection() {
        showSection(currentSection.nextSection());
    }

    private final void showSection(TutorialSection section) {
        section.show(player);

        if (section.isLastOne()) {
            Tutorial.exit(player);
            Extra.sound(player, Sounds.Tutorial.FINISHED);
        } else
            Extra.sound(player, Sounds.Tutorial.NEXT_SECTION);

        currentSection = section;
    }
}
