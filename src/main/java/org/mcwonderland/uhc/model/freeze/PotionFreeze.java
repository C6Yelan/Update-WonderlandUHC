package org.mcwonderland.uhc.model.freeze;

import org.mcwonderland.uhc.util.Extra;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class PotionFreeze implements FreezeMode {

    @Override
    public void freeze(Player player) {
        Extra.potion(player, PotionEffectType.SLOWNESS, 99999, 120, false);
//        Extra.potion(player, PotionEffectType.JUMP_BOOST, 99999, 250, false);
        Extra.potion(player, PotionEffectType.BLINDNESS, 99999, 0, false);
    }

    @Override
    public void unFreeze(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}
