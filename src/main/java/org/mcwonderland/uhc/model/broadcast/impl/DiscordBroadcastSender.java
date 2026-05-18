package org.mcwonderland.uhc.model.broadcast.impl;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.model.broadcast.AbstractBroadcastSender;
import org.mcwonderland.uhc.model.broadcast.BroadcastDeliveryException;

import java.util.EnumSet;
import java.util.List;

/**
 * 2019-11-20 下午 07:49
 */

public class DiscordBroadcastSender extends AbstractBroadcastSender {
    private final List<String> channelIds;
    private final String invalidChannel = getString("Invalid_Channel");

    public DiscordBroadcastSender() {
        super("Discord");

        this.channelIds = getStringList("Channel_Ids");
    }

    @Override
    protected void send(List<String> messages) {
        if (!DiscordSRV.isReady)
            throw new BroadcastDeliveryException("&cDiscordSRV 尚未完成啟動，請稍後再試。");

        channelIds.forEach(channel -> {
            TextChannel textChannel = DiscordUtil.getTextChannelById(channel);

            if (textChannel == null)
                throw new BroadcastDeliveryException(invalidChannel);

            String msg = getFormatterMessage(messages, textChannel);

            try {
                textChannel.sendMessage(msg)
                        .allowedMentions(EnumSet.of(
                                Message.MentionType.USER,
                                Message.MentionType.ROLE,
                                Message.MentionType.HERE,
                                Message.MentionType.EVERYONE))
                        .complete();
            } catch (RuntimeException e) {
                throw new BroadcastDeliveryException("&cDiscord公告發送失敗: " + e.getMessage());
            }
        });
    }

    private String getFormatterMessage(List<String> messages, TextChannel textChannel) {
        String result = String.join("\n", messages);
        result = DiscordUtil.convertMentionsFromNames(result, textChannel.getGuild());

        return result;
    }

    @Override
    public Dependency getDependency() {
        return Dependency.DISCORD_SRV;
    }
}
