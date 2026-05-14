package org.mcwonderland.uhc.game.state.playing.listener;

import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class DisableItemListenerTest {

    @Test
    public void toItemArrayKeepsBrewResultsInOrder() {
        ItemStack[] items = DisableItemListener.toItemArray(Arrays.asList(null, null));

        assertEquals(2, items.length);
        assertNull(items[0]);
        assertNull(items[1]);
    }

    @Test
    public void disabledMatcherHandlesMissingItems() {
        assertFalse(DisableItemListener.containsDisabledItem(null, (ItemStack[]) null));
    }
}
