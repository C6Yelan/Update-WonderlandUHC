package org.mcwonderland.uhc.game.player.staff;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;

@Getter
public enum OreAlert {
    GOLD_ORE(Material.GOLD_ORE, NamedTextColor.YELLOW),
    DIAMOND_ORE(Material.DIAMOND_ORE, NamedTextColor.AQUA);

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private final Material material;
    private final NamedTextColor color;

    OreAlert(Material material, NamedTextColor color) {
        this.material = material;
        this.color = color;
    }

    public static OreAlert fromMaterial(Material material) {
        for (OreAlert value : values()) {
            if (OreRuleSupport.matchesBlock(value.getMaterial(), material))
                return value;
        }

        return null;
    }

    public String colorizedName() {
        return LEGACY_AMPERSAND.serialize(Component.text(material.name(), color));
    }
}
