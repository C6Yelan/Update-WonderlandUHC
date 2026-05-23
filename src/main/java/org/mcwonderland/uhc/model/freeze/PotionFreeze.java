package org.mcwonderland.uhc.model.freeze;

import org.mcwonderland.uhc.util.Extra;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class PotionFreeze implements FreezeMode {

    @Override
    public void freeze(Player player) {
        AttributeInstance jumpStrength = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpStrength != null)
            jumpStrength.setBaseValue(0);

        Extra.potion(player, PotionEffectType.SLOWNESS, 99999, 120, false);
        Extra.potion(player, PotionEffectType.BLINDNESS, 99999, 0, false);
    }

    @Override
    public void unFreeze(Player player) {
        AttributeInstance jumpStrength = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpStrength != null)
            jumpStrength.setBaseValue(jumpStrength.getDefaultValue());

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.LEVITATION);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}
