package org.mcwonderland.uhc.model.broadcast;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GameStartTimeInputSession {

    private static final Map<UUID, GameStartTimeInputSession> sessions = new ConcurrentHashMap<>();
    private static final String CANCELLED_MESSAGE = "<red>Discord 公告輸入已取消。</red>";

    private final GameStartTimeInfo.GameStartTimeInfoBuilder infoBuilder = GameStartTimeInfo.builder();
    private final Consumer<GameStartTimeInfo> onComplete;
    private Step step = Step.IP;

    private GameStartTimeInputSession(Consumer<GameStartTimeInfo> onComplete) {
        this.onComplete = onComplete;
    }

    public static void start(Player player, Consumer<GameStartTimeInfo> onComplete) {
        GameStartTimeInputSession session = new GameStartTimeInputSession(onComplete);

        sessions.put(player.getUniqueId(), session);
        player.closeInventory();
        session.sendPrompt(player);
    }

    public static boolean handleInput(Player player, String input) {
        GameStartTimeInputSession session = sessions.get(player.getUniqueId());

        if (session == null)
            return false;

        if (session.isCancelInput(input)) {
            sessions.remove(player.getUniqueId());
            Chat.sendConversing(player, CANCELLED_MESSAGE);
            return true;
        }

        session.accept(player, input);
        return true;
    }

    public static void clear(Player player) {
        sessions.remove(player.getUniqueId());
    }

    private void accept(Player player, String input) {
        switch (step) {
            case IP:
                infoBuilder.ip(input);
                step = Step.JOIN_TIME;
                sendPrompt(player);
                break;
            case JOIN_TIME:
                infoBuilder.joinTime(input);
                step = Step.START_TIME;
                sendPrompt(player);
                break;
            case START_TIME:
                infoBuilder.startTime(input);
                sessions.remove(player.getUniqueId());
                onComplete.accept(infoBuilder.build());
                break;
        }
    }

    private void sendPrompt(Player player) {
        switch (step) {
            case IP:
                Chat.sendConversing(player, Messages.Editor.Broadcast.IP);
                break;
            case JOIN_TIME:
                Chat.sendConversing(player, Messages.Editor.Broadcast.JOIN_TIME);
                break;
            case START_TIME:
                Chat.sendConversing(player, Messages.Editor.Broadcast.START_TIME);
                break;
        }
    }

    private boolean isCancelInput(String input) {
        return input.equalsIgnoreCase("quit")
                || input.equalsIgnoreCase("cancel")
                || input.equalsIgnoreCase("exit");
    }

    private enum Step {
        IP,
        JOIN_TIME,
        START_TIME
    }
}
