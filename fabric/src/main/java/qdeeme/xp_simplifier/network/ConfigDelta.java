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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  S2C  –  Server pushes only what changed to all connected clients
//
//  serializeNulls() is required so null entry values (= delete) survive toJson().
// ═══════════════════════════════════════════════════════════════════════════
public record ConfigDelta(Map<String, Object> changes) implements CustomPayload {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static final Id<ConfigDelta> ID =
            new Id<>(Identifier.of("xp_simplifier", "xps_delta_update"));

    public static final PacketCodec<PacketByteBuf, ConfigDelta> CODEC =
            PacketCodec.of(ConfigDelta::encode, ConfigDelta::decode);

    private static void encode(ConfigDelta payload, PacketByteBuf buf) {
        buf.writeString(GSON.toJson(payload.changes()), Short.MAX_VALUE);
    }

    private static ConfigDelta decode(PacketByteBuf buf) {
        String json = buf.readString(Short.MAX_VALUE);
        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        return new ConfigDelta(map != null ? map : new HashMap<>());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

