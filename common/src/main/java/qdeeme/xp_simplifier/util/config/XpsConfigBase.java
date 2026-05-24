package qdeeme.xp_simplifier.util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qdeeme.xp_simplifier.util.CropXpMode;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Loader-independent config core shared by every mod-loader build.
 * <p>
 * Contains:
 * <ul>
 *   <li>All static flag fields and runtime index maps.</li>
 *   <li>Flag I/O helpers: {@link #applyDefaults()}, {@link #loadFlagsFromMap},
 *       {@link #loadFlagsFromDir}, {@link #saveFlagsToDir}.</li>
 *   <li>Category file I/O: {@link #loadCategoryFile}, {@link #saveCategoriesToDir}.</li>
 *   <li>All public flag/mode/multiplier getters.</li>
 *   <li>All rawId-based XP lookup getters (no registry dependency).</li>
 *   <li>{@link ConfigData} with the full {@code addDefaultCategories()} builder.</li>
 * </ul>
 * <p>
 * Concrete loader classes ({@code XpsConfig} for NeoForge, {@code Config} for
 * Fabric) extend this class and supply:
 * <ul>
 *   <li>The loader-specific {@code CONFIG_DIR} path.</li>
 *   <li>A registry-backed {@code buildIndexes()} implementation.</li>
 *   <li>A {@code load()} entry point that calls the base helpers.</li>
 * </ul>
 * <p>
 * Note: {@code ResourceLocation} in this common module is remapped to
 * {@code Identifier} by the Fabric build toolchain automatically.
 */
public abstract class XpsConfigBase {

    protected static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/config");
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final ReadWriteLock lock = new ReentrantReadWriteLock();

    // ── Runtime data ──────────────────────────────────────────────────────────
    /**
     * Live category maps. Populated by {@code load()} / {@code buildIndexes()} in the subclass.
     */
    protected static ConfigData data = new ConfigData();

    // Index maps — keyed by registry raw-ID for O(1) event-time lookups
    protected static final Int2ObjectOpenHashMap<XpValue> entityIndex = new Int2ObjectOpenHashMap<>();
    protected static final Int2ObjectOpenHashMap<XpValue> blockIndex = new Int2ObjectOpenHashMap<>();
    protected static final Int2ObjectOpenHashMap<XpValue> cropIndex = new Int2ObjectOpenHashMap<>();
    protected static final Map<String, Float> recipeIndex = new LinkedHashMap<>();
    protected static final Int2ObjectOpenHashMap<TradingXpValues> tradingIndex = new Int2ObjectOpenHashMap<>();
    protected static final Int2ObjectOpenHashMap<XpValue> breedingIndex = new Int2ObjectOpenHashMap<>();
    protected static final Int2ObjectOpenHashMap<XpValue> fishingIndex = new Int2ObjectOpenHashMap<>();
    /**
     * Keyed by {@code "namespace:path"} string — avoids ResourceLocation/Identifier type split between loaders.
     */
    protected static final HashMap<String, XpValue> grindstoneIndex = new HashMap<>();

    protected static XpValue dragonFirstXp;
    protected static XpValue dragonRespawnedXp;
    protected static int wanderingTraderRawId = -1;
    protected static XpValue globalFishingXp;

    // ── Scalar / enum flag fields ─────────────────────────────────────────────
    protected static float durabilityPerPointFlag;
    protected static int maxAnvilRepairCostFlag;
    protected static boolean xpRepairEnabledFlag;

    protected static OrbMode orbModeEnum;
    protected static XpMode entityXpModeEnum;
    protected static XpMode blockXpModeEnum;
    protected static CropXpMode cropXpModeEnum;
    protected static XpMode breedingXpModeEnum;
    protected static XpMode fishingXpModeEnum;
    protected static XpMode tradingPlayerXpModeEnum;
    protected static XpMode merchantXpModeEnum;
    protected static XpMode grindstoneXpModeEnum;
    protected static XpMode smeltingXpModeEnum;

    // ── Per-system XP multipliers ─────────────────────────────────────────────
    protected static float entityXpMultiplierFlag = 1.0f;
    protected static float blockXpMultiplierFlag = 1.0f;
    protected static float cropXpMultiplierFlag = 1.0f;
    protected static float smeltingXpMultiplierFlag = 1.0f;
    protected static float tradingXpMultiplierFlag = 1.0f;
    protected static float merchantXpMultiplierFlag = 1.0f;
    protected static float breedingXpMultiplierFlag = 1.0f;
    protected static float fishingXpMultiplierFlag = 1.0f;
    protected static float grindstoneXpMultiplierFlag = 1.0f;

    /**
     * When {@code false}, maps-tab buttons are locked for all players (view-only).
     */
    protected static boolean mapViewEnabledFlag = true;

    // ─────────────────────────────────────────────────────────────────────────
    // Flag initialisation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resets every flag field to the hard-coded defaults in {@link XpsConfigInit}.
     */
    protected static void applyDefaults() {
        durabilityPerPointFlag = XpsConfigInit.DURABILITY_PER_POINT;
        maxAnvilRepairCostFlag = XpsConfigInit.MAX_ANVIL_REPAIR_COST;
        xpRepairEnabledFlag = XpsConfigInit.XP_REPAIR_ENABLED;

        orbModeEnum = XpsConfigInit.ORB_MODE;
        entityXpModeEnum = XpsConfigInit.ENTITY_XP_MODE;
        blockXpModeEnum = XpsConfigInit.BLOCK_XP_MODE;
        cropXpModeEnum = XpsConfigInit.CROP_XP_MODE;
        breedingXpModeEnum = XpsConfigInit.BREEDING_XP_MODE;
        fishingXpModeEnum = XpsConfigInit.FISHING_XP_MODE;
        tradingPlayerXpModeEnum = XpsConfigInit.TRADING_PLAYER_XP;
        merchantXpModeEnum = XpsConfigInit.MERCHANT_XP;
        grindstoneXpModeEnum = XpsConfigInit.GRINDSTONE_XP_MODE;
        smeltingXpModeEnum = XpsConfigInit.SMELTING_XP_MODE;

        entityXpMultiplierFlag = XpsConfigInit.ENTITY_XP_MULTIPLIER;
        blockXpMultiplierFlag = XpsConfigInit.BLOCK_XP_MULTIPLIER;
        cropXpMultiplierFlag = XpsConfigInit.CROP_XP_MULTIPLIER;
        smeltingXpMultiplierFlag = XpsConfigInit.SMELTING_XP_MULTIPLIER;
        tradingXpMultiplierFlag = XpsConfigInit.TRADING_XP_MULTIPLIER;
        merchantXpMultiplierFlag = XpsConfigInit.MERCHANT_XP_MULTIPLIER;
        breedingXpMultiplierFlag = XpsConfigInit.BREEDING_XP_MULTIPLIER;
        fishingXpMultiplierFlag = XpsConfigInit.FISHING_XP_MULTIPLIER;
        grindstoneXpMultiplierFlag = XpsConfigInit.GRINDSTONE_XP_MULTIPLIER;
        mapViewEnabledFlag = true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Flag persistence
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Applies a raw GSON map (from any JSON source) into the live flag fields.
     * Only keys present in {@code m} are applied; absent keys keep their current
     * values (i.e. the defaults from {@link #applyDefaults()}).
     */
    protected static void loadFlagsFromMap(Map<String, Object> m) {
        // Enum modes
        if (m.get("orbMode") instanceof String s) try {
            orbModeEnum = OrbMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("entityXpMode") instanceof String s) try {
            entityXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("blockXpMode") instanceof String s) try {
            blockXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("cropXpMode") instanceof String s) try {
            cropXpModeEnum = CropXpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("breedingXpMode") instanceof String s) try {
            breedingXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("fishingXpMode") instanceof String s) try {
            fishingXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("tradingPlayerXp") instanceof String s) try {
            tradingPlayerXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("merchantXp") instanceof String s) try {
            merchantXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("grindstoneXpMode") instanceof String s) try {
            grindstoneXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        if (m.get("smeltingXpMode") instanceof String s) try {
            smeltingXpModeEnum = XpMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
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
        // Scalar flags
        if (m.get("xpRepairEnabled") instanceof Boolean b) xpRepairEnabledFlag = b;
        if (m.get("durabilityPerPoint") instanceof Number n) durabilityPerPointFlag = n.floatValue();
        if (m.get("maxAnvilRepairCost") instanceof Number n) maxAnvilRepairCostFlag = n.intValue();
        if (m.get("mapViewEnabled") instanceof Boolean b) mapViewEnabledFlag = b;
    }

    /**
     * Loads flag values from {@code <dir>/xps_main_config.json} and overrides the
     * in-memory flags. No-op if the file is absent. Must be called inside a write-lock.
     */
    protected static void loadFlagsFromDir(Path dir) {
        Path f = dir.resolve("xps_main_config.json");
        if (!Files.exists(f)) return;
        try {
            Map<String, Object> m = GSON.fromJson(Files.readString(f),
                    new TypeToken<Map<String, Object>>() {
                    }.getType());
            if (m == null) return;
            if (m.get("orbMode") instanceof String s) try {
                orbModeEnum = OrbMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("entityXpMode") instanceof String s) try {
                entityXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("blockXpMode") instanceof String s) try {
                blockXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("cropXpMode") instanceof String s) try {
                cropXpModeEnum = CropXpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("breedingXpMode") instanceof String s) try {
                breedingXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("fishingXpMode") instanceof String s) try {
                fishingXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("tradingPlayerXp") instanceof String s) try {
                tradingPlayerXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("merchantXp") instanceof String s) try {
                merchantXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("grindstoneXpMode") instanceof String s) try {
                grindstoneXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("smeltingXpMode") instanceof String s) try {
                smeltingXpModeEnum = XpMode.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (m.get("entityXpMultiplier") instanceof Number n) entityXpMultiplierFlag = n.floatValue();
            if (m.get("blockXpMultiplier") instanceof Number n) blockXpMultiplierFlag = n.floatValue();
            if (m.get("cropXpMultiplier") instanceof Number n) cropXpMultiplierFlag = n.floatValue();
            if (m.get("smeltingXpMultiplier") instanceof Number n) smeltingXpMultiplierFlag = n.floatValue();
            if (m.get("tradingXpMultiplier") instanceof Number n) tradingXpMultiplierFlag = n.floatValue();
            if (m.get("merchantXpMultiplier") instanceof Number n) merchantXpMultiplierFlag = n.floatValue();
            if (m.get("breedingXpMultiplier") instanceof Number n) breedingXpMultiplierFlag = n.floatValue();
            if (m.get("fishingXpMultiplier") instanceof Number n) fishingXpMultiplierFlag = n.floatValue();
            if (m.get("grindstoneXpMultiplier") instanceof Number n) grindstoneXpMultiplierFlag = n.floatValue();
            if (m.get("xpRepairEnabled") instanceof Boolean b) xpRepairEnabledFlag = b;
            if (m.get("durabilityPerPoint") instanceof Number n) durabilityPerPointFlag = n.floatValue();
            if (m.get("maxAnvilRepairCost") instanceof Number n) maxAnvilRepairCostFlag = n.intValue();
            if (m.get("mapViewEnabled") instanceof Boolean b) mapViewEnabledFlag = b;
            LOGGER.debug("Loaded xps_main_config.json from {}", dir);
        } catch (Exception e) {
            LOGGER.error("Failed to load xps_main_config.json from {}", dir, e);
        }
    }

    /**
     * Persists all current in-memory flag values to {@code <dir>/xps_main_config.json}.
     */
    protected static void saveFlagsToDir(Path dir) {
        Map<String, Object> f = new LinkedHashMap<>();
        // Enum modes — stored as lowercase strings
        f.put("orbMode", orbModeEnum != null ? orbModeEnum.name() : OrbMode.VANILLA.name());
        f.put("entityXpMode", entityXpModeEnum != null ? entityXpModeEnum.name() : XpMode.VANILLA.name());
        f.put("blockXpMode", blockXpModeEnum != null ? blockXpModeEnum.name() : XpMode.VANILLA.name());
        f.put("cropXpMode", cropXpModeEnum != null ? cropXpModeEnum.name() : CropXpMode.OFF.name());
        f.put("breedingXpMode", breedingXpModeEnum != null ? breedingXpModeEnum.name() : XpMode.VANILLA.name());
        f.put("fishingXpMode", fishingXpModeEnum != null ? fishingXpModeEnum.name() : XpMode.VANILLA.name());
        f.put("tradingPlayerXp", tradingPlayerXpModeEnum != null ? tradingPlayerXpModeEnum.name() : XpMode.VANILLA.name());
        f.put("merchantXp", merchantXpModeEnum != null ? merchantXpModeEnum.name() : XpMode.VANILLA.name());
        f.put("grindstoneXpMode", grindstoneXpModeEnum != null ? grindstoneXpModeEnum.name() : XpMode.VANILLA.name());
        f.put("smeltingXpMode", smeltingXpModeEnum != null ? smeltingXpModeEnum.name() : XpMode.VANILLA.name());
        // Multipliers
        f.put("entityXpMultiplier", entityXpMultiplierFlag);
        f.put("blockXpMultiplier", blockXpMultiplierFlag);
        f.put("cropXpMultiplier", cropXpMultiplierFlag);
        f.put("smeltingXpMultiplier", smeltingXpMultiplierFlag);
        f.put("tradingXpMultiplier", tradingXpMultiplierFlag);
        f.put("merchantXpMultiplier", merchantXpMultiplierFlag);
        f.put("breedingXpMultiplier", breedingXpMultiplierFlag);
        f.put("fishingXpMultiplier", fishingXpMultiplierFlag);
        f.put("grindstoneXpMultiplier", grindstoneXpMultiplierFlag);
        // Scalar flags
        f.put("xpRepairEnabled", xpRepairEnabledFlag);
        f.put("durabilityPerPoint", durabilityPerPointFlag);
        f.put("maxAnvilRepairCost", maxAnvilRepairCostFlag);
        f.put("mapViewEnabled", mapViewEnabledFlag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("xps_main_config.json"), GSON.toJson(f));
            LOGGER.debug("Saved xps_main_config.json to {}", dir);
        } catch (IOException e) {
            LOGGER.error("Failed to save xps_main_config.json to {}", dir, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Category file I/O
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deserialises one category JSON file into the matching field of {@link #data}.
     * No-op if the file does not exist.
     */
    protected static void loadCategoryFile(Path path, String fileName) {
        if (!Files.exists(path)) {
            System.out.println("Category file " + fileName + " not found at " + path + "; skipping.");
            return;
        }

        try {
            String json = Files.readString(path);
            switch (fileName) {
                case "Blocks" -> {
                    Map<String, BlockCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, BlockCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Blocks = loaded;
                        LOGGER.debug("Loaded Blocks config");
                    }
                }
                case "Crops" -> {
                    Map<String, CropCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, CropCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Crops = loaded;
                        LOGGER.debug("Loaded Crops config");
                    }
                }
                case "Entities" -> {
                    Map<String, EntitiesCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, EntitiesCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Entities = loaded;
                        LOGGER.debug("Loaded Entities config");
                    }
                }
                case "Smelting" -> {
                    Map<String, SmeltingCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, SmeltingCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Smelting = loaded;
                        LOGGER.debug("Loaded Smelting config");
                    }
                }
                case "Trading" -> {
                    Map<String, TradingCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, TradingCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Trading = loaded;
                        LOGGER.debug("Loaded Trading config");
                    }
                }
                case "Breeding" -> {
                    Map<String, BreedingCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, BreedingCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Breeding = loaded;
                        LOGGER.debug("Loaded Breeding config");
                    }
                }
                case "Fishing" -> {
                    Map<String, FishingCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, FishingCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Fishing = loaded;
                        LOGGER.debug("Loaded Fishing config");
                    }
                }
                case "Grindstone" -> {
                    Map<String, GrindstoneCategory> loaded = GSON.fromJson(json,
                            new TypeToken<Map<String, GrindstoneCategory>>() {
                            }.getType());
                    if (loaded != null) {
                        data.Grindstone = loaded;
                        LOGGER.debug("Loaded Grindstone config");
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("Invalid JSON in {} config file", fileName, e);
        } catch (IOException e) {
            LOGGER.error("Failed to load {} config", fileName, e);
        }
    }

    /**
     * Writes all non-null category maps from {@link #data} into the given directory
     * as {@code Blocks.json}, {@code Crops.json}, … etc.
     * Used by both the master-config save and the world-dir save path.
     */
    protected static void saveCategoriesToDir(Path dir) {
        try {
            Files.createDirectories(dir);
            if (data.Blocks != null) Files.writeString(dir.resolve("Blocks.json"), GSON.toJson(data.Blocks));
            if (data.Crops != null) Files.writeString(dir.resolve("Crops.json"), GSON.toJson(data.Crops));
            if (data.Entities != null) Files.writeString(dir.resolve("Entities.json"), GSON.toJson(data.Entities));
            if (data.Smelting != null) Files.writeString(dir.resolve("Smelting.json"), GSON.toJson(data.Smelting));
            if (data.Trading != null) Files.writeString(dir.resolve("Trading.json"), GSON.toJson(data.Trading));
            if (data.Breeding != null) Files.writeString(dir.resolve("Breeding.json"), GSON.toJson(data.Breeding));
            if (data.Fishing != null) Files.writeString(dir.resolve("Fishing.json"), GSON.toJson(data.Fishing));
            if (data.Grindstone != null) Files.writeString(dir.resolve("Grindstone.json"), GSON.toJson(data.Grindstone));
            LOGGER.debug("Saved category files to {}", dir);
        } catch (IOException e) {
            LOGGER.error("Failed to save category files to {}", dir, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public flag / mode getters
    // ─────────────────────────────────────────────────────────────────────────

    // Mending / anvil-repair
    public static boolean isXpRepairEnabled() {
        return xpRepairEnabledFlag;
    }

    public static int getMaxAnvilRepairCost() {
        return maxAnvilRepairCostFlag;
    }

    /**
     * Durability points restored per XP point spent on mending (float).
     */
    public static float getDurabilityPerPoint() {
        return durabilityPerPointFlag;
    }

    // XP-mode enums
    public static OrbMode getOrbModeEnum() {
        return orbModeEnum;
    }

    public static XpMode getEntityXpModeEnum() {
        return entityXpModeEnum;
    }

    public static XpMode getBlockXpModeEnum() {
        return blockXpModeEnum;
    }

    public static CropXpMode getCropXpModeEnum() {
        return cropXpModeEnum;
    }

    public static XpMode getBreedingXpModeEnum() {
        return breedingXpModeEnum;
    }

    public static XpMode getFishingXpModeEnum() {
        return fishingXpModeEnum;
    }

    public static XpMode getTradingPlayerXpModeEnum() {
        return tradingPlayerXpModeEnum;
    }

    public static XpMode getMerchantXpModeEnum() {
        return merchantXpModeEnum;
    }

    public static XpMode getGrindstoneXpModeEnum() {
        return grindstoneXpModeEnum;
    }

    public static XpMode getSmeltingXpModeEnum() {
        return smeltingXpModeEnum;
    }

    // Per-system multipliers — read by handlers/mixins at award time
    public static float getEntityXpMultiplier() {
        return entityXpMultiplierFlag;
    }

    public static float getBlockXpMultiplier() {
        return blockXpMultiplierFlag;
    }

    public static float getCropXpMultiplier() {
        return cropXpMultiplierFlag;
    }

    public static float getSmeltingXpMultiplier() {
        return smeltingXpMultiplierFlag;
    }

    public static float getTradingXpMultiplier() {
        return tradingXpMultiplierFlag;
    }

    public static float getMerchantXpMultiplier() {
        return merchantXpMultiplierFlag;
    }

    public static float getBreedingXpMultiplier() {
        return breedingXpMultiplierFlag;
    }

    public static float getFishingXpMultiplier() {
        return fishingXpMultiplierFlag;
    }

    public static float getGrindstoneXpMultiplier() {
        return grindstoneXpMultiplierFlag;
    }

    /**
     * Returns {@code true} if maps-tab editing is globally enabled by the server.
     */
    public static boolean isMapViewEnabled() {
        return mapViewEnabledFlag;
    }

    /**
     * Sets the maps-tab edit flag and persists flags to {@code worldRoot/xp_simplifier/}.
     */
    public static void setMapViewEnabled(boolean enabled, Path worldRoot) {
        mapViewEnabledFlag = enabled;
        lock.readLock().lock();
        try {
            saveFlagsToDir(worldRoot.resolve("xp_simplifier"));
        } finally {
            lock.readLock().unlock();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public XP lookup getters (rawId-based — no loader registry dependency)
    // ─────────────────────────────────────────────────────────────────────────

    public static float getRecipeXp(String productId) {
        if (productId == null) return -1.0f;
        Float value = recipeIndex.get(productId);
        return value != null ? value : -1.0f;
    }

    /**
     * Returns XP for the given block/crop raw-ID, enforcing the maxAge gate for crops.
     */
    public static Integer getBlockXp(int rawId, boolean isMaxAge) {
        XpValue cropValue = cropIndex.get(rawId);
        if (cropValue != null) {
            if (!isMaxAge) return 0;
            return cropValue.calculateXp();
        }
        XpValue blockValue = blockIndex.get(rawId);
        return blockValue != null ? blockValue.calculateXp() : null;
    }

    /**
     * Overload — returns XP for a plain block (no maturity check).
     */
    public static Integer getBlockXp(int rawId) {
        return getBlockXp(rawId, false);
    }

    public static Integer getEntityXp(int rawId) {
        XpValue xpValue = entityIndex.get(rawId);
        return xpValue != null ? xpValue.calculateXp() : null;
    }

    public static Integer getDragonXp(boolean respawned) {
        XpValue xpValue = respawned ? dragonRespawnedXp : dragonFirstXp;
        return xpValue != null ? xpValue.calculateXp() : null;
    }

    public static Integer getPlayerXp(int rawId) {
        TradingXpValues xpValues = tradingIndex.get(rawId);
        return xpValues != null ? xpValues.playerXp.calculateXp() : null;
    }

    public static Integer getMerchantXp(int rawId) {
        TradingXpValues xpValues = tradingIndex.get(rawId);
        return xpValues != null ? xpValues.merchantXp.calculateXp() : null;
    }

    public static int getWanderingTraderRawId() {
        return wanderingTraderRawId;
    }

    public static Integer getBreedingXp(int rawId) {
        XpValue xpValue = breedingIndex.get(rawId);
        return xpValue != null ? xpValue.calculateXp() : null;
    }

    public static Integer getFishingXp(int rawId) {
        XpValue xpValue = fishingIndex.get(rawId);
        return xpValue != null ? xpValue.calculateXp() : null;
    }

    public static Integer getGlobalFishingXp() {
        return globalFishingXp != null ? globalFishingXp.calculateXp() : null;
    }

    /**
     * Returns XP for a grindstone enchantment. Pass {@code id.toString()} from either
     * a NeoForge {@code ResourceLocation} or a Fabric {@code Identifier} — both produce
     * the same {@code "namespace:path"} string used as the index key.
     */
    public static Integer getGrindstoneXp(String id) {
        XpValue xpValue = grindstoneIndex.get(id);
        return xpValue != null ? xpValue.calculateXp() : null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ConfigData — maps-only holder; populated by the subclass load() / buildIndexes()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Holds the 8 category maps. Flags are NOT stored here — they live in
     * {@code xps_main_config.json} and in the static flag fields above.
     */
    public static class ConfigData {
        public Map<String, SmeltingCategory> Smelting = new LinkedHashMap<>();
        public Map<String, BlockCategory> Blocks = new LinkedHashMap<>();
        public Map<String, EntitiesCategory> Entities = new LinkedHashMap<>();
        public Map<String, CropCategory> Crops = new LinkedHashMap<>();
        public Map<String, TradingCategory> Trading = new LinkedHashMap<>();
        public Map<String, BreedingCategory> Breeding = new LinkedHashMap<>();
        public Map<String, FishingCategory> Fishing = new LinkedHashMap<>();
        public Map<String, GrindstoneCategory> Grindstone = new LinkedHashMap<>();

        public void initializeDefaults() {
            addDefaultCategories();
        }

        private void addDefaultCategories() {

            if (Entities == null || Entities.isEmpty()) {
                EntitiesCategory xpBottle = new EntitiesCategory();
                xpBottle.entities = new LinkedHashMap<>();
                xpBottle.entities.put("minecraft:experience_bottle", new XpValue(3, 11));
                Entities.put("Items", xpBottle);

                EntitiesCategory bosses = new EntitiesCategory();
                bosses.entities = new LinkedHashMap<>();
                bosses.entities.put("minecraft:ender_dragon_first", new XpValue(12000));
                bosses.entities.put("minecraft:ender_dragon_respawned", new XpValue(500));
                bosses.entities.put("minecraft:wither", new XpValue(50));
                bosses.entities.put("minecraft:warden", new XpValue(50));
                Entities.put("Bosses", bosses);

                EntitiesCategory hostileMobs = new EntitiesCategory();
                hostileMobs.entities = new LinkedHashMap<>();
                hostileMobs.entities.put("minecraft:husk", new XpValue(5));
                hostileMobs.entities.put("minecraft:zombie", new XpValue(5));
                hostileMobs.entities.put("minecraft:skeleton", new XpValue(5));
                hostileMobs.entities.put("minecraft:creeper", new XpValue(5));
                hostileMobs.entities.put("minecraft:enderman", new XpValue(5));
                hostileMobs.entities.put("minecraft:zombie_villager", new XpValue(5));
                hostileMobs.entities.put("minecraft:spider", new XpValue(5));
                hostileMobs.entities.put("minecraft:stray", new XpValue(5));
                hostileMobs.entities.put("minecraft:cave_spider", new XpValue(5));
                hostileMobs.entities.put("minecraft:silverfish", new XpValue(5));
                hostileMobs.entities.put("minecraft:piglin_brute", new XpValue(20));
                hostileMobs.entities.put("minecraft:ravager", new XpValue(20));
                hostileMobs.entities.put("minecraft:blaze", new XpValue(10));
                hostileMobs.entities.put("minecraft:evoker", new XpValue(10));
                hostileMobs.entities.put("minecraft:guardian", new XpValue(10));
                hostileMobs.entities.put("minecraft:elder_guardian", new XpValue(10));
                hostileMobs.entities.put("minecraft:zombified_piglin", new XpValue(5));
                hostileMobs.entities.put("minecraft:piglin", new XpValue(5));
                hostileMobs.entities.put("minecraft:ghast", new XpValue(5));
                hostileMobs.entities.put("minecraft:hoglin", new XpValue(5));
                hostileMobs.entities.put("minecraft:magma_cube", new XpValue(1, 4));
                hostileMobs.entities.put("minecraft:pillager", new XpValue(5));
                hostileMobs.entities.put("minecraft:drowned", new XpValue(5));
                hostileMobs.entities.put("minecraft:shulker", new XpValue(5));
                hostileMobs.entities.put("minecraft:wither_skeleton", new XpValue(5));
                hostileMobs.entities.put("minecraft:illusioner", new XpValue(5));
                hostileMobs.entities.put("minecraft:phantom", new XpValue(5));
                hostileMobs.entities.put("minecraft:vex", new XpValue(5));
                hostileMobs.entities.put("minecraft:vindicator", new XpValue(5));
                hostileMobs.entities.put("minecraft:zoglin", new XpValue(5));
                hostileMobs.entities.put("minecraft:slime", new XpValue(1, 4));
                hostileMobs.entities.put("minecraft:endermite", new XpValue(3));
                hostileMobs.entities.put("minecraft:witch", new XpValue(5));
                Entities.put("Hostile Mobs", hostileMobs);

                EntitiesCategory passiveMobs = new EntitiesCategory();
                passiveMobs.entities = new LinkedHashMap<>();
                passiveMobs.entities.put("minecraft:axolotl", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:camel", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:cat", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:chicken", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:cod", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:cow", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:donkey", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:fox", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:frog", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:glow_squid", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:horse", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:mooshroom", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:ocelot", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:parrot", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:pig", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:pufferfish", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:rabbit", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:salmon", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:sheep", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:squid", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:strider", new XpValue(1, 2));
                passiveMobs.entities.put("minecraft:tropical_fish", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:turtle", new XpValue(1, 3));
                passiveMobs.entities.put("minecraft:villager", new XpValue(0));
                passiveMobs.entities.put("minecraft:wandering_trader", new XpValue(0));
                Entities.put("Passive Mobs", passiveMobs);

                EntitiesCategory neutralMobs = new EntitiesCategory();
                neutralMobs.entities = new LinkedHashMap<>();
                neutralMobs.entities.put("minecraft:bee", new XpValue(1, 3));
                neutralMobs.entities.put("minecraft:dolphin", new XpValue(1, 3));
                neutralMobs.entities.put("minecraft:goat", new XpValue(1, 3));
                neutralMobs.entities.put("minecraft:llama", new XpValue(1, 3));
                neutralMobs.entities.put("minecraft:panda", new XpValue(1, 3));
                neutralMobs.entities.put("minecraft:polar_bear", new XpValue(1, 3));
                neutralMobs.entities.put("minecraft:trader_llama", new XpValue(1, 3));
                neutralMobs.entities.put("minecraft:wolf", new XpValue(1, 3));
                Entities.put("Neutral Mobs", neutralMobs);

                EntitiesCategory custom = new EntitiesCategory();
                custom.entities = new LinkedHashMap<>();
                Entities.put("Custom", custom);
            }

            if (Blocks == null || Blocks.isEmpty()) {
                BlockCategory spawners = new BlockCategory();
                spawners.blocks = new LinkedHashMap<>();
                spawners.blocks.put("minecraft:spawner", new XpValue(15, 43));
                Blocks.put("Spawners", spawners);

                BlockCategory ores = new BlockCategory();
                ores.blocks = new LinkedHashMap<>();
                ores.blocks.put("minecraft:coal_ore", new XpValue(0, 2));
                ores.blocks.put("minecraft:deepslate_coal_ore", new XpValue(0, 2));
                ores.blocks.put("minecraft:iron_ore", new XpValue(0));
                ores.blocks.put("minecraft:deepslate_iron_ore", new XpValue(0));
                ores.blocks.put("minecraft:copper_ore", new XpValue(0));
                ores.blocks.put("minecraft:deepslate_copper_ore", new XpValue(0));
                ores.blocks.put("minecraft:gold_ore", new XpValue(0));
                ores.blocks.put("minecraft:deepslate_gold_ore", new XpValue(0));
                ores.blocks.put("minecraft:redstone_ore", new XpValue(1, 5));
                ores.blocks.put("minecraft:deepslate_redstone_ore", new XpValue(1, 5));
                ores.blocks.put("minecraft:lapis_ore", new XpValue(2, 5));
                ores.blocks.put("minecraft:deepslate_lapis_ore", new XpValue(2, 5));
                ores.blocks.put("minecraft:diamond_ore", new XpValue(3, 7));
                ores.blocks.put("minecraft:deepslate_diamond_ore", new XpValue(3, 7));
                ores.blocks.put("minecraft:emerald_ore", new XpValue(3, 7));
                ores.blocks.put("minecraft:deepslate_emerald_ore", new XpValue(3, 7));
                ores.blocks.put("minecraft:nether_gold_ore", new XpValue(0, 1));
                ores.blocks.put("minecraft:nether_quartz_ore", new XpValue(2, 5));
                ores.blocks.put("minecraft:ancient_debris", new XpValue(2, 6));
                Blocks.put("Ores", ores);

                BlockCategory wood = new BlockCategory();
                wood.blocks = new LinkedHashMap<>();
                Blocks.put("Wood", wood);

                BlockCategory commonBlocks = new BlockCategory();
                commonBlocks.blocks = new LinkedHashMap<>();
                Blocks.put("Common Blocks", commonBlocks);

                BlockCategory nether = new BlockCategory();
                nether.blocks = new LinkedHashMap<>();
                Blocks.put("Nether", nether);

                BlockCategory end = new BlockCategory();
                end.blocks = new LinkedHashMap<>();
                Blocks.put("End", end);

                BlockCategory sculk = new BlockCategory();
                sculk.blocks = new LinkedHashMap<>();
                sculk.blocks.put("minecraft:sculk", new XpValue(1));
                sculk.blocks.put("minecraft:sculk_sensor", new XpValue(5));
                sculk.blocks.put("minecraft:sculk_shrieker", new XpValue(5));
                sculk.blocks.put("minecraft:sculk_catalyst", new XpValue(5));
                Blocks.put("Sculk", sculk);

                BlockCategory custom = new BlockCategory();
                custom.blocks = new LinkedHashMap<>();
                Blocks.put("Custom", custom);
            }

            if (Crops == null || Crops.isEmpty()) {
                CropCategory crops = new CropCategory();
                crops.crops = new LinkedHashMap<>();
                crops.crops.put("minecraft:wheat", new XpValue(0));
                crops.crops.put("minecraft:carrots", new XpValue(0));
                crops.crops.put("minecraft:potatoes", new XpValue(0));
                crops.crops.put("minecraft:beetroots", new XpValue(0));
                crops.crops.put("minecraft:nether_wart", new XpValue(0));
                crops.crops.put("minecraft:cocoa", new XpValue(0));
                Crops.put("Minecraft Crops", crops);

                CropCategory custom = new CropCategory();
                custom.crops = new LinkedHashMap<>();
                Crops.put("Custom", custom);
            }

            if (Smelting == null || Smelting.isEmpty()) {
                SmeltingCategory smeltingOres = new SmeltingCategory();
                smeltingOres.smelting = new LinkedHashMap<>();
                smeltingOres.smelting.put("minecraft:iron_ingot", 0.7f);
                smeltingOres.smelting.put("minecraft:copper_ingot", 0.7f);
                smeltingOres.smelting.put("minecraft:gold_ingot", 1.0f);
                smeltingOres.smelting.put("minecraft:redstone_dust", 0.7f);
                smeltingOres.smelting.put("minecraft:netherite_scrap", 2.0f);
                Smelting.put("Smelting Ores", smeltingOres);

                SmeltingCategory cooking = new SmeltingCategory();
                cooking.smelting = new LinkedHashMap<>();
                cooking.smelting.put("minecraft:baked_potato", 0.35f);
                cooking.smelting.put("minecraft:dried_kelp", 0.1f);
                cooking.smelting.put("minecraft:cooked_beef", 0.35f);
                cooking.smelting.put("minecraft:cooked_porkchop", 0.35f);
                cooking.smelting.put("minecraft:cooked_rabbit", 0.35f);
                cooking.smelting.put("minecraft:cooked_cod", 0.35f);
                cooking.smelting.put("minecraft:cooked_salmon", 0.35f);
                cooking.smelting.put("minecraft:cooked_chicken", 0.35f);
                cooking.smelting.put("minecraft:cooked_mutton", 0.35f);
                Smelting.put("Cooking", cooking);

                SmeltingCategory smeltingBlocks = new SmeltingCategory();
                smeltingBlocks.smelting = new LinkedHashMap<>();
                smeltingBlocks.smelting.put("minecraft:stone", 0.1f);
                smeltingBlocks.smelting.put("minecraft:stone_bricks", 0.1f);
                smeltingBlocks.smelting.put("minecraft:cracked_deepslate_bricks", 0.1f);
                smeltingBlocks.smelting.put("minecraft:cracked_deepslate_tiles", 0.1f);
                smeltingBlocks.smelting.put("minecraft:sandstone", 0.1f);
                smeltingBlocks.smelting.put("minecraft:red_sandstone", 0.1f);
                smeltingBlocks.smelting.put("minecraft:nether_bricks", 0.1f);
                smeltingBlocks.smelting.put("minecraft:polished_blackstone_bricks", 0.1f);
                smeltingBlocks.smelting.put("minecraft:smooth_basalt", 0.1f);
                smeltingBlocks.smelting.put("minecraft:glass", 0.1f);
                smeltingBlocks.smelting.put("minecraft:sponge", 0.15f);
                smeltingBlocks.smelting.put("minecraft:popped_chorus_fruit", 0.1f);
                smeltingBlocks.smelting.put("minecraft:lime_dye", 0.1f);
                smeltingBlocks.smelting.put("minecraft:green_dye", 0.1f);
                smeltingBlocks.smelting.put("minecraft:brick", 0.1f);
                smeltingBlocks.smelting.put("minecraft:nether_brick", 0.1f);
                Smelting.put("Smelting Blocks", smeltingBlocks);

                SmeltingCategory custom = new SmeltingCategory();
                custom.smelting = new LinkedHashMap<>();
                Smelting.put("Custom", custom);
            }

            if (Trading == null || Trading.isEmpty()) {
                TradingCategory merchants = new TradingCategory();
                merchants.trader = new LinkedHashMap<>();
                merchants.trader.put("minecraft:armorer", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:butcher", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:cartographer", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:cleric", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:farmer", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:fisherman", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:fletcher", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:leatherworker", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:librarian", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:mason", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:shepherd", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:toolsmith", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:weaponsmith", new TradingXpValues(3, 6, 2, 6));
                merchants.trader.put("minecraft:wandering_trader", new TradingXpValues(5, 5, 0, 0));
                Trading.put("Merchants", merchants);

                TradingCategory custom = new TradingCategory();
                custom.trader = new LinkedHashMap<>();
                Trading.put("Custom", custom);
            }

            if (Breeding == null || Breeding.isEmpty()) {
                BreedingCategory animals = new BreedingCategory();
                animals.breeding = new LinkedHashMap<>();
                animals.breeding.put("minecraft:axolotl", new XpValue(1, 7));
                animals.breeding.put("minecraft:bee", new XpValue(1, 7));
                animals.breeding.put("minecraft:cat", new XpValue(1, 7));
                animals.breeding.put("minecraft:camel", new XpValue(1, 7));
                animals.breeding.put("minecraft:cow", new XpValue(1, 7));
                animals.breeding.put("minecraft:chicken", new XpValue(1, 7));
                animals.breeding.put("minecraft:donkey", new XpValue(1, 7));
                animals.breeding.put("minecraft:sheep", new XpValue(1, 7));
                animals.breeding.put("minecraft:pig", new XpValue(1, 7));
                animals.breeding.put("minecraft:horse", new XpValue(1, 7));
                animals.breeding.put("minecraft:hoglin", new XpValue(1, 7));
                animals.breeding.put("minecraft:goat", new XpValue(1, 7));
                animals.breeding.put("minecraft:mooshroom", new XpValue(1, 7));
                animals.breeding.put("minecraft:rabbit", new XpValue(1, 7));
                animals.breeding.put("minecraft:fox", new XpValue(1, 7));
                animals.breeding.put("minecraft:panda", new XpValue(1, 7));
                animals.breeding.put("minecraft:turtle", new XpValue(1, 7));
                animals.breeding.put("minecraft:ocelot", new XpValue(1, 7));
                animals.breeding.put("minecraft:parrot", new XpValue(1, 7));
                animals.breeding.put("minecraft:frog", new XpValue(1, 7));
                animals.breeding.put("minecraft:sniffer", new XpValue(1, 7));
                animals.breeding.put("minecraft:strider", new XpValue(1, 7));
                animals.breeding.put("minecraft:wolf", new XpValue(1, 7));
                Breeding.put("Animals", animals);

                BreedingCategory custom = new BreedingCategory();
                custom.breeding = new LinkedHashMap<>();
                Breeding.put("Custom", custom);
            }

            if (Fishing == null || Fishing.isEmpty()) {
                FishingCategory global = new FishingCategory();
                global.fishing = new LinkedHashMap<>();
                global.fishing.put("Global_Fishing", new XpValue(1, 6));
                Fishing.put("Global", global);

                FishingCategory fish = new FishingCategory();
                fish.fishing = new LinkedHashMap<>();
                fish.fishing.put("minecraft:cod", new XpValue(1, 6));
                fish.fishing.put("minecraft:salmon", new XpValue(1, 6));
                fish.fishing.put("minecraft:tropical_fish", new XpValue(1, 6));
                fish.fishing.put("minecraft:pufferfish", new XpValue(1, 6));
                Fishing.put("Fish", fish);

                FishingCategory junk = new FishingCategory();
                junk.fishing = new LinkedHashMap<>();
                junk.fishing.put("minecraft:stick", new XpValue(1, 6));
                junk.fishing.put("minecraft:string", new XpValue(1, 6));
                junk.fishing.put("minecraft:leather", new XpValue(1, 6));
                junk.fishing.put("minecraft:bone", new XpValue(1, 6));
                junk.fishing.put("minecraft:water_bottle", new XpValue(1, 6));
                junk.fishing.put("minecraft:ink_sack", new XpValue(1, 6));
                junk.fishing.put("minecraft:rotten_flesh", new XpValue(1, 6));
                junk.fishing.put("minecraft:bowl", new XpValue(1, 6));
                junk.fishing.put("minecraft:tripwire_hook", new XpValue(1, 6));
                junk.fishing.put("minecraft:lilly_pad", new XpValue(1, 6));
                Fishing.put("Junk", junk);

                FishingCategory treasure = new FishingCategory();
                treasure.fishing = new LinkedHashMap<>();
                treasure.fishing.put("minecraft:enchanted_book", new XpValue(1, 6));
                treasure.fishing.put("minecraft:name_tag", new XpValue(1, 6));
                treasure.fishing.put("minecraft:nautilus_shell", new XpValue(1, 6));
                treasure.fishing.put("minecraft:saddle", new XpValue(1, 6));
                treasure.fishing.put("minecraft:bow", new XpValue(1, 6));
                treasure.fishing.put("minecraft:fishing_rod", new XpValue(1, 6));
                treasure.fishing.put("minecraft:leather_boots", new XpValue(1, 6));
                Fishing.put("Treasure", treasure);

                FishingCategory custom = new FishingCategory();
                custom.fishing = new LinkedHashMap<>();
                Fishing.put("Custom", custom);
            }

            if (Grindstone == null || Grindstone.isEmpty()) {
                GrindstoneCategory melee = new GrindstoneCategory();
                melee.enchantments = new LinkedHashMap<>();
                melee.enchantments.put("minecraft:sharpness", new XpValue(1, 4));
                melee.enchantments.put("minecraft:smite", new XpValue(1, 4));
                melee.enchantments.put("minecraft:bane_of_arthropods", new XpValue(1, 4));
                melee.enchantments.put("minecraft:looting", new XpValue(1, 4));
                melee.enchantments.put("minecraft:fire_aspect", new XpValue(1, 4));
                melee.enchantments.put("minecraft:knockback", new XpValue(1, 4));
                melee.enchantments.put("minecraft:sweeping_edge", new XpValue(1, 4));
                Grindstone.put("Melee", melee);

                GrindstoneCategory tools = new GrindstoneCategory();
                tools.enchantments = new LinkedHashMap<>();
                tools.enchantments.put("minecraft:efficiency", new XpValue(1, 4));
                tools.enchantments.put("minecraft:fortune", new XpValue(1, 4));
                tools.enchantments.put("minecraft:silk_touch", new XpValue(1, 4));
                tools.enchantments.put("minecraft:unbreaking", new XpValue(1, 4));
                tools.enchantments.put("minecraft:mending", new XpValue(5, 15));
                Grindstone.put("Tools", tools);

                GrindstoneCategory armor = new GrindstoneCategory();
                armor.enchantments = new LinkedHashMap<>();
                armor.enchantments.put("minecraft:protection", new XpValue(1, 4));
                armor.enchantments.put("minecraft:fire_protection", new XpValue(1, 4));
                armor.enchantments.put("minecraft:blast_protection", new XpValue(1, 4));
                armor.enchantments.put("minecraft:projectile_protection", new XpValue(1, 4));
                armor.enchantments.put("minecraft:feather_falling", new XpValue(1, 4));
                armor.enchantments.put("minecraft:respiration", new XpValue(1, 4));
                armor.enchantments.put("minecraft:aqua_affinity", new XpValue(1, 4));
                armor.enchantments.put("minecraft:depth_strider", new XpValue(1, 4));
                armor.enchantments.put("minecraft:frost_walker", new XpValue(1, 4));
                armor.enchantments.put("minecraft:thorns", new XpValue(1, 4));
                Grindstone.put("Armor", armor);

                GrindstoneCategory bow = new GrindstoneCategory();
                bow.enchantments = new LinkedHashMap<>();
                bow.enchantments.put("minecraft:power", new XpValue(1, 4));
                bow.enchantments.put("minecraft:punch", new XpValue(1, 4));
                bow.enchantments.put("minecraft:flame", new XpValue(1, 4));
                bow.enchantments.put("minecraft:infinity", new XpValue(5, 10));
                Grindstone.put("Bow", bow);

                GrindstoneCategory fishingRod = new GrindstoneCategory();
                fishingRod.enchantments = new LinkedHashMap<>();
                fishingRod.enchantments.put("minecraft:luck_of_the_sea", new XpValue(1, 4));
                fishingRod.enchantments.put("minecraft:lure", new XpValue(1, 4));
                Grindstone.put("Fishing", fishingRod);

                GrindstoneCategory custom = new GrindstoneCategory();
                custom.enchantments = new LinkedHashMap<>();
                Grindstone.put("Custom", custom);
            }
        }
    }
}
