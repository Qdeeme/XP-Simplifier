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
import org.jetbrains.annotations.NotNull;
import qdeeme.xp_simplifier.Xp_Simplifier;
import qdeeme.xp_simplifier.util.config.ConfigServerSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  S2C  –  Server pushes only what changed to all connected clients
//
//  Sent after every op Apply action; never on login (ConfigSync handles that).
//  serializeNulls() is required so null entry values (= delete) survive toJson().
// ═══════════════════════════════════════════════════════════════════════════
public record ConfigDelta(Map<String, Object> changes) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static final Type<ConfigDelta> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Xp_Simplifier.MOD_ID, "xps_delta_update"));

    public static final StreamCodec<FriendlyByteBuf, ConfigDelta> STREAM_CODEC =
            StreamCodec.of(ConfigDelta::encode, ConfigDelta::decode);

    private static void encode(FriendlyByteBuf buf, ConfigDelta payload) {
        buf.writeUtf(GSON.toJson(payload.changes()), Short.MAX_VALUE);
    }

    private static ConfigDelta decode(FriendlyByteBuf buf) {
        String json = buf.readUtf(Short.MAX_VALUE);
        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        return new ConfigDelta(map != null ? map : new HashMap<>());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── Handler (runs on CLIENT main thread) ──────────────────────────────────
    public static void handle(ConfigDelta payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ConfigServerSync.applyClientDelta(payload.changes()));
    }
}

