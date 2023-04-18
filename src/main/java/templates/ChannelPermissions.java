package templates;

import net.dv8tion.jda.api.Permission;

import java.util.EnumSet;
import java.util.stream.Collectors;

public class ChannelPermissions {
    public static EnumSet<Permission> all = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MANAGE_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_EXT_STICKER,
            Permission.MESSAGE_MENTION_EVERYONE,
            Permission.MESSAGE_MANAGE,
            Permission.MANAGE_THREADS,
            Permission.MESSAGE_HISTORY,
            Permission.USE_APPLICATION_COMMANDS
    );

    public static EnumSet<Permission> readAndReactAllow = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI
    );

    public static EnumSet<Permission> readAndReactDeny = all.stream()
            .filter(permission -> !readAndReactAllow.contains(permission))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));

    public static EnumSet<Permission> readOnlyAllow = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_HISTORY
    );

    public static EnumSet<Permission> readOnlyDeny = all.stream()
            .filter(permission -> !readOnlyAllow.contains(permission))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));

    public static EnumSet<Permission> readWriteAllow = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_EXT_STICKER,
            Permission.MESSAGE_MENTION_EVERYONE,
            Permission.MESSAGE_HISTORY
    );

    public static EnumSet<Permission> readWriteDeny = all.stream()
            .filter(permission -> !readWriteAllow.contains(permission))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));

    public static EnumSet<Permission> pollBotAllow = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_HISTORY
    );

    public static EnumSet<Permission> pollBotDeny = all.stream()
            .filter(permission -> !pollBotAllow.contains(permission))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));
}
