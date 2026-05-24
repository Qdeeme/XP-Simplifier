package qdeeme.xp_simplifier.permissions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import qdeeme.xp_simplifier.network.ConfigDelta;
import qdeeme.xp_simplifier.network.ConfigSync;
import qdeeme.xp_simplifier.permissions.XpsEditPermission;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.ConfigServerSync;

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
    private static void notifyPlayer(ServerCommandSource source, String playerName) {
        for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
            if (p.getName().getString().equals(playerName)) {
                trySend(p, ConfigSync.forPlayer(p));
                break;
            }
        }
    }

    /**
     * Broadcasts a payload to every online player, silently skipping vanilla clients.
     */
    private static void broadcastDelta(ServerCommandSource source, CustomPayload payload) {
        for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
            trySend(p, payload);
        }
    }

    /**
     * Sends a packet to a player; swallows non-connected player states silently.
     */
    private static void trySend(ServerPlayerEntity player, CustomPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.getId())) {
            ServerPlayNetworking.send(player, payload);
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


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("xps")
                        .requires(s -> s.hasPermissionLevel(2))

                        // /xps add <player> <true|false>
                        .then(CommandManager.literal("add")
                                .requires(s -> (s.hasPermissionLevel(2)))
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .then(CommandManager.argument("Can edit", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean canEdit = BoolArgumentType.getBool(ctx, "Can edit");
                                                    Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, "player");
                                                    if (profiles.isEmpty()) {
                                                        ctx.getSource().sendError(Text.translatable(XPS).append(Text.translatable(NOT_FOUND, "player")));
                                                        return 0;
                                                    }
                                                    Path worldDir = ConfigServerSync.getWorldDir();
                                                    if (worldDir == null) {
                                                        ctx.getSource().sendError(Text.translatable(XPS).append(Text.translatable(EXCE_COMM)));
                                                        return 0;
                                                    }
                                                    for (GameProfile profile : profiles) {
                                                        XpsEditPermission.set(profile.getName(), canEdit, worldDir);
                                                        notifyPlayer(ctx.getSource(), profile.getName());
                                                        if (profile.getName() != null) {
                                                            ctx.getSource().sendFeedback(
                                                                    () -> Text.translatable(XPS).append(Text.translatable(canEdit ? CAN_ON : CAN_OFF, profile.getName())), true);
                                                        } else {
                                                            ctx.getSource().sendFeedback(
                                                                    () -> Text.translatable(NOT_FOUND, profile.getName()), true);
                                                        }
                                                    }
                                                    return profiles.size();
                                                }))))

                        // /xps edit <player> <true|false>  (upsert — same as add)
                        .then(CommandManager.literal("edit")
                                .requires(s -> s.hasPermissionLevel(2))
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .then(CommandManager.argument("Can edit", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean canEdit = BoolArgumentType.getBool(ctx, "Can edit");
                                                    Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, "player");
                                                    if (profiles.isEmpty()) {
                                                        ctx.getSource().sendError(Text.translatable(XPS).append(Text.translatable(NOT_FOUND, "player")));
                                                        return 0;
                                                    }
                                                    Path worldDir = ConfigServerSync.getWorldDir();
                                                    if (worldDir == null) {
                                                        ctx.getSource().sendError(Text.translatable(XPS).append(Text.translatable(EXCE_COMM)));
                                                        return 0;
                                                    }
                                                    for (GameProfile profile : profiles) {
                                                        if (XpsEditPermission.getEntries().containsKey(profile.getName())) {
                                                            XpsEditPermission.set(profile.getName(), canEdit, worldDir);
                                                            notifyPlayer(ctx.getSource(), profile.getName());
                                                            ctx.getSource().sendFeedback(
                                                                    () -> Text.translatable(XPS).append(Text.translatable(canEdit ? CAN_ON : CAN_OFF, profile.getName())), true);
                                                        } else {
                                                            ctx.getSource().sendError(
                                                                    Text.translatable(XPS).append(Text.translatable(NOT_FOUND, profile.getName())));
                                                            return 0;
                                                        }
                                                    }
                                                    return 1;
                                                }))))

                        // /xps list
                        .then(CommandManager.literal("list")
                                .requires(s -> s.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    Map<String, Boolean> entries = XpsEditPermission.getEntries();
                                    if (entries.isEmpty()) {
                                        ctx.getSource().sendFeedback(
                                                () -> Text.translatable(XPS).append(Text.translatable(XPS_LIST_EMPTY)),
                                                true);
                                    } else {
                                        StringBuilder sb = new StringBuilder();
                                        for (Map.Entry<String, Boolean> entry : entries.entrySet()) {
                                            String formatted;

                                            // Try to find the player online to get their permission level
                                            ServerPlayerEntity online = ctx.getSource().getServer().getPlayerManager()
                                                    .getPlayerList()
                                                    .stream()
                                                    .filter(p -> p.getName().getString().equals(entry.getKey()))
                                                    .findFirst()
                                                    .orElse(null);

                                            if (online != null) {
                                                if (online.hasPermissionLevel(4)) {
                                                    formatted = "§7[4]§r §6" + entry.getKey() + "§r";
                                                } else if (online.hasPermissionLevel(3)) {
                                                    formatted = "§7[3]§r §9" + entry.getKey() + "§r";
                                                } else if (online.hasPermissionLevel(2)) {
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
                                        ctx.getSource().sendFeedback(
                                                () -> Text.translatable(XPS).append(Text.translatable(XPS_LIST, entries.size())).append("\n").append(sb.toString()),
                                                true);
                                    }
                                    return 1;
                                }))
                        .then(CommandManager.literal("delete")
                                .requires(s -> s.hasPermissionLevel(2))
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(ctx -> {
                                            Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, "player");
                                            if (profiles.isEmpty()) {
                                                ctx.getSource().sendError(Text.translatable(XPS).append(Text.translatable(NOT_FOUND, "player")));
                                                return 0;
                                            }
                                            Path worldDir = ConfigServerSync.getWorldDir();
                                            if (worldDir == null) {
                                                ctx.getSource().sendError(Text.translatable(XPS).append(Text.translatable(EXCE_COMM)));
                                                return 0;
                                            }
                                            int removed = 0;
                                            for (GameProfile profile : profiles) {
                                                if (XpsEditPermission.remove(profile.getName(), worldDir)) {
                                                    notifyPlayer(ctx.getSource(), profile.getName());
                                                    if (profile.getName() != null) {
                                                        ctx.getSource().sendFeedback(
                                                                () -> Text.translatable(XPS).append(Text.translatable(DEL_COMM, profile.getName())),
                                                                true);
                                                        removed++;
                                                    } else {
                                                        ctx.getSource().sendFeedback(
                                                                () -> Text.translatable(XPS).append(Text.translatable(NOT_FOUND, profile.getName())), true);
                                                    }
                                                } else {
                                                    ctx.getSource().sendError(
                                                            Text.translatable(XPS).append(Text.translatable(NOT_FOUND, profile.getName())));
                                                }
                                            }
                                            return removed;
                                        })))

                        // /xps mapview <true|false>
                        .then(CommandManager.literal("mapview")
                                .requires(s -> s.hasPermissionLevel(2))
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            Text enabledText = Text.translatable(enabled ? MAP_ON : MAP_OFF);
                                            Path worldDir = ConfigServerSync.getWorldDir();
                                            if (worldDir == null) {
                                                ctx.getSource().sendError(Text.translatable(XPS).append(Text.translatable(EXCE_COMM)));
                                                return 0;
                                            }
                                            Config.setMapViewEnabled(enabled, worldDir);
                                            // Broadcast the flag change to all modded clients; vanilla clients silently skipped.
                                            broadcastDelta(ctx.getSource(), new ConfigDelta(Map.of("mapViewEnabled", enabled)));
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.translatable(XPS).append(Text.translatable(MAPVIEW_COMM, enabledText)),
                                                    true);
                                            return 1;
                                        })))
        );
    }
}

