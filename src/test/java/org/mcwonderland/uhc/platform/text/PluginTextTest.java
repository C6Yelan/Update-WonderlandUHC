package org.mcwonderland.uhc.platform.text;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PluginTextTest {

    @Test
    public void toComponentReadsLegacyAmpersandColors() {
        assertEquals("\u00A7aHello \u00A7cWorld", PluginText.toLegacyString(PluginText.toComponent("&AHello &cWorld")));
    }

    @Test
    public void toNullableComponentKeepsNullMessage() {
        assertNull(PluginText.toNullableComponent(null));
    }

    @Test
    public void toLegacyStringSerializesComponent() {
        assertEquals("\u00A7bText", PluginText.toLegacyString(PluginText.toComponent("&bText")));
    }

    @Test
    public void toLegacyAmpersandStringSerializesComponent() {
        assertEquals("&bText", PluginText.toLegacyAmpersandString(PluginText.toComponent("&bText")));
    }
}
