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
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import qdeeme.xp_simplifier.permissions.XpsEditPermission;
import qdeeme.xp_simplifier.permissions.XpsPermissions;
import qdeeme.xp_simplifier.util.ConfigServerSync;

import java.util.HashMap;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  S2C  –  Server pushes full config snapshot to a client
// ═══════════════════════════════════════════════════════════════════════════
public record ConfigSync(Map<String, Object> values) implements CustomPayload {

    private static final Gson GSON = new Gson();

    /**
     * Reserved key injected server-side to carry the per-player edit permission result.
     */
    public static final String CAN_EDIT_KEY = "_xps_can_edit";
    public static final String CAN_EDIT_EXPLICIT_KEY = "_xps_explicit";

    public static final Id<ConfigSync> ID =
            new Id<>(Identifier.of("xp_simplifier", "xps_config_sync"));

    public static final PacketCodec<PacketByteBuf, ConfigSync> CODEC =
            PacketCodec.of(ConfigSync::encode, ConfigSync::decode);

    private static void encode(ConfigSync payload, PacketByteBuf buf) {
        buf.writeString(GSON.toJson(payload.values()), Short.MAX_VALUE);
    }

    private static ConfigSync decode(PacketByteBuf buf) {
        String json = buf.readString(Short.MAX_VALUE);
        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        return new ConfigSync(map != null ? map : new HashMap<>());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Per-player factory — includes the server-computed {@code canEdit} result so the
     * client screen can reflect permissions without a client-side PermissionAPI.
     */
    public static ConfigSync forPlayer(ServerPlayerEntity player) {
        Map<String, Object> values = new HashMap<>(ConfigServerSync.toMap());
        values.put(CAN_EDIT_KEY, XpsPermissions.canEdit(player));
        Boolean explicit = XpsEditPermission.canEditExplicit(player.getGameProfile().getName());
        if (explicit != null) values.put(CAN_EDIT_EXPLICIT_KEY, explicit);
        return new ConfigSync(values);
    }
}

