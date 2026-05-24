/*
 * Copyright (C) 2026 Qdeeme.
 *
 * This file is part of "Xp Simplifier".
 *
 * "Xp Simplifier" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */



package qdeeme.xp_simplifier.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import qdeeme.xp_simplifier.Xp_Simplifier;
import qdeeme.xp_simplifier.permissions.XpsEditPermission;
import qdeeme.xp_simplifier.util.config.ConfigServerSync;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  C2S  –  Client requests a config change (op-only)
// ═══════════════════════════════════════════════════════════════════════════
public record ConfigUpdate(Map<String, Object> changes) implements CustomPacketPayload {

    private static final String WRONG_PCT = "network.xp_simplifier.config.invalid_packet_received";
    private static final String APPLIED = "message.xp_simplifier.config.applied";
    private static final String XPS = "message.xp_simplifier.command.edit_permission.xps_label";

    // serializeNulls() required so null entry values (= delete) survive toJson() on the client.
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    // Per-map chunk ceiling — safely under Short.MAX_VALUE (32 767 B) used by writeUtf default.
    private static final int MAX_BYTES = Short.MAX_VALUE;

    public static final Type<ConfigUpdate> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Xp_Simplifier.MOD_ID, "xps_config_update"));

    public static final StreamCodec<FriendlyByteBuf, ConfigUpdate> STREAM_CODEC =
            StreamCodec.of(ConfigUpdate::encode, ConfigUpdate::decode);

    private static void encode(FriendlyByteBuf buf, ConfigUpdate payload) {
        buf.writeUtf(GSON.toJson(payload.changes), Short.MAX_VALUE);
    }

    private static ConfigUpdate decode(FriendlyByteBuf buf) {
        String json = buf.readUtf(Short.MAX_VALUE);
        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        return new ConfigUpdate(map != null ? map : new HashMap<>());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── Handler (runs on SERVER main thread) ──────────────────────────────────
    public static void handle(ConfigUpdate payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();

            // ── Security: three-way priority (file > PermissionAPI > vanilla op) ──
            Boolean explicit = XpsEditPermission.canEditExplicit(player.getGameProfile().getName());
            if (!(explicit && player.hasPermissions(2))) {
                // No explicit entry and permission denied — unexpected packet from a read-only client;
                // disconnect to guard against a modified client bypassing the locked UI.
                ctx.disconnect(Component.translatable(WRONG_PCT));
                return;
            }

            // ── Lane A: flag/mode/multiplier changes ──────────────────────────
            Map<String, Object> flagDelta = new HashMap<>(payload.changes());
            flagDelta.remove("map_entries");
            if (!flagDelta.isEmpty()) {
                XpsConfig.applyFlagDelta(flagDelta);  // applies only the changed flags
                ConfigServerSync.save();
            }

            // ── Lane B: per-entry map mutations ───────────────────────────────
            Object me = payload.changes().get("map_entries");
            if (me instanceof Map<?, ?> mapEntries) {
                Path worldDir = ConfigServerSync.getWorldDir();
                if (worldDir != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedEntries = (Map<String, Object>) mapEntries;
                    XpsConfig.applyEntryDelta(typedEntries, worldDir);
                }
            }

            // ── Broadcast delta to all players ────────────────────────────────
            broadcastDelta(flagDelta, me);

            for (ServerPlayer target : ((ServerPlayer) ctx.player()).server.getPlayerList().getPlayers()) {
                if (target.hasPermissions(2)) {
                    target.sendSystemMessage(Component.translatable(XPS).append(Component.translatable(APPLIED, player.getDisplayName())));
                }
            }
        });
    }

    /**
     * Sends only to players who have the mod installed (avoids UnsupportedOperationException
     * from optional() registration crashing the server task mid-broadcast).
     */
    private static void sendToModdedPlayers(CustomPacketPayload packet) {
        for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            try {
                PacketDistributor.sendToPlayer(p, packet);
            } catch (UnsupportedOperationException ignored) {
                // Vanilla client — no mod, skip silently
            } catch (java.util.concurrent.CompletionException e) {
                if (!(e.getCause() instanceof UnsupportedOperationException)) {
                    throw e; // re-throw unexpected errors
                }
            }
        }
    }

    /**
     * Sends flag changes as one small packet; map-entry changes batched by map in ≤32 KB chunks.
     */
    private static void broadcastDelta(Map<String, Object> flagDelta, Object mapEntriesRaw) {
        if (!flagDelta.isEmpty()) {
            sendToModdedPlayers(new ConfigDelta(flagDelta));
        }
        if (!(mapEntriesRaw instanceof Map<?, ?> mapEntries)) return;
        for (Map.Entry<?, ?> mEntry : mapEntries.entrySet()) {
            String mapName = (String) mEntry.getKey();
            if (!(mEntry.getValue() instanceof Map<?, ?> catChanges)) continue;
            Map<String, Object> chunk = new LinkedHashMap<>();
            int chunkBytes = 0;
            for (Map.Entry<?, ?> cEntry : catChanges.entrySet()) {
                String catName = (String) cEntry.getKey();
                String catJson = GSON.toJson(cEntry.getValue());
                if (chunkBytes + catJson.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES && !chunk.isEmpty()) {
                    flushMapChunk(mapName, chunk);
                    chunk = new LinkedHashMap<>();
                    chunkBytes = 0;
                }
                chunk.put(catName, cEntry.getValue());
                chunkBytes += catJson.getBytes(StandardCharsets.UTF_8).length;
            }
            if (!chunk.isEmpty()) flushMapChunk(mapName, chunk);
        }
    }

    private static void flushMapChunk(String mapName, Map<String, Object> chunk) {
        Map<String, Object> mapSection = new LinkedHashMap<>();
        mapSection.put(mapName, chunk);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("map_entries", mapSection);
        sendToModdedPlayers(new ConfigDelta(payload));
    }
}
