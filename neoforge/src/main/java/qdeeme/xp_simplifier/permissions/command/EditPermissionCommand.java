package qdeeme.xp_simplifier.permissions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import qdeeme.xp_simplifier.network.ConfigSync;
import qdeeme.xp_simplifier.permissions.XpsEditPermission;
import qdeeme.xp_simplifier.util.config.ConfigServerSync;
import qdeeme.xp_simplifier.util.config.XpsConfig;

import qdeeme.xp_simplifier.network.ConfigDelta;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Registers {@code /xps} sub-commands for managing per-world edit permissions.
 *
 * <pre>
 * /xps add     &lt;player&gt; &lt;true|false&gt;  – upsert player edit permission
 * /xps edit    &lt;player&gt; &lt;true|false&gt;  – alias for add
 * /xps delete  &lt;player&gt;               – remove player entry
 * /xps mapview &lt;true|false&gt;           – toggle map-tab visibility globally
 * </pre>
 * <p>
 * Access is gated by vanilla op levels; LuckPerms intercepts those checks
 * automatically through its own command-node system ({@code command.xps.*}).
 */
public final class EditPermissionCommand {
    private EditPermissionCommand() {
    }

    /**
     * Sends a fresh ConfigSync to a specific online player, silently skipping vanilla clients.
     */
    private static void notifyPlayer(CommandSourceStack source, String Name) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(Name);
        if (target != null) {
            trySend(target, ConfigSync.forPlayer(target));
        }
    }

    /**
     * Broadcasts a ConfigDelta to every online player, silently skipping vanilla clients.
     */
    private static void broadcastDelta(CommandSourceStack source, CustomPacketPayload payload) {
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            trySend(p, payload);
        }
    }

    /**
     * Sends a packet to a player; swallows UnsupportedOperationException for vanilla (no-mod) clients.
     */
    private static void trySend(ServerPlayer player, CustomPacketPayload payload) {
        try {
            PacketDistributor.sendToPlayer(player, payload);
        } catch (UnsupportedOperationException ignored) {
            // Client does not have the mod — config sync is optional for vanilla clients.
        }
    }

    private static final String DEL_COMM = "message.xp_simplifier.command.edit_permission.delete";
    private static final String EXCE_COMM = "message.xp_simplifier.command.edit_permission.exception";
    private static final String MAPVIEW_COMM = "message.xp_simplifier.command.edit_permission.mapview";
    private static final String NOT_FOUND = "message.xp_simplifier.command.edit_permission.notfound";
    private static final String XPS_LIST = "message.xp_simplifier.command.edit_permission.list";
    private static final String XPS_LIST_EMPTY = "message.xp_simplifier.command.edit_permission.list.empty";
    private static final String MAP_ON = "message.xp_simplifier.command.edit_permission.mapview.on";
    private static final String MAP_OFF = "message.xp_simplifier.command.edit_permission.mapview.off";
    private static final String CAN_ON = "message.xp_simplifier.command.edit_permission.can.on";
    private static final String CAN_OFF = "message.xp_simplifier.command.edit_permission.can.off";
    private static final String XPS = "message.xp_simplifier.command.edit_permission.xps_label";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("xps")
                        .requires(s -> s.hasPermission(2))  // visible to op 2+; sub-commands gate further

                        // /xps add <player> <true|false>
                        .then(Commands.literal("add")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("Can edit", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean canEdit = BoolArgumentType.getBool(ctx, "Can edit");
                                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                                    if (profiles.isEmpty()) {
                                                        ctx.getSource().sendFailure(Component.translatable(XPS).append(Component.translatable(NOT_FOUND, "player")));
                                                        return 0;
                                                    }
                                                    Path worldDir = ConfigServerSync.getWorldDir();
                                                    if (worldDir == null) {
                                                        ctx.getSource().sendFailure(Component.translatable(XPS).append(Component.translatable(EXCE_COMM)));
                                                        return 0;
                                                    }
                                                    for (GameProfile profile : profiles) {
                                                        XpsEditPermission.set(profile.getName(), canEdit, worldDir);
                                                        notifyPlayer(ctx.getSource(), profile.getName());
                                                        if (profile.getName() != null) {
                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.translatable(XPS).append(Component.translatable(canEdit ? CAN_ON : CAN_OFF, profile.getName())), true);
                                                        } else {
                                                            ctx.getSource().sendFailure(
                                                                    Component.translatable(XPS).append(Component.translatable(NOT_FOUND, profile.getName(), canEdit)));
                                                            return 0;
                                                        }
                                                    }
                                                    return 1;
                                                }))))

                        // /xps edit <player> <true|false>  (upsert — same as add)
                        .then(Commands.literal("edit")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("Can edit", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean canEdit = BoolArgumentType.getBool(ctx, "Can edit");
                                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                                    if (profiles.isEmpty()) {
                                                        ctx.getSource().sendFailure(Component.translatable(XPS).append(Component.translatable(NOT_FOUND, "player")));
                                                        return 0;
                                                    }
                                                    Path worldDir = ConfigServerSync.getWorldDir();
                                                    if (worldDir == null) {
                                                        ctx.getSource().sendFailure(Component.translatable(XPS).append(Component.translatable(EXCE_COMM)));
                                                        return 0;
                                                    }
                                                    for (GameProfile profile : profiles) {
                                                        if (XpsEditPermission.getEntries().containsKey(profile.getName())) {
                                                            XpsEditPermission.set(profile.getName(), canEdit, worldDir);
                                                            notifyPlayer(ctx.getSource(), profile.getName());
                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.translatable(XPS).append(Component.translatable(canEdit ? CAN_ON : CAN_OFF, profile.getName())), true);
                                                        } else {
                                                            ctx.getSource().sendFailure(
                                                                    Component.translatable(XPS).append(Component.translatable(NOT_FOUND, "player")));
                                                            return 0;
                                                        }
                                                    }
                                                    return 1;
                                                }))))

                        // /xps list
                        .then(Commands.literal("list")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    Map<String, Boolean> entries = XpsEditPermission.getEntries();
                                    if (entries.isEmpty()) {
                                        ctx.getSource().sendSuccess(
                                                () -> Component.translatable(XPS).append(Component.translatable(XPS_LIST_EMPTY)),
                                                true);
                                    } else {
                                        StringBuilder sb = new StringBuilder();
                                        for (Map.Entry<String, Boolean> entry : entries.entrySet()) {
                                            String formatted;

                                            // Try to find the player online to get their permission level
                                            ServerPlayer online = ctx.getSource().getServer().getPlayerList()
                                                    .getPlayers()
                                                    .stream()
                                                    .filter(p -> p.getName().getString().equals(entry.getKey()))
                                                    .findFirst()
                                                    .orElse(null);

                                            if (online != null) {
                                                if (online.hasPermissions(4)) {
                                                    formatted = "§7[4]§r §6" + entry.getKey() + "§r";
                                                } else if (online.hasPermissions(3)) {
                                                    formatted = "§7[3]§r §9" + entry.getKey() + "§r";
                                                } else if (online.hasPermissions(2)) {
                                                    formatted = "§7[2]§r §3" + entry.getKey() + "§r";
                                                } else {
                                                    formatted = "§7[0]§r §f" + entry.getKey() + "§r";
                                                }
                                            } else {
                                                // Offline — no permission level available
                                                formatted = "§8[?]§r §7" + entry.getKey() + "§r";
                                            }

                                            String s = entry.getValue() ? "§atrue§r" : "§cfalse§r";
                                            sb.append("  • ").append(formatted).append(" → ").append(s).append("\n");
                                        }
                                        ctx.getSource().sendSuccess(
                                                () -> Component.translatable(XPS).append(Component.translatable(XPS_LIST, entries.size())).append("\n").append(sb.toString()),
                                                true);
                                    }
                                    return 1;
                                }))

                        // /xps delete <player>
                        .then(Commands.literal("delete")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> {
                                            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                            if (profiles.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.translatable(XPS).append(Component.translatable(NOT_FOUND, "player")));
                                                return 0;
                                            }
                                            Path worldDir = ConfigServerSync.getWorldDir();
                                            if (worldDir == null) {
                                                ctx.getSource().sendFailure(Component.translatable(XPS).append(Component.translatable(EXCE_COMM)));
                                                return 0;
                                            }
                                            int removed = 0;
                                            for (GameProfile profile : profiles) {
                                                if (XpsEditPermission.remove(profile.getName(), worldDir)) {
                                                    notifyPlayer(ctx.getSource(), profile.getName());
                                                    if (profile.getName() != null) {
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.translatable(XPS).append(Component.translatable(DEL_COMM, profile.getName())),
                                                                true);
                                                        removed++;
                                                    } else {
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.translatable(XPS).append(Component.translatable(NOT_FOUND, profile.getName())), true);
                                                    }
                                                } else {
                                                    ctx.getSource().sendFailure(
                                                            Component.translatable(XPS).append(Component.translatable(NOT_FOUND, profile.getName())));
                                                }
                                            }
                                            return removed;
                                        })))

                        // /xps mapview <true|false>
                        .then(Commands.literal("mapview")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            Component enabledText = Component.translatable(enabled ? MAP_ON : MAP_OFF);
                                            Path worldDir = ConfigServerSync.getWorldDir();
                                            if (worldDir == null) {
                                                ctx.getSource().sendFailure(Component.translatable(XPS).append(Component.translatable(EXCE_COMM)));
                                                return 0;
                                            }
                                            XpsConfig.setMapViewEnabled(enabled, worldDir);
                                            broadcastDelta(ctx.getSource(), new ConfigDelta(Map.of("mapViewEnabled", enabled)));
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable(XPS).append(Component.translatable(MAPVIEW_COMM, enabledText)),
                                                    true);
                                            return 1;
                                        })))
        );
    }
}
