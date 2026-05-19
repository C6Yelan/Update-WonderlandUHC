package org.mcwonderland.uhc.platform.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.Test;
import org.mcwonderland.uhc.util.TimePlaceholderFormatter;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PluginTextTest {

    @Test
    public void toComponentKeepsAmpersandColorCodesLiteral() {
        assertEquals("&AHello &cWorld", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent("&AHello &cWorld")));
    }

    @Test
    public void toComponentReadsMiniMessageWhenNoAmpersandCodes() {
        assertEquals("Hello World", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent("<green>Hello <white>World")));
    }

    @Test
    public void toComponentKeepsUnknownAngleTextLiteral() {
        assertEquals("Use /uhc regen <seed>", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent("Use /uhc regen <seed>")));
    }

    @Test
    public void toComponentKeepsUnknownAngleTextLiteralInsideMiniMessage() {
        assertEquals("Use /uhc regen <seed>", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent("<gold>Use /uhc regen <seed></gold>")));
    }

    @Test
    public void toComponentReadsMiniMessageAfterPlaceholderReplacement() {
        String message = PluginText.replaceToString("<red>請稍等 {time} 秒後再使用這個指令。</red>", "{time}", 3);

        assertEquals("請稍等 3 秒後再使用這個指令。", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(message)));
    }

    @Test
    public void toComponentKeepsMiniMessageWhenPlaceholderValueHasAmpersandColor() {
        String message = PluginText.replaceToString("<gray>[{team}]</gray>", "{team}", "&aWonderland");

        assertEquals("[&aWonderland]", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(message)));
    }

    @Test
    public void replaceToStringCanInsertInternalMiniMessageSnippet() {
        String message = PluginText.replaceToString(
                "<gray>狀態: </gray>{status}",
                "{status}", PluginText.formatted("<green>On</green>")
        );

        assertEquals("狀態: On", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(message)));
    }

    @Test
    public void toComponentEscapesMiniMessageTagsInsidePlaceholderValue() {
        String message = PluginText.replaceToString("<red>{value}</red>", "{value}", "<green>literal</green>");

        assertEquals("<green>literal</green>", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(message)));
    }

    @Test
    public void replaceToStringKeepsEditorTextInputLiteralInsideMiniMessageTemplate() {
        String message = PluginText.replaceToString(
                "<gray>隊伍名稱已被 </gray><green>{player}</green><gray> 改成了: </gray><reset>{name}</reset><gray>。</gray>",
                "{player}", "Alex",
                "{name}", "<red>NotATag"
        );

        assertEquals("隊伍名稱已被 Alex 改成了: <red>NotATag。", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(message)));
    }

    @Test
    public void toComponentKeepsPaginationArrowsLiteralInsideMiniMessage() {
        String message = PluginText.replaceToString(
                "<dark_gray><< </dark_gray><white>第 {page} 頁</white>",
                "{page}", 2
        );

        assertEquals("<< 第 2 頁", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(message)));
    }

    @Test
    public void replaceToStringCanBuildMiniMessageTagNameFromPlaceholder() {
        String message = PluginText.replaceToString(
                "<gray>目前顏色: </gray><{color}>❤</{color}>",
                "{color}", "red"
        );

        assertEquals("目前顏色: ❤", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(message)));
    }

    @Test
    public void toComponentKeepsClickJoinMiniMessageReplacementClickable() {
        String clickHere = "<gold><bold>點擊這裡來接受</bold></gold>";
        String message = "<white>若同意請 </white><gold><bold>[</bold></gold>{click-join}<gold><bold>]</bold></gold>"
                .replace("{click-join}", clickHere);
        ClickEvent clickEvent = ClickEvent.runCommand("/team join Alex");
        HoverEvent<Component> hoverEvent = HoverEvent.showText(PluginText.toComponent(clickHere));

        Component component = PluginText.toComponent(message)
                .clickEvent(clickEvent)
                .hoverEvent(hoverEvent);

        assertEquals("若同意請 [點擊這裡來接受]", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals(clickEvent, component.clickEvent());
        assertEquals(hoverEvent, component.hoverEvent());
    }

    @Test
    public void toComponentKeepsInventoryEditorMiniMessageRunCommandClickable() {
        ClickEvent clickEvent = ClickEvent.runCommand("finish");

        Component component = PluginText.toComponent("<green><bold>[點擊這裡完成設定]</bold></green>")
                .clickEvent(clickEvent);

        assertEquals("[點擊這裡完成設定]", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals(clickEvent, component.clickEvent());
    }

    @Test
    public void toNullableComponentKeepsNullMessage() {
        assertNull(PluginText.toNullableComponent(null));
    }

    @Test
    public void scoreboardMiniMessageLineCanRenderPlainText() {
        String line = PluginText.replaceToString(
                "<white>玩家數量: </white><green>{online_players}/{max_players}</green>",
                "{online_players}", 3,
                "{max_players}", 20
        );

        assertEquals("玩家數量: 3/20", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(line)));
        assertEquals(
                "*----------------------*",
                PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent("<gray><strikethrough>*----------------------*</strikethrough></gray>"))
        );
    }

    @Test
    public void toMiniMessageComponentReadsMiniMessageColorsAndDecorations() {
        assertEquals("Hello World", PlainTextComponentSerializer.plainText().serialize(PluginText.toMiniMessageComponent("<green>Hello <bold>World")));
    }

    @Test
    public void replaceTimeToArrayKeepsMiniMessageMultilineTemplate() {
        List<String> messages = List.of(
                "<dark_red><strikethrough>---</strikethrough></dark_red>",
                " ",
                "<red>裝備代價的效果將會於 {fancy-time} 後生效</red>"
        );

        String[] replaced = PluginText.replaceTimeToArray(messages, 10);

        assertEquals(3, replaced.length);
        assertEquals("---", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(replaced[0])));
        assertEquals(" ", PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(replaced[1])));
        assertEquals(
                "裝備代價的效果將會於 " + TimePlaceholderFormatter.fancyTime(10) + " 後生效",
                PlainTextComponentSerializer.plainText().serialize(PluginText.toComponent(replaced[2]))
        );
    }
}
