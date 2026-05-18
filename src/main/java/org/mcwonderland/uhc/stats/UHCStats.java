package org.mcwonderland.uhc.stats;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Material;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;

import java.util.HashMap;
import java.util.Map;

public class UHCStats {

    @Getter(AccessLevel.PRIVATE)
    public Map<Material, Integer> oreMined = new HashMap<>();
    public int gamePlayed;
    public int totalKills;
    public int totalWins;
    public int kills;


    public double getKdr() {
        return totalKills / Math.max(gamePlayed, 1);
    }

    public int addOreMined(Material material) {
        Material countedMaterial = OreRuleSupport.canonicalLimitedOre(material);
        Integer amount = oreMined.getOrDefault(countedMaterial, 0);
        oreMined.put(countedMaterial, ++amount);

        return amount;
    }

}
