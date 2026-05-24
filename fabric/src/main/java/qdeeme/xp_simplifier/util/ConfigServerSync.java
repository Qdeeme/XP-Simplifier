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


package qdeeme.xp_simplifier.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qdeeme.xp_simplifier.network.ConfigSync;
import qdeeme.xp_simplifier.permissions.XpsEditPermission;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton lifecycle manager for the per-world config layer (Fabric port).
 *
 * <p>Server side: holds the world root path; delegates config data to {@link Config}.
 * On first world start a snapshot is written to {@code worldRoot/xp_simplifier/};
 * on subsequent starts that snapshot is loaded instead of the master config files.</p>
 *
 * <p>Client side: holds the last {@code Map<String,Object>} received via the S2C
 * {@code ConfigSync} packet so the config screen can read current values without
 * a server reference.</p>
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
     * Client-side: server-computed result of {@code XpsPermissions.canEdit(player)}.
     * Defaults to {@code true} so the screen is fully editable in singleplayer.
     */
    private static boolean clientCanEdit = true;

    /**
     * Client-side mirror of the explicit file entry for this player.
     * {@code null} = not in file; {@code true} = explicitly granted; {@code false} = explicitly denied.
     */
    private static Boolean clientExplicit = null;

    // ── Server lifecycle ──────────────────────────────────────────────────────

    /**
     * Called from {@code ServerLifecycleEvents.SERVER_STARTED}.
     */
    public static void initServer(Path serverWorldRoot) {
        worldRoot = serverWorldRoot;
        Path snapshotDir = serverWorldRoot.resolve("xp_simplifier");
        if (Files.exists(snapshotDir)) {
            Config.loadFromWorldDir(serverWorldRoot);
            LOGGER.info("Loaded world-specific config from {}", snapshotDir);
        } else {
            Config.saveToWorldDir(serverWorldRoot);
            LOGGER.info("Created world config snapshot at {}", snapshotDir);
        }
        MerchantOffersData.load();
        XpsEditPermission.load(serverWorldRoot);
    }

    /**
     * Called from {@code ServerLifecycleEvents.SERVER_STOPPED}.
     */
    public static void clearServer() {
        worldRoot = null;
        clientShadow = null;
        clientCanEdit = true;
        clientExplicit = null;
        XpsEditPermission.clear();
        Config.load();
    }

    // ── Data delegation ───────────────────────────────────────────────────────

    public static Map<String, Object> toMap() {
        return Config.toMapAll();
    }

    public static void save() {
        if (worldRoot != null) Config.saveToWorldDir(worldRoot);
    }

    public static Path getWorldDir() {
        return worldRoot;
    }

    // ── Client shadow ─────────────────────────────────────────────────────────

    /**
     * Called by the {@code ConfigSync} packet handler on the client.
     * Strips the {@code _xps_can_edit} sentinel key before storing the shadow.
     */
    public static void setClientShadow(Map<String, Object> shadow) {
        Object flag = shadow.remove(ConfigSync.CAN_EDIT_KEY);
        clientCanEdit = flag instanceof Boolean b ? b : true;
        Object explicitRaw = shadow.remove(ConfigSync.CAN_EDIT_EXPLICIT_KEY);
        clientExplicit = explicitRaw instanceof Boolean b ? b : null;
        Config.fromMapAll(shadow);
        clientShadow = shadow;
    }

    public static boolean clientCanEdit() {
        return clientCanEdit;
    }

    public static Boolean clientExplicit() {
        return clientExplicit;
    }

    /**
     * Returns the last config snapshot received from the server, or the master
     * config (for main-menu / offline editing).
     */
    public static Map<String, Object> getClientShadow() {
        return clientShadow != null ? clientShadow : Config.toMapAll();
    }

    /**
     * Deep-merges a {@code ConfigDelta} payload into the client shadow.
     */
    @SuppressWarnings("unchecked")
    public static void applyClientDelta(Map<String, Object> delta) {
        if (clientShadow == null) return;
        Map<String, Object> flagsOnly = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : delta.entrySet()) {
            if (entry.getKey().equals("map_entries")) continue;
            clientShadow.put(entry.getKey(), entry.getValue());
            flagsOnly.put(entry.getKey(), entry.getValue());
        }
        if (!flagsOnly.isEmpty()) Config.applyFlagDelta(flagsOnly);
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
                        "XPS_WARN: ConfigDelta references unknown category '" + catName + "' in map '" + mapName + "'");
                for (Map.Entry<?, ?> e : ((Map<?, ?>) cEntry.getValue()).entrySet()) {
                    if (e.getValue() == null) shadowCat.remove((String) e.getKey());
                    else shadowCat.put((String) e.getKey(), e.getValue());
                }
            }
        }
    }
}

