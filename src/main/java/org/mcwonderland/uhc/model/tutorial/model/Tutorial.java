package org.mcwonderland.uhc.model.tutorial.model;

import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public abstract class Tutorial {
    public static final String CANCELLER = "exit";
    private static final String TUTORIAL_TAG = "wonderlanduhc_tutorial_tag";

    public static Tutorial getCurrentTutorial(Player player) {
        MetadataValue metadata = LegacyFoundationAdapter.getTempMetadata(player, TUTORIAL_TAG);

        return metadata == null ? null : (Tutorial) metadata.value();
    }

    public static boolean isInTutorial(Player player) {
        return LegacyFoundationAdapter.hasTempMetadata(player, TUTORIAL_TAG);
    }

    public static void exit(Player player) {
        LegacyFoundationAdapter.removeTempMetadata(player, TUTORIAL_TAG);
    }

    private TutorialSection currentSection;
    private final Player player;

    public Tutorial(Player player) {
        this.player = player;
    }

    protected abstract TutorialSection getFirstSection();

    public final void start() {
        LegacyFoundationAdapter.setTempMetadata(player, TUTORIAL_TAG, this);

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
