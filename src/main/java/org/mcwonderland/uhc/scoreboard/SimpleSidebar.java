package org.mcwonderland.uhc.scoreboard;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.mcwonderland.uhc.platform.text.PluginText;

import java.util.List;

/**
 * @author crisdev333
 */
public class SimpleSidebar {
    private static final String SIDEBAR_ENTRY_UNIT = "\u200B";

    @Getter
    private final Scoreboard scoreboard;
    @Getter
    private final Objective sidebar;

    private SimpleSidebar(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
        this.sidebar = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, toComponent("sidebar"));
        this.sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 1; i <= 15; i++) {
            Team team = scoreboard.registerNewTeam("SLOT_" + i);
            team.addEntry(genEntry(i));
        }
    }

    public static SimpleSidebar createSidebarIn(Scoreboard scoreboard) {
        return new SimpleSidebar(scoreboard);
    }

    public final void setTitle(String title) {
        sidebar.displayName(toComponent(title));
    }

    public void setSlot(int slot, String text) {
        Team team = scoreboard.getTeam("SLOT_" + slot);
        String entry = genEntry(slot);
        if (!scoreboard.getEntries().contains(entry)) {
            sidebar.getScore(entry).setScore(slot);
        }
        team.prefix(toComponent(text));
        team.suffix(Component.empty());
    }

    public void removeSlot(int slot) {
        String entry = genEntry(slot);
        if (scoreboard.getEntries().contains(entry)) {
            scoreboard.resetScores(entry);
        }
    }

    public void setSlotsFromList(List<String> list) {
        while (list.size() > 15) {
            list.remove(list.size() - 1);
        }

        int slot = list.size();

        if (slot < 15) {
            for (int i = (slot + 1); i <= 15; i++) {
                removeSlot(i);
            }
        }

        for (String line : list) {
            setSlot(slot, line);
            slot--;
        }
    }

    private final String genEntry(int slot) {
        return SIDEBAR_ENTRY_UNIT.repeat(slot);
    }

    private Component toComponent(String text) {
        return PluginText.toComponent(text);
    }
}
