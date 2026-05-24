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


package qdeeme.xp_simplifier.permissions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-world edit-permission registry backed by
 * {@code worldRoot/xp_simplifier/xps_edit_permissions.json}.
 *
 * <p>Priority (evaluated in ConfigUpdate):</p>
 * <ol>
 *   <li>File entry {@code false} → deny (overrides even op level 4)</li>
 *   <li>File entry {@code true}  → allow</li>
 * </ol>
 */
public final class XpsEditPermission {

    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/edit-permission");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "xps_edit_permissions.json";

    private static final Map<String, Boolean> entries = new LinkedHashMap<>();

    private XpsEditPermission() {
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public static void load(Path worldRoot) {
        Path file = worldRoot.resolve("xp_simplifier").resolve(FILE_NAME);
        entries.clear();
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            Map<String, Boolean> raw = GSON.fromJson(json, new TypeToken<Map<String, Boolean>>() {
            }.getType());
            if (raw != null) {
                entries.putAll(raw);
            }
            LOGGER.info("Loaded {} edit-permission entries from {}", entries.size(), file);
        } catch (IOException e) {
            LOGGER.error("Failed to read {}: {}", file, e.getMessage());
        }
    }

    public static void save(Path worldRoot) {
        Path dir = worldRoot.resolve("xp_simplifier");
        Path file = dir.resolve(FILE_NAME);
        try {
            Files.createDirectories(dir);
            Files.writeString(file, GSON.toJson(entries));
        } catch (IOException e) {
            LOGGER.error("Failed to write {}: {}", file, e.getMessage());
        }
    }

    public static void clear() {
        entries.clear();
    }

    public static Map<String, Boolean> getEntries() {
        return entries;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    /**
     * Returns the explicit permission entry for this player name.
     * {@code null} means no entry — caller falls back to op level.
     */
    public static Boolean canEditExplicit(String playerName) {
        return entries.get(playerName);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public static void set(String playerName, boolean canEdit, Path worldRoot) {
        entries.put(playerName, canEdit);
        save(worldRoot);
    }

    public static boolean remove(String playerName, Path worldRoot) {
        boolean had = entries.remove(playerName) != null;
        save(worldRoot);
        return had;
    }
}

