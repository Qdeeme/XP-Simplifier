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


import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import qdeeme.xp_simplifier.util.CropXpMode;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static qdeeme.xp_simplifier.handler.SmeltingHandler.reloadRecipeXp;

// Main flags (orbMode, xpModes, anvilRepair, multipliers) are stored in xps_main_config.json.
// Hard-coded defaults are in XpsConfigInit; no NeoForge ModConfigSpec / TOML file is used.
// Loader-independent fields, getters, flag-I/O, ConfigData, and addDefaultCategories() live
// in the common superclass XpsConfigBase.

public class XpsConfig extends XpsConfigBase {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("Xp Simplifier");
    private static final Path BLOCKS_CONFIG_PATH = CONFIG_DIR.resolve("Blocks.json");
    private static final Path CROPS_CONFIG_PATH = CONFIG_DIR.resolve("Crops.json");
    private static final Path ENTITIES_CONFIG_PATH = CONFIG_DIR.resolve("Entities.json");
    private static final Path SMELTING_CONFIG_PATH = CONFIG_DIR.resolve("Smelting.json");
    private static final Path TRADING_CONFIG_PATH = CONFIG_DIR.resolve("Trading.json");
    private static final Path BREEDING_CONFIG_PATH = CONFIG_DIR.resolve("Breeding.json");
    private static final Path FISHING_CONFIG_PATH = CONFIG_DIR.resolve("Fishing.json");
    private static final Path GRINDSTONE_CONFIG_PATH = CONFIG_DIR.resolve("Grindstone.json");

    /**
     * Loads JSON map files from {@code CONFIG_DIR} and applies flag values from
     * {@code xps_main_config.json} (falling back to {@code XpsConfigInit} defaults).
     * Safe to call at any point — no NeoForge config lifecycle dependency.
     */
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
                data.initializeDefaults();
                needsSave = true;
            }

            buildIndexes();
            applyDefaults();
            loadFlagsFromDir(CONFIG_DIR);


            if (needsSave) saveInternal();
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
        wanderingTraderRawId = BuiltInRegistries.ENTITY_TYPE.getId(EntityType.WANDERING_TRADER);

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
                            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(key));
                            entityIndex.put(BuiltInRegistries.ENTITY_TYPE.getId(type), entry.getValue());
                        }
                    }
                }
            }
        }

        if (data.Blocks != null) {
            for (BlockCategory category : data.Blocks.values()) {
                if (category.blocks != null) {
                    for (Map.Entry<String, XpValue> entry : category.blocks.entrySet()) {
                        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(entry.getKey()));
                        blockIndex.put(BuiltInRegistries.BLOCK.getId(block), entry.getValue());
                    }
                }
            }
        }

        if (data.Crops != null) {
            for (CropCategory category : data.Crops.values()) {
                if (category.crops != null) {
                    for (Map.Entry<String, XpValue> entry : category.crops.entrySet()) {
                        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(entry.getKey()));
                        cropIndex.put(BuiltInRegistries.BLOCK.getId(block), entry.getValue());
                    }
                }
            }
        }

        if (data.Smelting != null) {
            for (SmeltingCategory category : data.Smelting.values()) {
                if (category.smelting != null) {
                    recipeIndex.putAll(category.smelting);
                }
            }
        }

        if (data.Trading != null) {
            for (TradingCategory category : data.Trading.values()) {
                if (category.trader != null) {
                    for (Map.Entry<String, TradingXpValues> entry : category.trader.entrySet()) {
                        String key = entry.getKey();
                        if (key.equals("minecraft:wandering_trader")) {
                            tradingIndex.put(wanderingTraderRawId, entry.getValue());
                        } else {
                            VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.get(ResourceLocation.tryParse(key));
                            tradingIndex.put(BuiltInRegistries.VILLAGER_PROFESSION.getId(profession), entry.getValue());
                        }
                    }
                }
            }
        }

        if (data.Breeding != null) {
            for (BreedingCategory category : data.Breeding.values()) {
                if (category.breeding != null) {
                    for (Map.Entry<String, XpValue> entry : category.breeding.entrySet()) {
                        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(entry.getKey()));
                        breedingIndex.put(BuiltInRegistries.ENTITY_TYPE.getId(type), entry.getValue());
                    }
                }
            }
        }

        if (data.Fishing != null) {
            for (FishingCategory category : data.Fishing.values()) {
                if (category.fishing != null) {
                    for (Map.Entry<String, XpValue> entry : category.fishing.entrySet()) {
                        String key = entry.getKey();
                        if (key.equals("Global_Fishing")) {
                            globalFishingXp = entry.getValue();
                        } else {
                            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(key));
                            fishingIndex.put(BuiltInRegistries.ITEM.getId(item), entry.getValue());
                        }
                    }
                }
            }
        }

        if (data.Grindstone != null) {
            for (GrindstoneCategory category : data.Grindstone.values()) {
                if (category.enchantments != null) {
                    for (Map.Entry<String, XpValue> entry : category.enchantments.entrySet()) {
                        ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                        if (id != null) {
                            grindstoneIndex.put(String.valueOf(id), entry.getValue());
                        } else {
                            LOGGER.warn("Unknown enchantment in Grindstone config: {}", entry.getKey());
                        }
                    }
                }
            }
        }
    }


    /**
     * Loads the 8 JSON category files from {@code worldRoot/xp_simplifier/} and
     * rebuilds the runtime indexes. Master config files in {@code config/Xp Simplifier/}
     * are never touched.
     */
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
            LOGGER.info("Loaded world config from {}", dir);
        } catch (Exception e) {
            LOGGER.error("Failed to load world config", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Writes the current in-memory {@code ConfigData} to {@code worldRoot/xp_simplifier/}.
     * Does NOT write to the master {@code config/Xp Simplifier/} directory.
     */
    public static void saveToWorldDir(Path worldRoot) {
        Path dir = worldRoot.resolve("xp_simplifier");
        lock.readLock().lock();
        try {
            Files.createDirectories(dir);
            saveCategoriesToDir(dir);
            saveFlagsToDir(dir);  // persist modes/multipliers alongside JSON maps
            reloadRecipeXp();
        } catch (IOException e) {
            LOGGER.error("Failed to save world config", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Serialises all runtime flags (main config) and the
     * full {@code ConfigData} JSON maps into a single {@code Map<String,Object>}.
     * Used by the network layer and by {@code ConfigServerSync.toMap()}.
     */
    public static Map<String, Object> toMapAll() {
        lock.readLock().lock();
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            // Enum flags
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
            // Multipliers
            m.put("entityXpMultiplier", entityXpMultiplierFlag);
            m.put("blockXpMultiplier", blockXpMultiplierFlag);
            m.put("cropXpMultiplier", cropXpMultiplierFlag);
            m.put("smeltingXpMultiplier", smeltingXpMultiplierFlag);
            m.put("tradingXpMultiplier", tradingXpMultiplierFlag);
            m.put("merchantXpMultiplier", merchantXpMultiplierFlag);
            m.put("breedingXpMultiplier", breedingXpMultiplierFlag);
            m.put("fishingXpMultiplier", fishingXpMultiplierFlag);
            m.put("grindstoneXpMultiplier", grindstoneXpMultiplierFlag);
            // Anvil repair
            m.put("xpRepairEnabled", xpRepairEnabledFlag);
            m.put("durabilityPerPoint", durabilityPerPointFlag);
            m.put("maxAnvilRepairCost", maxAnvilRepairCostFlag);
            m.put("mapViewEnabled", mapViewEnabledFlag);
            // JSON data maps — normalized: catName → {entryKey → rawValue} (no typed wrapper field)
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

    /**
     * Applies a map (received from the network or loaded from disk) into the live
     * in-memory state, then rebuilds the runtime indexes.
     * Does NOT persist anything to disk.
     */
    public static void fromMapAll(Map<String, Object> m) {
        lock.writeLock().lock();
        try {
            // Enum flags
            if (m.get("orbMode") instanceof String s) {
                try {
                    orbModeEnum = OrbMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("entityXpMode") instanceof String s) {
                try {
                    entityXpModeEnum = XpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("blockXpMode") instanceof String s) {
                try {
                    blockXpModeEnum = XpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("cropXpMode") instanceof String s) {
                try {
                    cropXpModeEnum = CropXpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("breedingXpMode") instanceof String s) {
                try {
                    breedingXpModeEnum = XpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("fishingXpMode") instanceof String s) {
                try {
                    fishingXpModeEnum = XpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("tradingPlayerXp") instanceof String s) {
                try {
                    tradingPlayerXpModeEnum = XpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("merchantXp") instanceof String s) {
                try {
                    merchantXpModeEnum = XpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("grindstoneXpMode") instanceof String s) {
                try {
                    grindstoneXpModeEnum = XpMode.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (m.get("smeltingXpMode") instanceof String s) {
                try {
                    smeltingXpModeEnum = XpMode.valueOf(s.toUpperCase());
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

    private static void saveInternal() {
        try {
            // Ensure config directory exists
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            // Save category files
            if (data.Blocks != null) {
                String blocksJson = GSON.toJson(data.Blocks);
                Files.writeString(BLOCKS_CONFIG_PATH, blocksJson);
            }
            if (data.Crops != null) {
                String cropsJson = GSON.toJson(data.Crops);
                Files.writeString(CROPS_CONFIG_PATH, cropsJson);
            }
            if (data.Entities != null) {
                String entitiesJson = GSON.toJson(data.Entities);
                Files.writeString(ENTITIES_CONFIG_PATH, entitiesJson);
            }
            if (data.Smelting != null) {
                String smeltingJson = GSON.toJson(data.Smelting);
                Files.writeString(SMELTING_CONFIG_PATH, smeltingJson);
            }
            if (data.Trading != null) {
                String tradingJson = GSON.toJson(data.Trading);
                Files.writeString(TRADING_CONFIG_PATH, tradingJson);
            }
            if (data.Breeding != null) {
                String breedingJson = GSON.toJson(data.Breeding);
                Files.writeString(BREEDING_CONFIG_PATH, breedingJson);
            }
            if (data.Fishing != null) {
                String fishingJson = GSON.toJson(data.Fishing);
                Files.writeString(FISHING_CONFIG_PATH, fishingJson);
            }
            if (data.Grindstone != null) {
                String grindstoneJson = GSON.toJson(data.Grindstone);
                Files.writeString(GRINDSTONE_CONFIG_PATH, grindstoneJson);
            }

            LOGGER.debug("Saved all maps to main");
        } catch (IOException e) {
            LOGGER.error("Failed to save maps to main", e);
        }
    }

    // ── Per-entry delta application ───────────────────────────────────────────

    /**
     * Writes exactly one JSON map file from the live {@code data.*} state.
     * {@code dir} must be the directory to write into (absolute path).
     */
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
                if (mapName.equals("Smelting")) reloadRecipeXp();  // ensure smelting changes take effect immediately
                LOGGER.debug("Saved {}.json", mapName);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save {}.json", mapName, e);
        }
    }

    /**
     * Applies a nested entry delta and writes only the touched JSON files to
     * {@code worldRoot/xp_simplifier/}.
     *
     * @param delta     {@code mapName → catName → entryKey → rawValue} (null = delete)
     * @param worldRoot world root; files go to {@code worldRoot/xp_simplifier/}
     */
    public static void applyEntryDelta(Map<String, ?> delta, Path worldRoot) {
        applyEntryDeltaToDir(delta, worldRoot.resolve("xp_simplifier"));
    }

    /**
     * Same as {@link #applyEntryDelta(Map, Path)} but writes to the master
     * {@code config/Xp Simplifier/} directory.  Used when no world is loaded
     * (main-menu editing) so changes become global defaults.
     */
    public static void applyEntryDelta(Map<String, ?> delta) {
        applyEntryDeltaToDir(delta, CONFIG_DIR);
    }

    /**
     * Internal: applies delta to in-memory state and saves dirty files to {@code dir}.
     */
    private static void applyEntryDeltaToDir(Map<String, ?> delta, Path dir) {
        lock.writeLock().lock();
        boolean anyApplied = false;
        Set<String> dirtyMaps = new LinkedHashSet<>();
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

    // ── G2b: applyFlagDelta (flag-only, doesn't touch maps/indexes) ─────────

    public static void applyFlagDelta(Map<String, Object> m) {
        lock.writeLock().lock();
        try {
            loadFlagsFromMap(m);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Saves the current in-memory state (maps + flags) to the master
     * {@code config/Xp Simplifier/} directory.  Used when applying changes
     * from the config screen while no world is loaded (main-menu / global editing).
     */
    public static void saveToMainConfig() {
        lock.writeLock().lock();
        try {
            saveCategoriesToDir(CONFIG_DIR);
            saveFlagsToDir(CONFIG_DIR);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── Typed per-entry mutators (called inside write-lock) ───────────────────

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
                ResourceLocation rl = ResourceLocation.tryParse(entryKey);
                if (rl != null)
                    entityIndex.remove(BuiltInRegistries.ENTITY_TYPE.getId(BuiltInRegistries.ENTITY_TYPE.get(rl)));
            }
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.entities.put(entryKey, xv);
            if (entryKey.equals("minecraft:ender_dragon_first")) dragonFirstXp = xv;
            else if (entryKey.equals("minecraft:ender_dragon_respawned")) dragonRespawnedXp = xv;
            else {
                ResourceLocation rl = ResourceLocation.tryParse(entryKey);
                if (rl != null)
                    entityIndex.put(BuiltInRegistries.ENTITY_TYPE.getId(BuiltInRegistries.ENTITY_TYPE.get(rl)), xv);
            }
        }
    }

    private static void applyBlockEntry(String catName, String entryKey, Object rawValue) {
        BlockCategory cat = data.Blocks.get(catName);
        if (cat == null) {
            LOGGER.warn("applyBlockEntry: unknown category '{}'", catName);
            return;
        }
        ResourceLocation rl = ResourceLocation.tryParse(entryKey);
        if (rl == null) return;
        int rawId = BuiltInRegistries.BLOCK.getId(BuiltInRegistries.BLOCK.get(rl));
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
        ResourceLocation rl = ResourceLocation.tryParse(entryKey);
        if (rl == null) return;
        int rawId = BuiltInRegistries.BLOCK.getId(BuiltInRegistries.BLOCK.get(rl));
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
                ResourceLocation rl = ResourceLocation.tryParse(entryKey);
                if (rl != null)
                    tradingIndex.remove(BuiltInRegistries.VILLAGER_PROFESSION.getId(BuiltInRegistries.VILLAGER_PROFESSION.get(rl)));
            }
        } else {
            TradingXpValues tv = TradingXpValues.fromRaw(rawValue);
            cat.trader.put(entryKey, tv);
            if (entryKey.equals("minecraft:wandering_trader")) tradingIndex.put(wanderingTraderRawId, tv);
            else {
                ResourceLocation rl = ResourceLocation.tryParse(entryKey);
                if (rl != null)
                    tradingIndex.put(BuiltInRegistries.VILLAGER_PROFESSION.getId(BuiltInRegistries.VILLAGER_PROFESSION.get(rl)), tv);
            }
        }
    }

    private static void applyBreedingEntry(String catName, String entryKey, Object rawValue) {
        BreedingCategory cat = data.Breeding.get(catName);
        if (cat == null) {
            LOGGER.warn("applyBreedingEntry: unknown category '{}'", catName);
            return;
        }
        ResourceLocation rl = ResourceLocation.tryParse(entryKey);
        if (rl == null) return;
        int rawId = BuiltInRegistries.ENTITY_TYPE.getId(BuiltInRegistries.ENTITY_TYPE.get(rl));
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
                ResourceLocation rl = ResourceLocation.tryParse(entryKey);
                if (rl != null) fishingIndex.remove(BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(rl)));
            }
        } else {
            XpValue xv = XpValue.fromRaw(rawValue);
            cat.fishing.put(entryKey, xv);
            if (entryKey.equals("Global_Fishing")) globalFishingXp = xv;
            else {
                ResourceLocation rl = ResourceLocation.tryParse(entryKey);
                if (rl != null) fishingIndex.put(BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(rl)), xv);
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

    // Category inner classes are in common (qdeeme.xp_simplifier.util.config):
    // XpValue, TradingXpValues, BlockCategory, CropCategory, EntitiesCategory,
    // SmeltingCategory, TradingCategory, BreedingCategory, FishingCategory, GrindstoneCategory.
    // ConfigData (maps-only, no flags) and all getters/setters live in XpsConfigBase (common).
}
