/*
 * This file is part of HuskChat, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskchat.config;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.william278.huskchat.HuskChat;
import net.william278.huskchat.channel.Channel;
import net.william278.huskchat.user.OnlineUser;
import net.william278.huskchat.user.UserCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.logging.Level;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Locales {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      HuskChat - Locales      ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ See plugin about menu for international locale credits
            ┗╸ Translate HuskClaims: https://william278.net/docs/huskchat/translations""";

    private static final String SILENT_JOIN_PERMISSION = "huskchat.silent_join";
    private static final String SILENT_QUIT_PERMISSION = "huskchat.silent_quit";
    private static final String FORMATTED_CHAT_PERMISSION = "huskchat.formatted_chat";
    static final String DEFAULT_LOCALE = "en-gb";

    // The raw set of locales loaded from yaml
    Map<String, String> locales = new TreeMap<>();

    @Nullable
    public String getRawLocale(@NotNull String id) {
        return locales.get(id);
    }

    public void sendMessage(@NotNull OnlineUser player, @NotNull String id, @NotNull String... replacements) {
        String locale = getRawLocale(id);

        // Don't send empty messages
        if (locale == null || locale.isEmpty()) {
            return;
        }

        // Replace placeholders
        int replacementIndexer = 1;
        for (String replacement : replacements) {
            String replacementString = "%" + replacementIndexer + "%";
            locale = locale.replace(replacementString, replacement);
            replacementIndexer = replacementIndexer + 1;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize(locale));
    }

    public void sendChannelMessage(@NotNull OnlineUser target, @NotNull OnlineUser sender, @NotNull Channel channel,
                                   @NotNull String message, @NotNull HuskChat plugin) {
        plugin.replacePlaceholders(sender, channel.getFormat()).thenAccept(replaced -> {
            StringBuilder replacedBuilder = new StringBuilder(replaced);
            if (sender.hasPermission(FORMATTED_CHAT_PERMISSION, false)) {
                replacedBuilder.append(message);
            } else {
                replacedBuilder.append(MiniMessage.miniMessage().escapeTags(message));
            }
            target.sendMessage(MiniMessage.miniMessage().deserialize(replacedBuilder.toString()));
        });
    }

    public void sendOutboundPrivateMessage(@NotNull OnlineUser sender, @NotNull List<OnlineUser> recipients,
                                           @NotNull String message, @NotNull HuskChat plugin) {
        plugin.replacePlaceholders(recipients.get(0), recipients.size() == 1
                ? plugin.getSettings().getMessageCommand().getFormat().getOutbound()
                : plugin.getSettings().getMessageCommand().getFormat().getGroupOutbound()
        ).thenAccept(replaced -> {
            if (recipients.size() > 1) {
                replaced = replaced.replace("%group_amount_subscript%", superscriptNumber(recipients.size() - 1))
                        .replace("%group_amount%", Integer.toString(recipients.size() - 1))
                        .replace("%group_members_comma_separated%", getGroupMemberList(recipients, ", "))
                        .replace("%group_members%", MiniMessage.miniMessage().escapeTags(getGroupMemberList(recipients, "\n")));
            }

            StringBuilder replacedBuilder = new StringBuilder();
            if (sender.hasPermission(FORMATTED_CHAT_PERMISSION, false)) {
                replacedBuilder.append(replaced).append(message);
            } else {
                replacedBuilder.append(replaced).append(MiniMessage.miniMessage().escapeTags(message));
            }

            sender.sendMessage(MiniMessage.miniMessage().deserialize(replacedBuilder.toString()));
        });
    }

    // Gets the last TextColor from a component
    @Nullable
    private TextColor getFormatColor(@NotNull Component component) {
        // get the last color in the format
        TextColor color = component.color();
        if (component.children().isEmpty()) {
            return color;
        }
        for (Component child : component.children()) {
            TextColor childColor = getFormatColor(child);
            if (childColor != null) {
                color = childColor;
            }
        }
        return color;
    }

    public void sendInboundPrivateMessage(@NotNull List<OnlineUser> recipients, @NotNull OnlineUser sender,
                                          @NotNull String message, @NotNull HuskChat plugin) {
        plugin.replacePlaceholders(sender, recipients.size() == 1
                ? plugin.getSettings().getMessageCommand().getFormat().getInbound()
                : plugin.getSettings().getMessageCommand().getFormat().getGroupInbound()
        ).thenAccept(replaced -> {
            if (recipients.size() > 1) {
                replaced = replaced.replace("%group_amount_subscript%", superscriptNumber(recipients.size() - 1))
                        .replace("%group_amount%", Integer.toString(recipients.size() - 1))
                        .replace("%group_members_comma_separated%", getGroupMemberList(recipients, ", "))
                        .replace("%group_members%", MiniMessage.miniMessage().escapeTags(getGroupMemberList(recipients, "\n")));
            }

            StringBuilder replacedBuilder = new StringBuilder(replaced);
            if (sender.hasPermission(FORMATTED_CHAT_PERMISSION, false)) {
                replacedBuilder.append(message);
            } else {
                replacedBuilder.append(MiniMessage.miniMessage().escapeTags(message));
            }

            for (final OnlineUser recipient : recipients) {
                recipient.sendMessage(MiniMessage.miniMessage().deserialize(replacedBuilder.toString()));
            }
        });
    }

    public void sendLocalSpy(@NotNull OnlineUser spy, @NotNull UserCache.SpyColor spyColor, @NotNull OnlineUser sender,
                             @NotNull Channel channel, @NotNull String message, @NotNull HuskChat plugin) {
        plugin.replacePlaceholders(sender, plugin.getSettings().getLocalSpy().getFormat())
                .thenAccept(replaced -> {
                    final TextComponent.Builder componentBuilder = Component.text()
                            .append(MiniMessage.miniMessage().deserialize(replaced.replace("%spy_color%", "<" + spyColor.name().toLowerCase() + ">")
                                    .replace("%channel%", channel.getId()) +
                                    MiniMessage.miniMessage().escapeTags(message)));
                    spy.sendMessage(componentBuilder.build());
                });
    }

    public void sendSocialSpy(@NotNull OnlineUser spy, @NotNull UserCache.SpyColor spyColor, @NotNull OnlineUser sender,
                              @NotNull List<OnlineUser> receivers, @NotNull String message, @NotNull HuskChat plugin) {
        plugin.replacePlaceholders(sender, receivers.size() == 1
                ? plugin.getSettings().getSocialSpy().getFormat()
                : plugin.getSettings().getSocialSpy().getGroupFormat()
                .replace("%sender_", "%")
        ).thenAccept(senderReplaced -> plugin.replacePlaceholders(receivers.get(0), senderReplaced
                .replace("%receiver_", "%")
        ).thenAccept(replaced -> {
            if (receivers.size() > 1) {
                replaced = replaced.replace("%group_amount_subscript%", superscriptNumber(receivers.size() - 1))
                        .replace("%group_amount%", Integer.toString(receivers.size() - 1))
                        .replace("%group_members_comma_separated%", getGroupMemberList(receivers, ","))
                        .replace("%group_members%", MiniMessage.miniMessage().escapeTags(getGroupMemberList(receivers, "\n")));
            }
            spy.sendMessage(MiniMessage.miniMessage().deserialize(
                    replaced.replace("%spy_color%", "<" + spyColor.name().toLowerCase() + ">") + MiniMessage.miniMessage().escapeTags(message)
            ));
        }));
    }

    public void sendJoinMessage(@NotNull OnlineUser player, @NotNull HuskChat plugin) {
        if (player.hasPermission(SILENT_JOIN_PERMISSION, false)) {
            return;
        }
        plugin.replacePlaceholders(player,
                        plugin.getDataGetter().getTextFromNode(player, "huskchat.join_message")
                                .orElse(plugin.getSettings().getJoinAndQuitMessages().getJoin().getFormat()))
                .thenAccept(replaced -> sendJoinQuitMessage(player, MiniMessage.miniMessage().deserialize(replaced), plugin));
    }

    public void sendQuitMessage(@NotNull OnlineUser player, @NotNull HuskChat plugin) {
        if (player.hasPermission(SILENT_QUIT_PERMISSION, false)) {
            return;
        }
        plugin.replacePlaceholders(player,
                        plugin.getDataGetter().getTextFromNode(player, "huskchat.quit_message")
                                .orElse(plugin.getSettings().getJoinAndQuitMessages().getQuit().getFormat()))
                .thenAccept(replaced -> sendJoinQuitMessage(player, MiniMessage.miniMessage().deserialize(replaced), plugin));
    }

    // Dispatch a join/quit message to the correct server
    private void sendJoinQuitMessage(@NotNull OnlineUser player, @NotNull Component component,
                                     @NotNull HuskChat plugin) {
        boolean local = List.of(Channel.BroadcastScope.LOCAL, Channel.BroadcastScope.LOCAL_PASSTHROUGH)
                .contains(plugin.getSettings().getJoinAndQuitMessages().getBroadcastScope());
        for (OnlineUser online : plugin.getOnlinePlayers()) {
            if (local && !online.getServerName().equals(player.getServerName())) {
                continue;
            }
            online.sendMessage(component);
        }
    }

    // Returns a newline-separated list of player names
    @NotNull
    public final String getGroupMemberList(@NotNull List<OnlineUser> players, @NotNull String delimiter) {
        final StringJoiner memberList = new StringJoiner(delimiter);
        for (OnlineUser player : players) {
            memberList.add(player.getName());
        }
        return memberList.toString();
    }

    // Get the corresponding subscript unicode character from a normal one
    @NotNull
    public final String superscriptNumber(int number) {
        final String[] digits = {"₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"};
        final StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(number).toCharArray()) {
            sb.append(digits[Integer.parseInt(String.valueOf(c))]);
        }
        return sb.toString();
    }

}
