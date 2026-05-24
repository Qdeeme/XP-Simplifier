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
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import qdeeme.xp_simplifier.permissions.XpsEditPermission;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.ConfigServerSync;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  C2S  –  Client requests a config change (op-only)
// ═══════════════════════════════════════════════════════════════════════════
public record ConfigUpdate(Map<String, Object> changes) implements CustomPayload {

    private static final String WRONG_PCT = "network.xp_simplifier.config.invalid_packet_received";
    private static final String APPLIED = "message.xp_simplifier.config.applied";
    private static final String XPS = "message.xp_simplifier.command.edit_permission.xps_label";

    // serializeNulls() required so null entry values (= delete) survive toJson() on the client.
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final int MAX_BYTES = Short.MAX_VALUE;

    public static final Id<ConfigUpdate> ID =
            new Id<>(Identifier.of("xp_simplifier", "xps_config_update"));

    public static final PacketCodec<PacketByteBuf, ConfigUpdate> CODEC =
            PacketCodec.of(ConfigUpdate::encode, ConfigUpdate::decode);

    private static void encode(ConfigUpdate payload, PacketByteBuf buf) {
        buf.writeString(GSON.toJson(payload.changes()), Short.MAX_VALUE);
    }

    private static ConfigUpdate decode(PacketByteBuf buf) {
        String json = buf.readString(Short.MAX_VALUE);
        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        return new ConfigUpdate(map != null ? map : new HashMap<>());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    // ── Handler (runs on SERVER main thread) ──────────────────────────────────
    public static void handle(ConfigUpdate payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayerEntity player = context.player();

            // ── Security: three-way priority (file > op level) ────────────────
            Boolean explicit = XpsEditPermission.canEditExplicit(player.getGameProfile().getName());
            if (!(explicit && player.hasPermissionLevel(2))) {
                // Unexpected packet from a read-only client — disconnect to guard against bypass
                player.networkHandler.disconnect(Text.translatable(WRONG_PCT));
                return;
            }

            // ── Lane A: flag/mode/multiplier changes ──────────────────────────
            Map<String, Object> flagDelta = new HashMap<>(payload.changes());
            flagDelta.remove("map_entries");
            if (!flagDelta.isEmpty()) {
                Config.applyFlagDelta(flagDelta);
                ConfigServerSync.save();
            }

            // ── Lane B: per-entry map mutations ───────────────────────────────
            Object me = payload.changes().get("map_entries");
            if (me instanceof Map<?, ?> mapEntries) {
                Path worldDir = ConfigServerSync.getWorldDir();
                if (worldDir != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedEntries = (Map<String, Object>) mapEntries;
                    Config.applyEntryDelta(typedEntries, worldDir);
                }
            }

            // ── Broadcast delta to all players ────────────────────────────────
            broadcastDelta(context.server(), flagDelta, me);
            for (ServerPlayerEntity target : context.server().getPlayerManager().getPlayerList()) {
                if (target.hasPermissionLevel(2)) {
                    target.sendMessage(Text.translatable(XPS).append(Text.translatable(APPLIED, player.getDisplayName())));
                }
            }
        });
    }

    /**
     * Sends only to players who have the mod installed (avoids UnsupportedOperationException
     * from optional() registration crashing the server task mid-broadcast).
     */
    private static void sendToModdedPlayers(MinecraftServer server, CustomPayload packet) {
        for (ServerPlayerEntity p : PlayerLookup.all(server)) {
            if (ServerPlayNetworking.canSend(p, packet.getId())) {
                ServerPlayNetworking.send(p, packet);
            }
        }
    }

    private static void broadcastDelta(MinecraftServer server, Map<String, Object> flagDelta, Object mapEntriesRaw) {
        if (!flagDelta.isEmpty()) {
            sendToModdedPlayers(server, new ConfigDelta(flagDelta));
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
                    flushMapChunk(server, mapName, chunk);
                    chunk = new LinkedHashMap<>();
                    chunkBytes = 0;
                }
                chunk.put(catName, cEntry.getValue());
                chunkBytes += catJson.getBytes(StandardCharsets.UTF_8).length;
            }
            if (!chunk.isEmpty()) flushMapChunk(server, mapName, chunk);
        }
    }

    private static void flushMapChunk(MinecraftServer server, String mapName, Map<String, Object> chunk) {
        Map<String, Object> mapSection = new LinkedHashMap<>();
        mapSection.put(mapName, chunk);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("map_entries", mapSection);
        sendToModdedPlayers(server, new ConfigDelta(payload));
    }
}
