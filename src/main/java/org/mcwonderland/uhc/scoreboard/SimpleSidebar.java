package org.mcwonderland.uhc.scoreboard;

import org.mcwonderland.uhc.platform.text.PluginText;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

/**
 * @author crisdev333
 */
public class SimpleSidebar {
    private static final char LEGACY_COLOR_CHAR = '\u00A7';
    private static final char[] SIDEBAR_ENTRY_CODES = "0123456789abcdef".toCharArray();

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
        title = PluginText.colorize(title);
        sidebar.displayName(toComponent(StringUtils.left(title, 32)));
    }

    public void setSlot(int slot, String text) {
        Team team = scoreboard.getTeam("SLOT_" + slot);
        String entry = genEntry(slot);
        if (!scoreboard.getEntries().contains(entry)) {
            sidebar.getScore(entry).setScore(slot);
        }
        text = PluginText.colorize(text);
        String pre = getFirstSplit(text);
        String suf = getFirstSplit(getLastColors(pre) + getSecondSplit(text));
        team.prefix(toComponent(pre));
        team.suffix(toComponent(suf));
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
        return LEGACY_COLOR_CHAR + String.valueOf(SIDEBAR_ENTRY_CODES[slot]);
    }

    private final String getFirstSplit(String s) {
        return StringUtils.left(s, 16);
    }

    private final String getSecondSplit(String s) {
        if (s.length() > 32) {
            s = StringUtils.left(s, 32);
        }

        return s.length() > 16 ? s.substring(16) : "";
    }

    private Component toComponent(String text) {
        return PluginText.toComponent(text);
    }

    private String getLastColors(String input) {
        String result = "";

        for (int index = input.length() - 1; index > -1; index--) {
            if (input.charAt(index) != LEGACY_COLOR_CHAR || index >= input.length() - 1)
                continue;

            String hexColor = getHexColor(input, index);
            if (hexColor != null)
                return hexColor + result;

            char color = Character.toLowerCase(input.charAt(index + 1));
            if (!isLegacyCode(color))
                continue;

            result = LEGACY_COLOR_CHAR + String.valueOf(color) + result;

            if (isLegacyColor(color) || color == 'r')
                break;
        }

        return result;
    }

    private String getHexColor(String input, int index) {
        if (index < 12)
            return null;

        if (input.charAt(index - 12) != LEGACY_COLOR_CHAR || Character.toLowerCase(input.charAt(index - 11)) != 'x')
            return null;

        for (int i = index - 10; i <= index; i += 2) {
            if (input.charAt(i) != LEGACY_COLOR_CHAR)
                return null;
        }

        for (int i = index - 9; i <= index + 1; i += 2) {
            if (Character.digit(input.charAt(i), 16) == -1)
                return null;
        }

        return input.substring(index - 12, index + 2);
    }

    private boolean isLegacyCode(char color) {
        return isLegacyColor(color) || "klmnor".indexOf(color) >= 0;
    }

    private boolean isLegacyColor(char color) {
        return "0123456789abcdef".indexOf(color) >= 0;
    }

}
