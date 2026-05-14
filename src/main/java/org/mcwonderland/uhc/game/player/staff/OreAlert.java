package org.mcwonderland.uhc.game.player.staff;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;

@Getter
public enum OreAlert {
    GOLD_ORE(Material.GOLD_ORE, ChatColor.YELLOW),
    DIAMOND_ORE(Material.DIAMOND_ORE, ChatColor.AQUA);

    private final Material material;
    private final ChatColor color;

    OreAlert(Material material, ChatColor color) {
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
        return color + material.name();
    }
}
