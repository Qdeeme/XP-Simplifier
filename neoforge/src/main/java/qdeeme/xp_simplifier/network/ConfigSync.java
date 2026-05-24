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
import org.jetbrains.annotations.NotNull;
import qdeeme.xp_simplifier.Xp_Simplifier;
import qdeeme.xp_simplifier.permissions.XpsPermissions;
import qdeeme.xp_simplifier.util.config.ConfigServerSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  S2C  –  Server pushes full config snapshot to a client
// ═══════════════════════════════════════════════════════════════════════════
public record ConfigSync(Map<String, Object> values) implements CustomPacketPayload {

    private static final Gson GSON = new Gson();

    /**
     * Reserved key injected server-side to carry the per-player edit permission result.
     */
    public static final String CAN_EDIT_KEY = "_xps_can_edit";

    /**
     * Reserved key carrying the explicit file entry for this player:
     * {@code true} = explicitly allowed, {@code false} = explicitly denied, absent = not in file.
     */
    public static final String CAN_EDIT_EXPLICIT_KEY = "_xps_explicit";

    public static final Type<ConfigSync> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Xp_Simplifier.MOD_ID, "xps_config_sync"));

    // Encode the entire map as a single JSON string — handles all value types
    // (String, Number, Boolean, nested Map) without per-type dispatch.
    public static final StreamCodec<FriendlyByteBuf, ConfigSync> STREAM_CODEC =
            StreamCodec.of(ConfigSync::encode, ConfigSync::decode);

    private static void encode(FriendlyByteBuf buf, ConfigSync payload) {
        buf.writeUtf(GSON.toJson(payload.values), Short.MAX_VALUE);
    }

    private static ConfigSync decode(FriendlyByteBuf buf) {
        String json = buf.readUtf(Short.MAX_VALUE);
        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        return new ConfigSync(map != null ? map : new HashMap<>());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── Handler (runs on CLIENT main thread) ──────────────────────────────────
    public static void handle(ConfigSync payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ConfigServerSync.setClientShadow(payload.values()));
    }

    /**
     * Per-player factory — includes the server-computed {@code canEdit} result so the
     * client screen can reflect LuckPerms-aware permissions without a client-side PermissionAPI.
     */
    public static ConfigSync forPlayer(ServerPlayer player) {
        Map<String, Object> values = new HashMap<>(ConfigServerSync.toMap());
        values.put(CAN_EDIT_KEY, XpsPermissions.canEdit(player));
        // Embed the raw explicit entry so the client hint label can show the correct message
        Boolean explicit = qdeeme.xp_simplifier.permissions.XpsEditPermission.canEditExplicit(player.getGameProfile().getName());
        if (explicit != null) values.put(CAN_EDIT_EXPLICIT_KEY, explicit);
        return new ConfigSync(values);
    }
}