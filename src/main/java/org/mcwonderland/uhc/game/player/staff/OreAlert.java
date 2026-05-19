package org.mcwonderland.uhc.game.player.staff;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;
import org.mcwonderland.uhc.platform.text.PluginText;

@Getter
public enum OreAlert {
    GOLD_ORE(Material.GOLD_ORE, NamedTextColor.YELLOW, "yellow"),
    DIAMOND_ORE(Material.DIAMOND_ORE, NamedTextColor.AQUA, "aqua");

    private final Material material;
    private final NamedTextColor color;
    private final String colorTag;

    OreAlert(Material material, NamedTextColor color, String colorTag) {
        this.material = material;
        this.color = color;
        this.colorTag = colorTag;
    }

    public static OreAlert fromMaterial(Material material) {
        for (OreAlert value : values()) {
            if (OreRuleSupport.matchesBlock(value.getMaterial(), material))
                return value;
        }

        return null;
    }

    public Object formattedName() {
        return PluginText.formatted("<" + colorTag + ">" + material.name() + "</" + colorTag + ">");
    }
}
