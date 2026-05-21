package org.mcwonderland.uhc.scoreboard;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class SimpleSidebarTest {

    @Test
    public void sidebarEntriesUseInvisibleFormattingCodes() {
        Set<String> entries = new HashSet<>();

        for (int slot = 1; slot <= 15; slot++) {
            String entry = SimpleSidebar.genEntry(slot);

            assertEquals(2, entry.length());
            assertEquals('\u00A7', entry.charAt(0));
            assertFalse(entry.contains("\u200B"));
            entries.add(entry);
        }

        assertEquals(15, entries.size());
    }

    @Test
    public void sidebarEntryRejectsInvalidSlot() {
        assertThrows(IllegalArgumentException.class, () -> SimpleSidebar.genEntry(0));
        assertThrows(IllegalArgumentException.class, () -> SimpleSidebar.genEntry(16));
    }
}
