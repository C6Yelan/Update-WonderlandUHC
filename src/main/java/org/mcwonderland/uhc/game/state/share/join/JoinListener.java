package org.mcwonderland.uhc.game.state.share.join;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JoinListener implements Listener {

    private List<JoinBehavior> joinBehaviors;

    public JoinListener(JoinBehavior... joinBehaviors) {
        this.joinBehaviors = new ArrayList<>(Arrays.asList(joinBehaviors));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(null);

        UHCJoinEvent uhcJoinEvent = new UHCJoinEvent(e);
        joinBehaviors.forEach(joinBehavior -> joinBehavior.onJoin(uhcJoinEvent));
        onPlayerJoin(uhcJoinEvent);
    }

    protected void onPlayerJoin(UHCJoinEvent e) {

    }
}
