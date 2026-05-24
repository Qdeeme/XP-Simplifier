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



package qdeeme.xp_simplifier.util.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qdeeme.xp_simplifier.network.ConfigSync;
import qdeeme.xp_simplifier.permissions.XpsEditPermission;
import qdeeme.xp_simplifier.util.MerchantOffersData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton lifecycle manager for the per-world config layer.
 *
 * <p>Server side: holds the world root path; delegates all config data to
 * {@link XpsConfig}. On first world start a snapshot is written to
 * {@code worldRoot/xp_simplifier/}; on subsequent starts that snapshot is
 * loaded instead of the master {@code config/Xp Simplifier/} files.</p>
 *
 * <p>Client side: holds the last {@code Map<String,Object>} received via the
 * S2C {@code ConfigSync} packet so the ConfigScreen can read current values
 * without a server reference.</p>
 */
public class ConfigServerSync {

    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/config-sync");

    /**
     * World root path (non-null while logical server is running).
     */
    private static Path worldRoot = null;

    /**
     * Client-side shadow: last config snapshot pushed from the server.
     */
    private static Map<String, Object> clientShadow = null;

    /**
     * Client-side: server-computed result of {@code XpsPermissions.canEdit(player)},
     * embedded in the {@code ConfigSync} packet. Defaults to {@code true} so the screen
     * is fully editable in singleplayer / main-menu (no server = no restrictions).
     */
    private static boolean clientCanEdit = true;

    /**
     * Client-side mirror of the explicit file entry for this player.
     * {@code null} = not in file; {@code true} = explicitly granted; {@code false} = explicitly denied.
     */
    private static Boolean clientExplicit = false;

    // ── Server lifecycle ──────────────────────────────────────────────────────

    /**
     * Called from {@code ServerStartingEvent}.
     *
     * @param serverWorldRoot the world root path
     *                        (e.g. {@code event.getServer().getWorldPath(LevelResource.ROOT)})
     */
    public static void initServer(Path serverWorldRoot) {
        worldRoot = serverWorldRoot;
        Path snapshotDir = serverWorldRoot.resolve("xp_simplifier");
        if (Files.exists(snapshotDir)) {
            XpsConfig.loadFromWorldDir(serverWorldRoot);
            LOGGER.info("Loaded world-specific config from {}", snapshotDir);
        } else {
            XpsConfig.saveToWorldDir(serverWorldRoot);
            LOGGER.info("Created world config snapshot at {}", snapshotDir);
        }
        MerchantOffersData.load();
        XpsEditPermission.load(serverWorldRoot);
    }

    /**
     * Called from {@code ServerStoppedEvent}. Reloads master config so the main menu reflects defaults.
     */
    public static void clearServer() {
        worldRoot = null;
        clientShadow = null;
        clientCanEdit = true;
        clientExplicit = true;
        XpsEditPermission.clear();
        XpsConfig.load(); // revert in-memory state to CONFIG_DIR (world overrides are gone)
    }

    // ── Data delegation ───────────────────────────────────────────────────────

    /**
     * Returns the full config as a {@code Map<String,Object>} suitable for
     * network transport. Delegates to {@link XpsConfig#toMapAll()}.
     */
    public static Map<String, Object> toMap() {
        return XpsConfig.toMapAll();
    }

    /**
     * Persists the current config state to the world-specific directory.
     * Never touches the master {@code config/Xp Simplifier/} files.
     */
    public static void save() {
        if (worldRoot != null) {
            XpsConfig.saveToWorldDir(worldRoot);
        }
    }

    /**
     * Returns the world root path currently in use on the server side.
     * Null if no server is running (singleplayer or between sessions).
     */
    public static Path getWorldDir() {
        return worldRoot;
    }

    // ── Client shadow ─────────────────────────────────────────────────────────

    /**
     * Called by the {@code ConfigSync} packet handler on the client after
     * receiving the S2C sync packet. Strips the {@code _xps_can_edit} sentinel
     * key before storing the shadow to keep config data clean.
     */
    public static void setClientShadow(Map<String, Object> shadow) {
        Object flag = shadow.remove(ConfigSync.CAN_EDIT_KEY);
        clientCanEdit = flag instanceof Boolean b ? b : true;
        Object explicitRaw = shadow.remove(ConfigSync.CAN_EDIT_EXPLICIT_KEY);
        clientExplicit = explicitRaw instanceof Boolean b ? b : null;
        // Apply config values (including mapViewEnabled) to in-memory XpsConfig state
        // so that XpsConfig.isMapViewEnabled() etc. reflect the server's values on the client.
        XpsConfig.fromMapAll(shadow);
        clientShadow = shadow;
    }

    /**
     * Returns the server-computed edit permission for this client session.
     * {@code true} when no server is active (singleplayer / main menu).
     */
    public static boolean clientCanEdit() {
        return clientCanEdit;
    }

    /**
     * Returns the explicit file entry for this client player as last synced by the server.
     * {@code null} = not in file; {@code true} = explicitly granted; {@code false} = explicitly denied.
     */
    public static Boolean clientExplicit() {
        if (clientExplicit == null) {
            return false;
        }
        return clientExplicit;
    }

    /**
     * Returns the last config snapshot received from the server.
     * Falls back to the master config files (loaded at mod init) when no server
     * session is active — enables main-menu / offline editing.
     */
    public static Map<String, Object> getClientShadow() {
        return clientShadow != null ? clientShadow : XpsConfig.toMapAll();
    }

    /**
     * Deep-merges a {@code ConfigDelta} payload into the client shadow.
     * Uses the normalized schema ({@code mapName → catName → entryKey → rawValue}).
     * Asserts that every referenced map and category already exists in the shadow —
     * absence is a server-side bug, not a graceful condition.
     *
     * <p>Called on the client main thread via {@code ctx.enqueueWork()}.</p>
     */
    @SuppressWarnings("unchecked")
    public static void applyClientDelta(Map<String, Object> delta) {
        if (clientShadow == null) return; // no snapshot yet — ignore (should not happen)
        // Collect non-map_entries flag changes (e.g. mapViewEnabled) to apply to XpsConfig too
        java.util.Map<String, Object> flagsOnly = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : delta.entrySet()) {
            if (entry.getKey().equals("map_entries")) continue;
            clientShadow.put(entry.getKey(), entry.getValue()); // flag / mode / multiplier
            flagsOnly.put(entry.getKey(), entry.getValue());
        }
        // Keep XpsConfig in-memory state in sync so isMapViewEnabled() etc. reflect server values
        if (!flagsOnly.isEmpty()) XpsConfig.applyFlagDelta(flagsOnly);
        Object meRaw = delta.get("map_entries");
        if (!(meRaw instanceof Map<?, ?> mapEntries)) return;
        for (Map.Entry<?, ?> mEntry : mapEntries.entrySet()) {
            String mapName = (String) mEntry.getKey();
            Map<?, ?> shadowMap = (Map<?, ?>) clientShadow.get(mapName);
            Objects.requireNonNull(shadowMap,
                    "XPS_WARN: ConfigDelta references unknown map '" + mapName + "'");
            for (Map.Entry<?, ?> cEntry : ((Map<?, ?>) mEntry.getValue()).entrySet()) {
                String catName = (String) cEntry.getKey();
                Map<String, Object> shadowCat = (Map<String, Object>) shadowMap.get(catName);
                Objects.requireNonNull(shadowCat,
                        "XPS_Warn: ConfigDelta references unknown category '" + catName + "' in map '" + mapName + "'");
                for (Map.Entry<?, ?> e : ((Map<?, ?>) cEntry.getValue()).entrySet()) {
                    if (e.getValue() == null) shadowCat.remove((String) e.getKey());
                    else shadowCat.put((String) e.getKey(), e.getValue());
                }
            }
        }
    }
}