package qdeeme.xp_simplifier.util;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;

public class Config {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("Xp Simplifier");
	private static final Path MAIN_CONFIG_PATH = CONFIG_DIR.resolve("config.json");
	private static final Path BLOCKS_CONFIG_PATH = CONFIG_DIR.resolve("Blocks.json");
	private static final Path CROPS_CONFIG_PATH = CONFIG_DIR.resolve("Crops.json");
	private static final Path ENTITIES_CONFIG_PATH = CONFIG_DIR.resolve("Entities.json");
	private static final Path SMELTING_CONFIG_PATH = CONFIG_DIR.resolve("Smelting.json");
	private static final Path TRADING_CONFIG_PATH = CONFIG_DIR.resolve("Trading.json");
	private static final Path BREEDING_CONFIG_PATH = CONFIG_DIR.resolve("Breeding.json");
 	private static final Path FISHING_CONFIG_PATH = CONFIG_DIR.resolve("Fishing.json");
	private static final Path GRINDSTONE_CONFIG_PATH = CONFIG_DIR.resolve("Grindstone.json");
	private static final Random RANDOM = new Random();
	private static final ReadWriteLock lock = new ReentrantReadWriteLock();

	private static ConfigData data = new ConfigData();
	
	// Index maps for O(1) lookups — all keyed by registry raw ID, no string alloc on every event
	private static final Int2ObjectOpenHashMap<XpValue> entityIndex = new Int2ObjectOpenHashMap<>();
	private static final Int2ObjectOpenHashMap<XpValue> blockIndex = new Int2ObjectOpenHashMap<>();
	private static final Int2ObjectOpenHashMap<XpValue> cropIndex = new Int2ObjectOpenHashMap<>();
	private static final Map<String, Float> recipeIndex = new LinkedHashMap<>();
	private static final Int2ObjectOpenHashMap<TradingXpValues> tradingIndex = new Int2ObjectOpenHashMap<>();
	private static final Int2ObjectOpenHashMap<XpValue> breedingIndex = new Int2ObjectOpenHashMap<>();
	private static final Int2ObjectOpenHashMap<XpValue> fishingIndex = new Int2ObjectOpenHashMap<>();
	private static final HashMap<Identifier, XpValue> grindstoneIndex = new HashMap<>();

	private static XpValue dragonFirstXp;
	private static XpValue dragonRespawnedXp;
	private static int wanderingTraderRawId = -1;
	private static XpValue globalFishingXp;
	
	// Flags for quick checks
	private static int durabilityPerLevelFlag;
	private static int maxAnvilRepairCostFlag;
	private static boolean xpRepairEnabledFlag;
	// Enum flags
	private static OrbMode orbModeEnum;
	private static XpMode entityXpModeEnum;
	private static XpMode blockXpModeEnum;
	private static XpMode cropXpModeEnum;
	private static XpMode breedingXpModeEnum;
	private static XpMode fishingXpModeEnum;
	private static XpMode tradingPlayerXpModeEnum;
	private static XpMode merchantXpModeEnum;
	private static XpMode grindstoneXpModeEnum;
	private static XpMode smeltingXpModeEnum;

	public static void load() {
		lock.writeLock().lock();
		try {
			// Create config directory if it doesn't exist
			if (!Files.exists(CONFIG_DIR)) {
				Files.createDirectories(CONFIG_DIR);
				LOGGER.info("Created config directory: {}", CONFIG_DIR);
			}

			boolean needsSave = false;
			data = new ConfigData();

			// Load main config
			if (Files.exists(MAIN_CONFIG_PATH)) {
				try {
					String json = Files.readString(MAIN_CONFIG_PATH);
					ConfigData mainConfig = GSON.fromJson(json, ConfigData.class);
					if (mainConfig != null) {
						// Copy main settings
					copyConfigFlags(mainConfig, data);
					LOGGER.info("Loaded main config");
					}
				} catch (JsonSyntaxException e) {
					LOGGER.error("Invalid JSON in main config file", e);
					needsSave = true;
				}
			} else {
				needsSave = true;
			}

			// Load category files
			loadCategoryFile(BLOCKS_CONFIG_PATH, "Blocks");
			loadCategoryFile(CROPS_CONFIG_PATH, "Crops");
			loadCategoryFile(ENTITIES_CONFIG_PATH, "Entities");
			loadCategoryFile(SMELTING_CONFIG_PATH, "Smelting");
			loadCategoryFile(TRADING_CONFIG_PATH, "Trading");
			loadCategoryFile(BREEDING_CONFIG_PATH, "Breeding");
			loadCategoryFile(FISHING_CONFIG_PATH, "Fishing");
			loadCategoryFile(GRINDSTONE_CONFIG_PATH, "Grindstone");

			// Check if any category is empty and needs defaults
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
			
			// Always build indexes and update flags after loading
			buildIndexes();
			updateFlags();

			// Save initial config if default or invalid
			if (needsSave) {
				saveInternal();
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load config", e);
			data = new ConfigData();
			data.initializeDefaults();
		} finally {
			lock.writeLock().unlock();
		}
	}

	private static void loadCategoryFile(Path path, String categoryName) {
		if (!Files.exists(path)) {
			return;
		}

		try {
			String json = Files.readString(path);
			switch (categoryName) {
				case "Blocks" -> {
					Map<String, BlockCategory> loaded = GSON.fromJson(json, 
						new TypeToken<Map<String, BlockCategory>>(){}.getType());
					if (loaded != null) {
						data.Blocks = loaded;
						LOGGER.debug("Loaded Blocks config");
					}
				}
				case "Crops" -> {
					Map<String, CropCategory> loaded = GSON.fromJson(json,
						new TypeToken<Map<String, CropCategory>>(){}.getType());
					if (loaded != null) {
						data.Crops = loaded;
						LOGGER.debug("Loaded Crops config");
					}
				}
				case "Entities" -> {
					Map<String, EntitiesCategory> loaded = GSON.fromJson(json,
						new TypeToken<Map<String, EntitiesCategory>>(){}.getType());
					if (loaded != null) {
						data.Entities = loaded;
						LOGGER.debug("Loaded Entities config");
					}
				}
				case "Smelting" -> {
					Map<String, SmeltingCategory> loaded = GSON.fromJson(json,
						new TypeToken<Map<String, SmeltingCategory>>(){}.getType());
					if (loaded != null) {
						data.Smelting = loaded;
						LOGGER.debug("Loaded Smelting config");
					}
				}
				case "Trading" -> {
					Map<String, TradingCategory> loaded = GSON.fromJson(json,
						new TypeToken<Map<String, TradingCategory>>(){}.getType());
					if (loaded != null) {
						data.Trading = loaded;
						LOGGER.debug("Loaded Trading config");
					}
				}
				case "Breeding" -> {
					Map<String, BreedingCategory> loaded = GSON.fromJson(json,
						new TypeToken<Map<String, BreedingCategory>>(){}.getType());
					if (loaded != null) {
						data.Breeding = loaded;
						LOGGER.debug("Loaded Breeding config");
					}
				}
				case "Fishing" -> {
					Map<String, FishingCategory> loaded = GSON.fromJson(json,
						new TypeToken<Map<String, FishingCategory>>(){}.getType());
					if (loaded != null) {
						data.Fishing = loaded;
						LOGGER.debug("Loaded Fishing config");
					}
				}
				case "Grindstone" -> {
					Map<String, GrindstoneCategory> loaded = GSON.fromJson(json,
						new TypeToken<Map<String, GrindstoneCategory>>(){}.getType());
					if (loaded != null) {
						data.Grindstone = loaded;
						LOGGER.debug("Loaded Grindstone config");
					}
				}
			}
		} catch (JsonSyntaxException e) {
			LOGGER.error("Invalid JSON in {} config file", categoryName, e);
		} catch (IOException e) {
			LOGGER.error("Failed to load {} config", categoryName, e);
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
							if (type != null) {
								entityIndex.put(Registries.ENTITY_TYPE.getRawId(type), entry.getValue());
							} else {
								LOGGER.warn("Unknown entity in Entities config: {}", key);
							}
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
						if (block != null) {
							blockIndex.put(Registries.BLOCK.getRawId(block), entry.getValue());
						} else {
							LOGGER.warn("Unknown block in Blocks config: {}", entry.getKey());
						}
					}
				}
			}
		}

		if (data.Crops != null) {
			for (CropCategory category : data.Crops.values()) {
				if (category.crops != null) {
					for (Map.Entry<String, XpValue> entry : category.crops.entrySet()) {
						Block block = Registries.BLOCK.get(Identifier.tryParse(entry.getKey()));
						if (block != null) {
							cropIndex.put(Registries.BLOCK.getRawId(block), entry.getValue());
						} else {
							LOGGER.warn("Unknown block in Crops config: {}", entry.getKey());
						}
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
							VillagerProfession profession = Registries.VILLAGER_PROFESSION.get(Identifier.tryParse(key));
							if (profession != null) {
								tradingIndex.put(Registries.VILLAGER_PROFESSION.getRawId(profession), entry.getValue());
							} else {
								LOGGER.warn("Unknown villager profession in Trading config: {}", key);
							}
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
						if (type != null) {
							breedingIndex.put(Registries.ENTITY_TYPE.getRawId(type), entry.getValue());
						} else {
							LOGGER.warn("Unknown entity in Breeding config: {}", entry.getKey());
						}
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
							Item item = Registries.ITEM.get(Identifier.tryParse(key));
							if (item != null) {
								fishingIndex.put(Registries.ITEM.getRawId(item), entry.getValue());
							} else {
								LOGGER.warn("Unknown item in Fishing config: {}", key);
							}
						}
					}
				}
			}
		}

		if (data.Grindstone != null) {
			for (GrindstoneCategory category : data.Grindstone.values()) {
				if (category.enchantments != null) {
					for (Map.Entry<String, XpValue> entry : category.enchantments.entrySet()) {
						Identifier id = Identifier.tryParse(entry.getKey());
						if (id != null) {
							grindstoneIndex.put(id, entry.getValue());
						} else {
							LOGGER.warn("Unknown enchantment in Grindstone config: {}", entry.getKey());
						}
					}
				}
			}
		}
	}
	
	private static void updateFlags() {
		durabilityPerLevelFlag = data.durabilityPerLevel;
		maxAnvilRepairCostFlag = data.maxAnvilRepairCost;
		xpRepairEnabledFlag = data.xpRepairEnabled;

		// Parse enum flags
		orbModeEnum = OrbMode.from(data.orbMode);
		entityXpModeEnum = XpMode.from(data.entityXpMode);
		blockXpModeEnum = XpMode.from(data.blockXpMode);
		cropXpModeEnum = XpMode.from(data.cropXpMode);
		breedingXpModeEnum = XpMode.from(data.breedingXpMode);
		fishingXpModeEnum = XpMode.from(data.fishingXpMode);
		tradingPlayerXpModeEnum = XpMode.from(data.tradingPlayerXp);
		merchantXpModeEnum = XpMode.from(data.merchantXp);
		grindstoneXpModeEnum = XpMode.from(data.grindstoneXpMode);
		smeltingXpModeEnum = XpMode.from(data.smeltingXpMode);
	}

	// Config is static after load()
	// save() kept for future use
	public static void save() {
		lock.writeLock().lock();
		try {
			saveInternal();
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

			// Save main config
			ConfigData mainConfig = new ConfigData();
			copyConfigFlags(data, mainConfig);
			
			String mainJson = GSON.toJson(mainConfig);
			Files.writeString(MAIN_CONFIG_PATH, mainJson);

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
			if (data.Breeding !=null) {
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

			LOGGER.debug("Saved all config files");
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
		}
	}


	public static boolean isXpRepairEnabled() {
		return xpRepairEnabledFlag;
	}

	public static int getMaxAnvilRepairCost() {
		return maxAnvilRepairCostFlag;
	}

	public static int getDurabilityPerLevel() {
		return durabilityPerLevelFlag;
	}


	// Enum getters
	public static OrbMode getOrbModeEnum() {
		return orbModeEnum;
	}
	public static XpMode getEntityXpModeEnum() {
		return entityXpModeEnum;
	}
	public static XpMode getBlockXpModeEnum() {
		return blockXpModeEnum;
	}
	public static XpMode getCropXpModeEnum() {
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




	public static float getRecipeXp(String productId) {
		if (productId == null) {
			return -1.0f;
		}

		// immutable map
		Float value = recipeIndex.get(productId);
		return value != null ? value : -1.0f;
	}

	// Get block XP with max age check (for crops)
	public static Integer getBlockXp(int rawId, boolean isMaxAge) {
		// First check crops with isMaxAge flag
		XpValue cropValue = cropIndex.get(rawId);
		if (cropValue != null) {
			if (!isMaxAge) {
				return 0;
			}
			return cropValue.calculateXp();
		}

		// Then check regular blocks (no maturity requirement)
		XpValue blockValue = blockIndex.get(rawId);
		if (blockValue != null) {
			return blockValue.calculateXp();
		}

		return null;
	}

	// Overload for blocks without max age check
	public static Integer getBlockXp(int rawId) {
		return getBlockXp(rawId, false);
	}

	// Get entity XP — null if not in config, negative values valid
	public static Integer getEntityXp(int rawId) {
		XpValue xpValue = entityIndex.get(rawId);
		return xpValue != null ? xpValue.calculateXp() : null;
	}

	// Get dragon XP — null if not in config
	public static Integer getDragonXp(boolean respawned) {
		XpValue xpValue = respawned ? dragonRespawnedXp : dragonFirstXp;
		return xpValue != null ? xpValue.calculateXp() : null;
	}

	// Get player trade XP — null if not in config
	public static Integer getPlayerXp(int rawId) {
		TradingXpValues xpValues = tradingIndex.get(rawId);
		return xpValues != null ? xpValues.playerXp.calculateXp() : null;
	}

	// Get merchant XP — null if not in config
	public static Integer getMerchantXp(int rawId) {
		TradingXpValues xpValues = tradingIndex.get(rawId);
		return xpValues != null ? xpValues.merchantXp.calculateXp() : null;
	}

	public static int getWanderingTraderRawId() {
		return wanderingTraderRawId;
	}

	// Get breeding XP — null if not in config
	public static Integer getBreedingXp(int rawId) {
		XpValue xpValue = breedingIndex.get(rawId);
		return xpValue != null ? xpValue.calculateXp() : null;
	}

	// Get fishing XP by item raw ID — null if not in config
	public static Integer getFishingXp(int rawId) {
		XpValue xpValue = fishingIndex.get(rawId);
		return xpValue != null ? xpValue.calculateXp() : null;
	}

	// Get global fishing XP (fallback for DEFAULT mode) — null if not in config
	public static Integer getGlobalFishingXp() {
		return globalFishingXp != null ? globalFishingXp.calculateXp() : null;
	}

	// Get grindstone XP — null if not in config
	public static Integer getGrindstoneXp(Identifier id) {
		XpValue xpValue = grindstoneIndex.get(id);
		return xpValue != null ? xpValue.calculateXp() : null;
	}

	public static void setBlockXp(String category, String blockId, int min, int max) {
		if (category == null || blockId == null) {
			throw new IllegalArgumentException("Category and block ID cannot be null");
		}
		lock.writeLock().lock();
		try {
			if (data.Blocks == null) {
				data.Blocks = new LinkedHashMap<>();
			}
			BlockCategory blockCategory = data.Blocks.computeIfAbsent(category, k -> new BlockCategory());
			if (blockCategory.blocks == null) {
				blockCategory.blocks = new LinkedHashMap<>();
			}
			XpValue xpValue = min == max ? new XpValue(min) : new XpValue(min, max);
			blockCategory.blocks.put(blockId, xpValue);
			Block block = Registries.BLOCK.get(Identifier.tryParse(blockId));
			blockIndex.put(Registries.BLOCK.getRawId(block), xpValue);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void setCropXp(String category, String blockId, int min, int max) {
		if (category == null || blockId == null) {
			throw new IllegalArgumentException("Category and Crop ID cannot be null");
		}
		lock.writeLock().lock();
		try {
			if (data.Crops == null) {
				data.Crops = new LinkedHashMap<>();
			}
			CropCategory cropCategory = data.Crops.computeIfAbsent(category, k -> new CropCategory());
			if (cropCategory.crops == null) {
				cropCategory.crops = new LinkedHashMap<>();
			}
			XpValue xpValue = min == max ? new XpValue(min) : new XpValue(min, max);
			cropCategory.crops.put(blockId, xpValue);
			Block block = Registries.BLOCK.get(Identifier.tryParse(blockId));
			cropIndex.put(Registries.BLOCK.getRawId(block), xpValue);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void setEntityXp(String category, String entityId, int min, int max) {
		if (category == null || entityId == null) {
			throw new IllegalArgumentException("Category and entity ID cannot be null");
		}
		lock.writeLock().lock();
		try {
			if (data.Entities == null) {
				data.Entities = new LinkedHashMap<>();
			}
			EntitiesCategory entitiesCategory = data.Entities.computeIfAbsent(category, k -> new EntitiesCategory());
			if (entitiesCategory.entities == null) {
				entitiesCategory.entities = new LinkedHashMap<>();
			}
			XpValue xpValue = min == max ? new XpValue(min) : new XpValue(min, max);
			entitiesCategory.entities.put(entityId, xpValue);
			if (entityId.equals("minecraft:ender_dragon_first")) {
				dragonFirstXp = xpValue;
			} else if (entityId.equals("minecraft:ender_dragon_respawned")) {
				dragonRespawnedXp = xpValue;
			} else {
				EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.tryParse(entityId));
				entityIndex.put(Registries.ENTITY_TYPE.getRawId(type), xpValue);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void setBreedingXp(String category, String entityId, int min, int max) {
		if (category == null || entityId == null) {
			throw new IllegalArgumentException("Category and entity ID cannot be null");
		}
		lock.writeLock().lock();
		try {
			if (data.Breeding == null) {
				data.Breeding = new LinkedHashMap<>();
			}
			BreedingCategory breedingCategory = data.Breeding.computeIfAbsent(category, k -> new BreedingCategory());
			if (breedingCategory.breeding == null) {
				breedingCategory.breeding = new LinkedHashMap<>();
			}
			XpValue xpValue = min == max ? new XpValue(min) : new XpValue(min, max);
			breedingCategory.breeding.put(entityId, xpValue);
			EntityType<?> breedType = Registries.ENTITY_TYPE.get(Identifier.tryParse(entityId));
			breedingIndex.put(Registries.ENTITY_TYPE.getRawId(breedType), xpValue);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void setRecipeXp(String category, String productId, float xp) {
		if (category == null || productId == null) {
			throw new IllegalArgumentException("Category and product ID cannot be null");
		}

		lock.writeLock().lock();
		try {
			if (data.Smelting == null) {
				data.Smelting = new LinkedHashMap<>();
			}

			SmeltingCategory smeltingCategory = data.Smelting.computeIfAbsent(
				category,
				k -> new SmeltingCategory());
			
			if (smeltingCategory.smelting == null) {
				smeltingCategory.smelting = new LinkedHashMap<>();
			}
			smeltingCategory.smelting.put(productId, xp);
			recipeIndex.put(productId, xp);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void setTradingXp(String category, String merchantType, int playerMin, int playerMax, int merchantMin, int merchantMax) {
		if (category == null || merchantType == null) {
			throw new IllegalArgumentException("Category and merchant type cannot be null");
		}
		lock.writeLock().lock();
		try {
			if (data.Trading == null) {
			data.Trading = new LinkedHashMap<>();
			}

			TradingCategory tradingCategory = data.Trading.computeIfAbsent(category, k -> new TradingCategory());
			if (tradingCategory.trader == null) {
				tradingCategory.trader = new LinkedHashMap<>();
			}

			TradingXpValues tradingData = new TradingXpValues(playerMin, playerMax, merchantMin, merchantMax);
			tradingCategory.trader.put(merchantType, tradingData);
			if (merchantType.equals("minecraft:wandering_trader")) {
				tradingIndex.put(wanderingTraderRawId, tradingData);
			} else {
				VillagerProfession prof = Registries.VILLAGER_PROFESSION.get(Identifier.tryParse(merchantType));
				tradingIndex.put(Registries.VILLAGER_PROFESSION.getRawId(prof), tradingData);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void setFishingXp(String category, String catchId, int min, int max) {
		if (category == null || catchId == null) {
			throw new IllegalArgumentException("Category and catch ID cannot be null");
		}
		lock.writeLock().lock();
		try {
			if (data.Fishing == null) {
				data.Fishing = new LinkedHashMap<>();
			}
			FishingCategory fishingCategory = data.Fishing.computeIfAbsent(category, k -> new FishingCategory());
			if (fishingCategory.fishing == null) {
				fishingCategory.fishing = new LinkedHashMap<>();
			}
			XpValue xpValue = min == max ? new XpValue(min) : new XpValue(min, max);
			fishingCategory.fishing.put(catchId, xpValue);
			if (catchId.equals("Global_Fishing")) {
				globalFishingXp = xpValue;
			} else {
				Item fishItem = Registries.ITEM.get(Identifier.tryParse(catchId));
				fishingIndex.put(Registries.ITEM.getRawId(fishItem), xpValue);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}


	private static void copyConfigFlags(ConfigData source, ConfigData target) {
		target.durabilityPerLevel = source.durabilityPerLevel;
		target.maxAnvilRepairCost = source.maxAnvilRepairCost;
		target.xpRepairEnabled = source.xpRepairEnabled;
		target.blockXpMode = source.blockXpMode;
		target.cropXpMode = source.cropXpMode;
		target.entityXpMode = source.entityXpMode;
		target.orbMode = source.orbMode;
		target.tradingPlayerXp = source.tradingPlayerXp;
		target.merchantXp = source.merchantXp;
		target.breedingXpMode = source.breedingXpMode;
		target.fishingXpMode = source.fishingXpMode;
		target.grindstoneXpMode = source.grindstoneXpMode;
		target.smeltingXpMode = source.smeltingXpMode;

	}

	public static class ConfigData {
		public String ConfigVersion = "4.0";
		public String orbMode = "vanilla";
		public String blockXpMode = "vanilla";
		public String cropXpMode = "off";
		public String entityXpMode = "vanilla";
		public String breedingXpMode = "vanilla";
		public String fishingXpMode = "vanilla";
		public String grindstoneXpMode = "vanilla";
		public String smeltingXpMode = "vanilla";
		public String tradingPlayerXp = "vanilla";
		public String merchantXp = "vanilla";
		public boolean xpRepairEnabled = true;
		public int durabilityPerLevel = 100;
		public int maxAnvilRepairCost = 40;


		// Category maps - each category has a name and a map of entries (blocks/entities/etc)
		public String _MAPS = " - DO NOT EDIT THIS SECTION - use the specific JSON files instead - ";
		public Map<String, SmeltingCategory> Smelting = new LinkedHashMap<>();
		public Map<String, BlockCategory> Blocks = new LinkedHashMap<>();
		public Map<String, EntitiesCategory> Entities = new LinkedHashMap<>();
		public Map<String, CropCategory> Crops = new LinkedHashMap<>();
		public Map<String, TradingCategory> Trading = new LinkedHashMap<>();
		public Map<String, BreedingCategory> Breeding = new LinkedHashMap<>();
		public Map<String, FishingCategory> Fishing = new LinkedHashMap<>();
		public Map<String, GrindstoneCategory> Grindstone = new LinkedHashMap<>();

		public ConfigData() {
			initializeMaps();
		}

		private void initializeMaps() {
			if (Blocks == null) {
				Blocks = new LinkedHashMap<>();
			}
			if (Crops == null) {
				Crops = new LinkedHashMap<>();
			}
			if (Entities == null) {
				Entities = new LinkedHashMap<>();
			}
			if (Smelting == null) {
				Smelting = new LinkedHashMap<>();
			}
			if (Trading == null) {
				Trading = new LinkedHashMap<>();
			}
			if (Breeding == null) {
				Breeding = new LinkedHashMap<>();
			}
			if (Fishing == null) {
				Fishing = new LinkedHashMap<>();
			}
			if (Grindstone == null) {
				Grindstone = new LinkedHashMap<>();
			}
		}

		public void initializeDefaults() {
			initializeMaps();
			addDefaultCategories();
		}

		private void addDefaultCategories() {
			if (Entities == null || Entities.isEmpty()) {
			// Entity items category
			EntitiesCategory xpBottle = new EntitiesCategory();
			xpBottle.entities = new LinkedHashMap<>();
			xpBottle.entities.put("minecraft:experience_bottle", new XpValue(3, 11));
			Entities.put("Items", xpBottle);

			EntitiesCategory bossEntity = new EntitiesCategory();
			bossEntity.entities = new LinkedHashMap<>();
			bossEntity.entities.put("minecraft:ender_dragon_first", new XpValue(12000));
			bossEntity.entities.put("minecraft:ender_dragon_respawned", new XpValue(500));
			bossEntity.entities.put("minecraft:wither", new XpValue(50));
			bossEntity.entities.put("minecraft:warden", new XpValue(50));
			Entities.put("Bosses", bossEntity);

			
			// Hostile Mobs 
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

			// Passive Mobs
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
			}

			if (Blocks == null || Blocks.isEmpty()) {
			BlockCategory spawners = new BlockCategory();
			spawners.blocks = new LinkedHashMap<>();
			spawners.blocks.put("minecraft:spawner", new XpValue(15, 43));
			Blocks.put("Spawners", spawners);


			// Ores 
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


			// Wood
			BlockCategory wood = new BlockCategory();
			wood.blocks = new LinkedHashMap<>();
			Blocks.put("Wood", wood);

			// Common blocks 
			BlockCategory common = new BlockCategory();
			common.blocks = new LinkedHashMap<>();
			Blocks.put("Common Blocks", common);

			// Nether 
			BlockCategory nether = new BlockCategory();
			nether.blocks = new LinkedHashMap<>();
			Blocks.put("Nether", nether);

			// End 
			BlockCategory end = new BlockCategory();
			end.blocks = new LinkedHashMap<>();
			Blocks.put("End", end);

			// Sculk
			BlockCategory sculk = new BlockCategory();
			sculk.blocks = new LinkedHashMap<>();
			sculk.blocks.put("minecraft:sculk", new XpValue(1));
			sculk.blocks.put("minecraft:sculk_sensor", new XpValue(5));
			sculk.blocks.put("minecraft:sculk_shrieker", new XpValue(5));
			sculk.blocks.put("minecraft:sculk_catalyst", new XpValue(5));
			Blocks.put("Sculk", sculk);
			}


			
			// crops
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
			}

			
			// smelting
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
			}

			if (Grindstone == null || Grindstone.isEmpty()) {
			GrindstoneCategory meele = new GrindstoneCategory();
			meele.enchantments = new LinkedHashMap<>();
			// Swords / axes
			meele.enchantments.put("minecraft:sharpness", new XpValue(1, 4));
			meele.enchantments.put("minecraft:smite", new XpValue(1, 4));
			meele.enchantments.put("minecraft:bane_of_arthropods", new XpValue(1, 4));
			meele.enchantments.put("minecraft:looting", new XpValue(1, 4));
			meele.enchantments.put("minecraft:fire_aspect", new XpValue(1, 4));
			meele.enchantments.put("minecraft:knockback", new XpValue(1, 4));
			meele.enchantments.put("minecraft:sweeping_edge", new XpValue(1, 4));
			Grindstone.put("Melee", meele);


			// Tools
			GrindstoneCategory tools = new GrindstoneCategory();
			tools.enchantments = new LinkedHashMap<>();
			tools.enchantments.put("minecraft:efficiency", new XpValue(1, 4));
			tools.enchantments.put("minecraft:fortune", new XpValue(1, 4));
			tools.enchantments.put("minecraft:silk_touch", new XpValue(1, 4));
			tools.enchantments.put("minecraft:unbreaking", new XpValue(1, 4));
			tools.enchantments.put("minecraft:mending", new XpValue(5, 15));
			Grindstone.put("Tools", tools);


			// Armor
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


			// Bow
			GrindstoneCategory bow = new GrindstoneCategory();
			bow.enchantments = new LinkedHashMap<>();
			bow.enchantments.put("minecraft:power", new XpValue(1, 4));
			bow.enchantments.put("minecraft:punch", new XpValue(1, 4));
			bow.enchantments.put("minecraft:flame", new XpValue(1, 4));
			bow.enchantments.put("minecraft:infinity", new XpValue(5, 10));
			Grindstone.put("Bow", bow);


			// Fishing rod
			GrindstoneCategory fishingRod = new GrindstoneCategory();
			fishingRod.enchantments = new LinkedHashMap<>();
			fishingRod.enchantments.put("minecraft:luck_of_the_sea", new XpValue(1, 4));
			fishingRod.enchantments.put("minecraft:lure", new XpValue(1, 4));
			Grindstone.put("Fishing", fishingRod);
			}

		}
	}

	public static class FishingCategory {
		public Map<String, XpValue> fishing = new LinkedHashMap<>();

		public FishingCategory() {
			if (fishing == null) {
				fishing = new LinkedHashMap<>();
			}
		}
	}

	public static class GrindstoneCategory {
		public Map<String, XpValue> enchantments = new LinkedHashMap<>();

		public GrindstoneCategory() {
			if (enchantments == null) {
				enchantments = new LinkedHashMap<>();
			}
		}
	}

	public static class BreedingCategory {
		public Map<String, XpValue> breeding = new LinkedHashMap<>();

		public BreedingCategory() {
			if (breeding == null) {
				breeding = new LinkedHashMap<>();
			}
		}
	}

	public static class TradingCategory {
		public Map<String, TradingXpValues> trader = new LinkedHashMap<>();

		public TradingCategory() {
			if (trader == null) {
				trader = new LinkedHashMap<>();
			}
		}
	}

	// Smelting
	public static class SmeltingCategory {
		public Map<String, Float> smelting = new LinkedHashMap<>();

		public SmeltingCategory() {
			if (smelting == null) {
				smelting = new LinkedHashMap<>();
			}
		}
	}

	// Entities 
	public static class EntitiesCategory {
		public Map<String, XpValue> entities = new LinkedHashMap<>();

		public EntitiesCategory() {
			if (entities == null) {
				entities = new LinkedHashMap<>();
			}
		}
	}

	// Regular blocks (no age requirement)
	public static class BlockCategory {
		public Map<String, XpValue> blocks = new LinkedHashMap<>();

		public BlockCategory() {
			if (blocks == null) {
				blocks = new LinkedHashMap<>();
			}
		}
	}

	// Crops (checked for maturity)
	public static class CropCategory {
		public Map<String, XpValue> crops = new LinkedHashMap<>();

		public CropCategory() {
			if (crops == null) {
				crops = new LinkedHashMap<>();
			}
		}
	}

	public static class TradingXpValues {
		public XpValue playerXp;
		public XpValue merchantXp;

		public TradingXpValues() {
			this.playerXp = new XpValue();
			this.merchantXp = new XpValue();
		}

		public TradingXpValues(int playerMin, int playerMax, int merchantMin, int merchantMax) {
		this.playerXp = playerMin == playerMax ? new XpValue(playerMin) : new XpValue(playerMin, playerMax);
		this.merchantXp = merchantMin == merchantMax ? new XpValue(merchantMin) : new XpValue(merchantMin, merchantMax);
		}
	}

	public static class XpValue {
		public String type;
		public Integer min;
		public Integer max;
		public Integer fixed;

		// Default constructor for GSON
		public XpValue() {
			this.type = "Fixed";
			this.fixed = 0;
		}

		public XpValue(int min, int max) {
			this.type = "Random";
			this.min = Math.min(min, max);
			this.max = Math.max(min, max);
		}

		public XpValue(int fixed) {
			this.type = "Fixed";
			this.fixed = fixed;
		}

		public int calculateXp() {
			if (type == null || type.isEmpty()) {
				type = "Fixed";
			}

			return switch (type) {
				case "Fixed" -> fixed == null ? 0 : fixed.intValue();
				case "Random" -> {
					if (min == null || max == null) {
						yield 0;
					}
					if (min.equals(max)) {
						yield min;
					}
					yield min + RANDOM.nextInt(max - min + 1);
				}
				default -> {
					LOGGER.warn("Unknown XP value type: {}, defaulting to 0", type);
					yield 0;
				}
			};
		}
	}
}