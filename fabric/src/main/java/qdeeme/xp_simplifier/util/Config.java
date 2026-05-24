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

import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

import net.fabricmc.loader.api.FabricLoader;

import qdeeme.xp_simplifier.util.config.BlockCategory;
import qdeeme.xp_simplifier.util.config.BreedingCategory;
import qdeeme.xp_simplifier.util.config.CropCategory;
import qdeeme.xp_simplifier.util.config.EntitiesCategory;
import qdeeme.xp_simplifier.util.config.FishingCategory;
import qdeeme.xp_simplifier.util.config.GrindstoneCategory;
import qdeeme.xp_simplifier.util.config.SmeltingCategory;
import qdeeme.xp_simplifier.util.config.TradingCategory;
import qdeeme.xp_simplifier.util.config.TradingXpValues;
import qdeeme.xp_simplifier.util.config.XpValue;
import qdeeme.xp_simplifier.util.config.XpsConfigBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static qdeeme.xp_simplifier.handler.SmeltingHandler.reloadRecipeXp;

public class Config extends XpsConfigBase {

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("Xp Simplifier");
    private static final Path BLOCKS_CONFIG_PATH = CONFIG_DIR.resolve("Blocks.json");
    private static final Path CROPS_CONFIG_PATH = CONFIG_DIR.resolve("Crops.json");
    private static final Path ENTITIES_CONFIG_PATH = CONFIG_DIR.resolve("Entities.json");
    private static final Path SMELTING_CONFIG_PATH = CONFIG_DIR.resolve("Smelting.json");
    private static final Path TRADING_CONFIG_PATH = CONFIG_DIR.resolve("Trading.json");
    private static final Path BREEDING_CONFIG_PATH = CONFIG_DIR.resolve("Breeding.json");
    private static final Path FISHING_CONFIG_PATH = CONFIG_DIR.resolve("Fishing.json");
    private static final Path GRINDSTONE_CONFIG_PATH = CONFIG_DIR.resolve("Grindstone.json");

    public static void load() {
        lock.writeLock().lock();
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
                LOGGER.info("Created config directory: {}", CONFIG_DIR);
            }

            boolean needsSave = false;
            data = new ConfigData();

            loadCategoryFile(BLOCKS_CONFIG_PATH, "Blocks");
            loadCategoryFile(CROPS_CONFIG_PATH, "Crops");
            loadCategoryFile(ENTITIES_CONFIG_PATH, "Entities");
            loadCategoryFile(SMELTING_CONFIG_PATH, "Smelting");
            loadCategoryFile(TRADING_CONFIG_PATH, "Trading");
            loadCategoryFile(BREEDING_CONFIG_PATH, "Breeding");
            loadCategoryFile(FISHING_CONFIG_PATH, "Fishing");
            loadCategoryFile(GRINDSTONE_CONFIG_PATH, "Grindstone");

            if ((data.Blocks == null || data.Blocks.isEmpty()) ||
                    (data.Crops == null || data.Crops.isEmpty()) ||
                    (data.Entities == null || data.Entities.isEmpty()) ||
                    (data.Smelting == null || data.Smelting.isEmpty()) ||
                    (data.Trading == null || data.Trading.isEmpty()) ||
                    (data.Breeding == null || data.Breeding.isEmpty()) ||
                    (data.Fishing == null || data.Fishing.isEmpty()) ||
                    (data.Grindstone == null || data.Grindstone.isEmpty())) {
                LOGGER.info("One or more config categories empty, filling defaults");
                LOGGER.info("Default of:{}", data.toString());
                data.initializeDefaults();
                needsSave = true;
            }

            buildIndexes();
            applyDefaults();
            loadFlagsFromDir(CONFIG_DIR);

            if (needsSave) {
                saveCategoriesToDir(CONFIG_DIR);
                saveFlagsToDir(CONFIG_DIR);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
            data = new ConfigData();
            data.initializeDefaults();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void buildIndexes() {
        entityIndex.clear();
        blockIndex.clear();
        cropIndex.clear();
        recipeIndex.clear();
        tradingIndex.clear();
        breedingIndex.clear();
        fishingIndex.clear();
        grindstoneIndex.clear();
        dragonFirstXp = null;
        dragonRespawnedXp = null;
        globalFishingXp = null;
        wanderingTraderRawId = Registries.ENTITY_TYPE.getRawId(EntityType.WANDERING_TRADER);

        if (data.Entities != null) {
            for (EntitiesCategory category : data.Entities.values()) {
                if (category.entities != null) {
                    for (Map.Entry<String, XpValue> entry : category.entities.entrySet()) {
                        String key = entry.getKey();
                        if (key.equals("minecraft:ender_dragon_first")) {
                            dragonFirstXp = entry.getValue();
                        } else if (key.equals("minecraft:ender_dragon_respawned")) {
                            dragonRespawnedXp = entry.getValue();
                        } else {
                            EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.tryParse(key));
                            if (type != null) entityIndex.put(Registries.ENTITY_TYPE.getRawId(type), entry.getValue());
                            else LOGGER.warn("Unknown entity in Entities config: {}", key);
                        }
                    }
                }
            }
        }
        if (data.Blocks != null) {
            for (BlockCategory category : data.Blocks.values()) {
                if (category.blocks != null) {
                    for (Map.Entry<String, XpValue> entry : category.blocks.entrySet()) {
                        Block block = Registries.BLOCK.get(Identifier.tryParse(entry.getKey()));
                        if (block != null) blockIndex.put(Registries.BLOCK.getRawId(block), entry.getValue());
                        else LOGGER.warn("Unknown block in Blocks config: {}", entry.getKey());
                    }
                }
            }
        }
        if (data.Crops != null) {
            for (CropCategory category : data.Crops.values()) {
                if (category.crops != null) {
                    for (Map.Entry<String, XpValue> entry : category.crops.entrySet()) {
                        Block block = Registries.BLOCK.get(Identifier.tryParse(entry.getKey()));
                        if (block != null) cropIndex.put(Registries.BLOCK.getRawId(block), entry.getValue());
                        else LOGGER.warn("Unknown block in Crops config: {}", entry.getKey());
                    }
                }
            }
        }
        if (data.Smelting != null) {
            for (SmeltingCategory category : data.Smelting.values())
                if (category.smelting != null) recipeIndex.putAll(category.smelting);
        }
        if (data.Trading != null) {
            for (TradingCategory category : data.Trading.values()) {
                if (category.trader != null) {
                    for (Map.Entry<String, TradingXpValues> entry : category.trader.entrySet()) {
                        String key = entry.getKey();
                        if (key.equals("minecraft:wandering_trader")) {
                            tradingIndex.put(wanderingTraderRawId, entry.getValue());
                        } else {
                            VillagerProfession profession = Registries.VILLAGER_PROFESSION.get(Identifier.tryParse(key));
                            if (profession != null)
                                tradingIndex.put(Registries.VILLAGER_PROFESSION.getRawId(profession), entry.getValue());
                            else LOGGER.warn("Unknown villager profession in Trading config: {}", key);
                        }
                    }
                }
            }
        }
        if (data.Breeding != null) {
            for (BreedingCategory category : data.Breeding.values()) {
                if (category.breeding != null) {
                    for (Map.Entry<String, XpValue> entry : category.breeding.entrySet()) {
                        EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.tryParse(entry.getKey()));
                        if (type != null) breedingIndex.put(Registries.ENTITY_TYPE.getRawId(type), entry.getValue());
                        else LOGGER.warn("Unknown entity in Breeding config: {}", entry.getKey());
                    }
                }
            }
        }
        if (data.Fishing != null) {
            for (FishingCategory category : data.Fishing.values()) {
                if (category.fishing != null) {
                    for (Map.Entry<String, XpValue> entry : category.fishing.entrySet()) {
                        String key = entry.getKey();
                        if (key.equals("Global_Fishing")) globalFishingXp = entry.getValue();
                        else {
                            Item item = Registries.ITEM.get(Identifier.tryParse(key));
                            if (item != null) fishingIndex.put(Registries.ITEM.getRawId(item), entry.getValue());
                            else LOGGER.warn("Unknown item in Fishing config: {}", key);
                        }
                    }
                }
            }
        }
        if (data.Grindstone != null) {
            for (GrindstoneCategory category : data.Grindstone.values()) {
                if (category.enchantments != null) {
                    for (Map.Entry<String, XpValue> entry : category.enchantments.entrySet()) {
                        if (entry.getKey() != null) grindstoneIndex.put(entry.getKey(), entry.getValue());
                        else LOGGER.warn("Null enchantment key in Grindstone config");
                    }
                }
            }
        }
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    public static void saveToMainConfig() {
        lock.writeLock().lock();
        try {
            saveFlagsToDir(CONFIG_DIR);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── G3: toMapAll ─────────────────────────────────────────────────────────

    public static Map<String, Object> toMapAll() {
        lock.readLock().lock();
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orbMode", orbModeEnum != null ? orbModeEnum.name() : OrbMode.VANILLA.name());
            m.put("entityXpMode", entityXpModeEnum != null ? entityXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("blockXpMode", blockXpModeEnum != null ? blockXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("cropXpMode", cropXpModeEnum != null ? cropXpModeEnum.name() : CropXpMode.OFF.name());
            m.put("breedingXpMode", breedingXpModeEnum != null ? breedingXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("fishingXpMode", fishingXpModeEnum != null ? fishingXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("tradingPlayerXp", tradingPlayerXpModeEnum != null ? tradingPlayerXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("merchantXp", merchantXpModeEnum != null ? merchantXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("grindstoneXpMode", grindstoneXpModeEnum != null ? grindstoneXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("smeltingXpMode", smeltingXpModeEnum != null ? smeltingXpModeEnum.name() : XpMode.VANILLA.name());
            m.put("entityXpMultiplier", entityXpMultiplierFlag);
            m.put("blockXpMultiplier", blockXpMultiplierFlag);
            m.put("cropXpMultiplier", cropXpMultiplierFlag);
            m.put("smeltingXpMultiplier", smeltingXpMultiplierFlag);
            m.put("tradingXpMultiplier", tradingXpMultiplierFlag);
            m.put("merchantXpMultiplier", merchantXpMultiplierFlag);
            m.put("breedingXpMultiplier", breedingXpMultiplierFlag);
            m.put("fishingXpMultiplier", fishingXpMultiplierFlag);
            m.put("grindstoneXpMultiplier", grindstoneXpMultiplierFlag);
            m.put("xpRepairEnabled", xpRepairEnabledFlag);
            m.put("durabilityPerPoint", durabilityPerPointFlag);
            m.put("maxAnvilRepairCost", maxAnvilRepairCostFlag);
            m.put("mapViewEnabled", mapViewEnabledFlag);

            Map<String, Object> blks = new LinkedHashMap<>();
            if (data.Blocks != null) for (Map.Entry<String, BlockCategory> e : data.Blocks.entrySet())
                blks.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().blocks), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Blocks", blks);

            Map<String, Object> crps = new LinkedHashMap<>();
            if (data.Crops != null) for (Map.Entry<String, CropCategory> e : data.Crops.entrySet())
                crps.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().crops), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Crops", crps);

            Map<String, Object> ents = new LinkedHashMap<>();
            if (data.Entities != null) for (Map.Entry<String, EntitiesCategory> e : data.Entities.entrySet())
                ents.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().entities), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Entities", ents);

            Map<String, Object> smlt = new LinkedHashMap<>();
            if (data.Smelting != null) for (Map.Entry<String, SmeltingCategory> e : data.Smelting.entrySet())
                smlt.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().smelting), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Smelting", smlt);

            Map<String, Object> trd = new LinkedHashMap<>();
            if (data.Trading != null) for (Map.Entry<String, TradingCategory> e : data.Trading.entrySet())
                trd.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().trader), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Trading", trd);

            Map<String, Object> brd = new LinkedHashMap<>();
            if (data.Breeding != null) for (Map.Entry<String, BreedingCategory> e : data.Breeding.entrySet())
                brd.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().breeding), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Breeding", brd);

            Map<String, Object> fsh = new LinkedHashMap<>();
            if (data.Fishing != null) for (Map.Entry<String, FishingCategory> e : data.Fishing.entrySet())
                fsh.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().fishing), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Fishing", fsh);

            Map<String, Object> grd = new LinkedHashMap<>();
            if (data.Grindstone != null) for (Map.Entry<String, GrindstoneCategory> e : data.Grindstone.entrySet())
                grd.put(e.getKey(), GSON.fromJson(GSON.toJson(e.getValue().enchantments), new TypeToken<Map<String, Object>>() {
                }.getType()));
            m.put("Grindstone", grd);
            return m;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ── G2: fromMapAll ───────────────────────────────────────────────────────

    public static void fromMapAll(Map<String, Object> m) {
        lock.writeLock().lock();
        try {
            // Enum flags
            if (m.get("orbMode") instanceof String s) {
                try {
                    orbModeEnum = OrbMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("entityXpMode") instanceof String s) {
                try {
                    entityXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("blockXpMode") instanceof String s) {
                try {
                    blockXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("cropXpMode") instanceof String s) {
                try {
                    cropXpModeEnum = CropXpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("breedingXpMode") instanceof String s) {
                try {
                    breedingXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("fishingXpMode") instanceof String s) {
                try {
                    fishingXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("tradingPlayerXp") instanceof String s) {
                try {
                    tradingPlayerXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("merchantXp") instanceof String s) {
                try {
                    merchantXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("grindstoneXpMode") instanceof String s) {
                try {
                    grindstoneXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("smeltingXpMode") instanceof String s) {
                try {
                    smeltingXpModeEnum = XpMode.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            // Multipliers
            if (m.get("entityXpMultiplier") instanceof Number n) entityXpMultiplierFlag = n.floatValue();
            if (m.get("blockXpMultiplier") instanceof Number n) blockXpMultiplierFlag = n.floatValue();
            if (m.get("cropXpMultiplier") instanceof Number n) cropXpMultiplierFlag = n.floatValue();
            if (m.get("smeltingXpMultiplier") instanceof Number n) smeltingXpMultiplierFlag = n.floatValue();
            if (m.get("tradingXpMultiplier") instanceof Number n) tradingXpMultiplierFlag = n.floatValue();
            if (m.get("merchantXpMultiplier") instanceof Number n) merchantXpMultiplierFlag = n.floatValue();
            if (m.get("breedingXpMultiplier") instanceof Number n) breedingXpMultiplierFlag = n.floatValue();
            if (m.get("fishingXpMultiplier") instanceof Number n) fishingXpMultiplierFlag = n.floatValue();
            if (m.get("grindstoneXpMultiplier") instanceof Number n) grindstoneXpMultiplierFlag = n.floatValue();
            // Anvil repair
            if (m.get("xpRepairEnabled") instanceof Boolean b) xpRepairEnabledFlag = b;
            if (m.get("durabilityPerPoint") instanceof Number n) durabilityPerPointFlag = n.floatValue();
            if (m.get("maxAnvilRepairCost") instanceof Number n) maxAnvilRepairCostFlag = n.intValue();
            if (m.get("mapViewEnabled") instanceof Boolean b) mapViewEnabledFlag = b;


            // JSON data maps — round-trip back to typed structures
            if (m.containsKey("Blocks") && m.get("Blocks") instanceof Map<?, ?> flat) {
                data.Blocks = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    BlockCategory cat = new BlockCategory();
                    cat.blocks = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, XpValue>>() {
                    }.getType());
                    data.Blocks.put((String) e.getKey(), cat);
                }
            }
            // Crops
            if (m.containsKey("Crops") && m.get("Crops") instanceof Map<?, ?> flat) {
                data.Crops = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    CropCategory cat = new CropCategory();
                    cat.crops = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, XpValue>>() {
                    }.getType());
                    data.Crops.put((String) e.getKey(), cat);
                }
            }
            // Entities
            if (m.containsKey("Entities") && m.get("Entities") instanceof Map<?, ?> flat) {
                data.Entities = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    EntitiesCategory cat = new EntitiesCategory();
                    cat.entities = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, XpValue>>() {
                    }.getType());
                    data.Entities.put((String) e.getKey(), cat);
                }
            }
            // Smelting
            if (m.containsKey("Smelting") && m.get("Smelting") instanceof Map<?, ?> flat) {
                data.Smelting = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    SmeltingCategory cat = new SmeltingCategory();
                    cat.smelting = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, Float>>() {
                    }.getType());
                    data.Smelting.put((String) e.getKey(), cat);
                }
            }
            // Trading
            if (m.containsKey("Trading") && m.get("Trading") instanceof Map<?, ?> flat) {
                data.Trading = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    TradingCategory cat = new TradingCategory();
                    cat.trader = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, TradingXpValues>>() {
                    }.getType());
                    data.Trading.put((String) e.getKey(), cat);
                }
            }
            // Breeding
            if (m.containsKey("Breeding") && m.get("Breeding") instanceof Map<?, ?> flat) {
                data.Breeding = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    BreedingCategory cat = new BreedingCategory();
                    cat.breeding = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, XpValue>>() {
                    }.getType());
                    data.Breeding.put((String) e.getKey(), cat);
                }
            }
            // Fishing
            if (m.containsKey("Fishing") && m.get("Fishing") instanceof Map<?, ?> flat) {
                data.Fishing = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    FishingCategory cat = new FishingCategory();
                    cat.fishing = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, XpValue>>() {
                    }.getType());
                    data.Fishing.put((String) e.getKey(), cat);
                }
            }
            // Grindstone
            if (m.containsKey("Grindstone") && m.get("Grindstone") instanceof Map<?, ?> flat) {
                data.Grindstone = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : flat.entrySet()) {
                    GrindstoneCategory cat = new GrindstoneCategory();
                    cat.enchantments = GSON.fromJson(GSON.toJson(e.getValue()), new TypeToken<Map<String, XpValue>>() {
                    }.getType());
                    data.Grindstone.put((String) e.getKey(), cat);
                }
            }
            buildIndexes();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── G2b: applyFlagDelta (flag-only, doesn't touch maps/indexes) ─────────

    public static void applyFlagDelta(Map<String, Object> m) {
        lock.writeLock().lock();
        try {
            loadFlagsFromMap(m);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── G5: World-dir I/O ────────────────────────────────────────────────────

    public static void loadFromWorldDir(Path worldRoot) {
        Path dir = worldRoot.resolve("xp_simplifier");
        lock.writeLock().lock();
        try {
            data = new ConfigData();
            loadCategoryFile(dir.resolve("Blocks.json"), "Blocks");
            loadCategoryFile(dir.resolve("Crops.json"), "Crops");
            loadCategoryFile(dir.resolve("Entities.json"), "Entities");
            loadCategoryFile(dir.resolve("Smelting.json"), "Smelting");
            loadCategoryFile(dir.resolve("Trading.json"), "Trading");
            loadCategoryFile(dir.resolve("Breeding.json"), "Breeding");
            loadCategoryFile(dir.resolve("Fishing.json"), "Fishing");
            loadCategoryFile(dir.resolve("Grindstone.json"), "Grindstone");
            buildIndexes();
            applyDefaults();
            loadFlagsFromDir(dir);
            reloadRecipeXp();
            LOGGER.info("Loaded world config from {}", dir);
        } catch (Exception e) {
            LOGGER.error("Failed to load world config", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void saveToWorldDir(Path worldRoot) {
        Path dir = worldRoot.resolve("xp_simplifier");
        lock.readLock().lock();
        try {
            saveCategoriesToDir(dir);
            saveFlagsToDir(dir);
            reloadRecipeXp();
        } catch (Exception e) {
            LOGGER.error("Failed to save world config", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ── G4: applyEntryDelta ──────────────────────────────────────────────────

    public static void applyEntryDelta(Map<String, ?> delta, Path worldRoot) {
        applyEntryDeltaToDir(delta, worldRoot.resolve("xp_simplifier"));
    }

    public static void applyEntryDelta(Map<String, ?> delta) {
        applyEntryDeltaToDir(delta, CONFIG_DIR);
    }

    private static void applyEntryDeltaToDir(Map<String, ?> delta, Path dir) {
        lock.writeLock().lock();
        Set<String> dirtyMaps = new LinkedHashSet<>();
        boolean anyApplied = false;
        try {
            for (Map.Entry<String, ?> mapEntry : delta.entrySet()) {
                String mapName = mapEntry.getKey();
                if (!(mapEntry.getValue() instanceof Map<?, ?> catChanges)) continue;
                for (Map.Entry<?, ?> catEntry : catChanges.entrySet()) {
                    String catName = (String) catEntry.getKey();
                    if (!(catEntry.getValue() instanceof Map<?, ?> entries)) continue;
                    for (Map.Entry<?, ?> e : entries.entrySet()) {
                        String entryKey = (String) e.getKey();
                        Object rawValue = e.getValue();
                        switch (mapName) {
                            case "Entities" -> applyEntityEntry(catName, entryKey, rawValue);
                            case "Blocks" -> applyBlockEntry(catName, entryKey, rawValue);
                            case "Crops" -> applyCropEntry(catName, entryKey, rawValue);
                            case "Smelting" -> applySmeltingEntry(catName, entryKey, rawValue);
                            case "Trading" -> applyTradingEntry(catName, entryKey, rawValue);
                            case "Breeding" -> applyBreedingEntry(catName, entryKey, rawValue);
                            case "Fishing" -> applyFishingEntry(catName, entryKey, rawValue);
                            case "Grindstone" -> applyGrindstoneEntry(catName, entryKey, rawValue);
                        }
                        anyApplied = true;
                    }
                    dirtyMaps.add(mapName);
                }
            }
            if (anyApplied) {
                for (String mapName : dirtyMaps) {
                    saveMapFile(mapName, dir);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void saveMapFile(String mapName, Path dir) {
        try {
            Files.createDirectories(dir);
            String json = switch (mapName) {
                case "Blocks" -> GSON.toJson(data.Blocks);
                case "Crops" -> GSON.toJson(data.Crops);
                case "Entities" -> GSON.toJson(data.Entities);
                case "Smelting" -> GSON.toJson(data.Smelting);
                case "Trading" -> GSON.toJson(data.Trading);
                case "Breeding" -> GSON.toJson(data.Breeding);
                case "Fishing" -> GSON.toJson(data.Fishing);
                case "Grindstone" -> GSON.toJson(data.Grindstone);
                default -> null;
            };
            if (json != null) {
                Files.writeString(dir.resolve(mapName + ".json"), json);
                if (mapName.equals("Smelting")) reloadRecipeXp();
                LOGGER.debug("Saved {}.json", mapName);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save {}.json", mapName, e);
        }
    }

    // ── Per-entry helpers ────────────────────────────────────────────────────

    private static void applyEntityEntry(String catName, String entryKey, Object rawValue) {
        EntitiesCategory cat = data.Entities.get(catName);
        if (cat == null) {
            LOGGER.warn("applyEntityEntry: unknown category '{}'", catName);
            return;
        }
        if (rawValue == null) {
            cat.entities.remove(entryKey);
            if (entryKey.equals("minecraft:ender_dragon_first")) dragonFirstXp = null;
            else if (entryKey.equals("minecraft:ender_dragon_respawned")) dragonRespawnedXp = null;
            else {
                Identifier id = Identifier.tryParse(entryKey);
                if (id != null) {
                    EntityType<?> t = Registries.ENTITY_TYPE.get(id);
                    if (t != null) entityIndex.remove(Registries.ENTITY_TYPE.getRawId(t));
                }
            }
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.entities.put(entryKey, xv);
            if (entryKey.equals("minecraft:ender_dragon_first")) dragonFirstXp = xv;
            else if (entryKey.equals("minecraft:ender_dragon_respawned")) dragonRespawnedXp = xv;
            else {
                Identifier id = Identifier.tryParse(entryKey);
                if (id != null) {
                    EntityType<?> t = Registries.ENTITY_TYPE.get(id);
                    if (t != null) entityIndex.put(Registries.ENTITY_TYPE.getRawId(t), xv);
                }
            }
        }
    }

    private static void applyBlockEntry(String catName, String entryKey, Object rawValue) {
        BlockCategory cat = data.Blocks.get(catName);
        if (cat == null) {
            LOGGER.warn("applyBlockEntry: unknown category '{}'", catName);
            return;
        }
        Identifier id = Identifier.tryParse(entryKey);
        if (id == null) return;
        Block block = Registries.BLOCK.get(id);
        if (block == null) return;
        int rawId = Registries.BLOCK.getRawId(block);
        if (rawValue == null) {
            cat.blocks.remove(entryKey);
            blockIndex.remove(rawId);
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.blocks.put(entryKey, xv);
            blockIndex.put(rawId, xv);
        }
    }

    private static void applyCropEntry(String catName, String entryKey, Object rawValue) {
        CropCategory cat = data.Crops.get(catName);
        if (cat == null) {
            LOGGER.warn("applyCropEntry: unknown category '{}'", catName);
            return;
        }
        Identifier id = Identifier.tryParse(entryKey);
        if (id == null) return;
        Block block = Registries.BLOCK.get(id);
        if (block == null) return;
        int rawId = Registries.BLOCK.getRawId(block);
        if (rawValue == null) {
            cat.crops.remove(entryKey);
            cropIndex.remove(rawId);
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.crops.put(entryKey, xv);
            cropIndex.put(rawId, xv);
        }
    }

    private static void applySmeltingEntry(String catName, String entryKey, Object rawValue) {
        SmeltingCategory cat = data.Smelting.get(catName);
        if (cat == null) {
            LOGGER.warn("applySmeltingEntry: unknown category '{}'", catName);
            return;
        }
        if (rawValue == null) {
            cat.smelting.remove(entryKey);
            recipeIndex.remove(entryKey);
        } else {
            float xp = ((Number) rawValue).floatValue();
            cat.smelting.put(entryKey, xp);
            recipeIndex.put(entryKey, xp);
        }
    }

    private static void applyTradingEntry(String catName, String entryKey, Object rawValue) {
        TradingCategory cat = data.Trading.get(catName);
        if (cat == null) {
            LOGGER.warn("applyTradingEntry: unknown category '{}'", catName);
            return;
        }
        if (rawValue == null) {
            cat.trader.remove(entryKey);
            if (entryKey.equals("minecraft:wandering_trader")) tradingIndex.remove(wanderingTraderRawId);
            else {
                Identifier id = Identifier.tryParse(entryKey);
                if (id != null) {
                    VillagerProfession p = Registries.VILLAGER_PROFESSION.get(id);
                    if (p != null) tradingIndex.remove(Registries.VILLAGER_PROFESSION.getRawId(p));
                }
            }
        } else {
            TradingXpValues tv = TradingXpValues.fromRaw(rawValue);
            cat.trader.put(entryKey, tv);
            if (entryKey.equals("minecraft:wandering_trader")) tradingIndex.put(wanderingTraderRawId, tv);
            else {
                Identifier id = Identifier.tryParse(entryKey);
                if (id != null) {
                    VillagerProfession p = Registries.VILLAGER_PROFESSION.get(id);
                    if (p != null) tradingIndex.put(Registries.VILLAGER_PROFESSION.getRawId(p), tv);
                }
            }
        }
    }

    private static void applyBreedingEntry(String catName, String entryKey, Object rawValue) {
        BreedingCategory cat = data.Breeding.get(catName);
        if (cat == null) {
            LOGGER.warn("applyBreedingEntry: unknown category '{}'", catName);
            return;
        }
        Identifier id = Identifier.tryParse(entryKey);
        if (id == null) return;
        EntityType<?> type = Registries.ENTITY_TYPE.get(id);
        if (type == null) return;
        int rawId = Registries.ENTITY_TYPE.getRawId(type);
        if (rawValue == null) {
            cat.breeding.remove(entryKey);
            breedingIndex.remove(rawId);
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.breeding.put(entryKey, xv);
            breedingIndex.put(rawId, xv);
        }
    }

    private static void applyFishingEntry(String catName, String entryKey, Object rawValue) {
        FishingCategory cat = data.Fishing.get(catName);
        if (cat == null) {
            LOGGER.warn("applyFishingEntry: unknown category '{}'", catName);
            return;
        }
        if (rawValue == null) {
            cat.fishing.remove(entryKey);
            if (entryKey.equals("Global_Fishing")) globalFishingXp = null;
            else {
                Identifier id = Identifier.tryParse(entryKey);
                if (id != null) {
                    Item item = Registries.ITEM.get(id);
                    if (item != null) fishingIndex.remove(Registries.ITEM.getRawId(item));
                }
            }
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.fishing.put(entryKey, xv);
            if (entryKey.equals("Global_Fishing")) globalFishingXp = xv;
            else {
                Identifier id = Identifier.tryParse(entryKey);
                if (id != null) {
                    Item item = Registries.ITEM.get(id);
                    if (item != null) fishingIndex.put(Registries.ITEM.getRawId(item), xv);
                }
            }
        }
    }

    private static void applyGrindstoneEntry(String catName, String entryKey, Object rawValue) {
        GrindstoneCategory cat = data.Grindstone.get(catName);
        if (cat == null) {
            LOGGER.warn("applyGrindstoneEntry: unknown category '{}'", catName);
            return;
        }
        if (entryKey == null || entryKey.isEmpty()) {
            LOGGER.warn("applyGrindstoneEntry: invalid key '{}'", entryKey);
            return;
        }
        if (rawValue == null) {
            cat.enchantments.remove(entryKey);
            grindstoneIndex.remove(entryKey);
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.enchantments.put(entryKey, xv);
            grindstoneIndex.put(entryKey, xv);
        }
    }
}
