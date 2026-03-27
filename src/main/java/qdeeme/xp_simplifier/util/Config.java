package qdeeme.xp_simplifier.util;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
	private static final Random RANDOM = new Random();
	private static final ReadWriteLock lock = new ReentrantReadWriteLock();

	private static ConfigData data = new ConfigData();
	
	// Index maps for O(1) lookups
	private static final Map<String, XpValue> entityIndex = new LinkedHashMap<>();
	private static final Map<String, XpValue> blockIndex = new LinkedHashMap<>();
	private static final Map<String, XpValue> cropIndex = new LinkedHashMap<>();
	private static final Map<String, Float> recipeIndex = new LinkedHashMap<>();
	private static final Map<String, TradingXpValues> tradingIndex = new LinkedHashMap<>();
	
	// Flags for quick checks
	private static int durabilityPerLevelFlag;
	private static int maxAnvilRepairCostFlag;
	private static boolean xpRepairEnabledFlag;
	private static boolean blockBreakXpEnabledFlag;
	private static boolean cropXpEnabledFlag;
	private static boolean entityKillXpEnabledFlag;
	private static boolean xpSmeltingEnabledFlag;
	private static boolean tradingXpEnabledFlag;
	private static boolean orbsEnabledFlag;
	private static String tradingPlayerXpModeFlag;
	private static String merchantXpModeFlag;

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

			// Check if files are empty and need defaults
			if ((data.Blocks == null || data.Blocks.isEmpty()) &&
			    (data.Crops == null || data.Crops.isEmpty()) &&
			    (data.Entities == null || data.Entities.isEmpty()) &&
			    (data.Smelting == null || data.Smelting.isEmpty()) &&
				(data.Trading == null || data.Trading.isEmpty())) {
				LOGGER.info("Config files empty or not found, creating default config");
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
		
		// Index all entities
		if (data.Entities != null) {
			for (EntitiesCategory category : data.Entities.values()) {
				if (category.entities != null) {
					entityIndex.putAll(category.entities);
				}
			}
		}
		
		// Index all blocks
		if (data.Blocks != null) {
			for (BlockCategory category : data.Blocks.values()) {
				if (category.blocks != null) {
					blockIndex.putAll(category.blocks);
				}
			}
		}
		
		// Index all crops
		if (data.Crops != null) {
			for (CropCategory category : data.Crops.values()) {
				if (category.crops != null) {
					cropIndex.putAll(category.crops);
				}
			}
		}
		
		// Index all recipes
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
					tradingIndex.putAll(category.trader);
				}
			}
		}
	}
	
	private static void updateFlags() {
		durabilityPerLevelFlag = data.durabilityPerLevel;
		maxAnvilRepairCostFlag = data.maxAnvilRepairCost;
		xpRepairEnabledFlag = data.xpRepairEnabled;
		blockBreakXpEnabledFlag = data.blockBreakXpEnabled;
		cropXpEnabledFlag = data.cropXpEnabled;
		entityKillXpEnabledFlag = data.entityKillXpEnabled;
		xpSmeltingEnabledFlag = data.xpSmeltingEnabled;
		tradingXpEnabledFlag = data.tradingXpEnabled;
		orbsEnabledFlag = data.orbsEnabled;
		tradingPlayerXpModeFlag = data.tradingPlayerXp;
		merchantXpModeFlag = data.merchantXp;
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

			LOGGER.debug("Saved all config files");
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
		}
	}

	public static boolean isSmeltingXpEnabled() {
		return xpSmeltingEnabledFlag;
	}

	public static boolean isXpRepairEnabled() {
		return xpRepairEnabledFlag;
	}

	public static int getMaxAnvilRepairCost() {
		return maxAnvilRepairCostFlag;
	}

	public static boolean isBlockBreakXpEnabled() {
		return blockBreakXpEnabledFlag;
	}

	public static boolean isCropXpEnabled() {
		return cropXpEnabledFlag;
	}

	public static boolean isEntityKillXpEnabled() {
		return entityKillXpEnabledFlag;
	}

	public static boolean isTradingXpEnabled() {
		return tradingXpEnabledFlag;
	}

	public static boolean isOrbsEnabled() {
		return orbsEnabledFlag;
	}

	public static String getTradingPlayerXpMode() {
		return tradingPlayerXpModeFlag;
	}

	public static String getMerchantXpMode() {
		return merchantXpModeFlag;
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
	public static int getBlockXp(String blockId, boolean isMaxAge) {
		if (blockId == null) {
			return -1;
		}

		// First check crops with isMaxAge flag
		XpValue cropValue = cropIndex.get(blockId);
		if (cropValue != null) {
			// Only give XP if crop is at max age
			if (!isMaxAge) {
				return 0;
			}
			return cropValue.calculateXp();
		}
		
		// Then check regular blocks (no maturity requirement)
		XpValue blockValue = blockIndex.get(blockId);
		if (blockValue != null) {
			return blockValue.calculateXp();
		}

		return -1;
	}

	// Overload for blocks without max age check
	public static int getBlockXp(String blockId) {
		return getBlockXp(blockId, false);
	}

	// Get entity XP - value or -1 if not found
	public static int getEntityXp(String entityId) {
		if (entityId == null) {
			return -1; // Not in config
		}

		// Immutable map
		XpValue xpValue = entityIndex.get(entityId);
		return xpValue != null ? xpValue.calculateXp() : -1;
	}

	public static int getPlayerXp(String merchantType) {
		if (merchantType == null) {
			return -1;
		}

		// Immutable map
		TradingXpValues xpValues = tradingIndex.get(merchantType);
		return xpValues != null ? xpValues.playerXp.calculateXp() : -1;
	}

	public static int getMerchantXp(String merchantType) {
		if (merchantType == null) {
			return -1;
		}

		// Immutable map
		TradingXpValues xpValues = tradingIndex.get(merchantType);
		return xpValues != null ? xpValues.merchantXp.calculateXp() : -1;
	}


	// Check if entity exists in config
	public static boolean hasEntityXpConfig(String entityId) {
		if (entityId == null) {
			return false;
		}

		// Immutable map
		return entityIndex.containsKey(entityId);
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
			blockIndex.put(blockId, xpValue);
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
			cropIndex.put(blockId, xpValue);
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
			entityIndex.put(entityId, xpValue);
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
			tradingIndex.put(merchantType, tradingData);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static int getDurabilityPerLevel() {
		return durabilityPerLevelFlag;
	}

	// Helper methods to reduce duplication

	/**
	 * Copies all config flags from source to target ConfigData
	 */
	private static void copyConfigFlags(ConfigData source, ConfigData target) {
		target.durabilityPerLevel = source.durabilityPerLevel;
		target.maxAnvilRepairCost = source.maxAnvilRepairCost;
		target.xpRepairEnabled = source.xpRepairEnabled;
		target.blockBreakXpEnabled = source.blockBreakXpEnabled;
		target.cropXpEnabled = source.cropXpEnabled;
		target.entityKillXpEnabled = source.entityKillXpEnabled;
		target.xpSmeltingEnabled = source.xpSmeltingEnabled;
		target.tradingXpEnabled = source.tradingXpEnabled;
		target.orbsEnabled = source.orbsEnabled;
		target.tradingPlayerXp = source.tradingPlayerXp;
		target.merchantXp = source.merchantXp;
	}

	public static class ConfigData {
		public String ConfigVersion = "3.0";
		public boolean orbsEnabled = false;
		public boolean blockBreakXpEnabled = true;
		public boolean cropXpEnabled = true;
		public boolean entityKillXpEnabled = true;
		public boolean xpSmeltingEnabled = true;
		public boolean tradingXpEnabled = true;
		public String tradingPlayerXp = "on";
		public String merchantXp = "off";
		public boolean xpRepairEnabled = true;
		public int durabilityPerLevel = 100;
		public int maxAnvilRepairCost = 40;

		// Category maps - each category has a name and a map of entries (blocks, entities, or recipes)
		public String _MAPS = " - DO NOT EDIT THIS SECTION - use the specific JSON files instead - ";
		public Map<String, SmeltingCategory> Smelting = new LinkedHashMap<>();
		public Map<String, BlockCategory> Blocks = new LinkedHashMap<>();
		public Map<String, EntitiesCategory> Entities = new LinkedHashMap<>();
		public Map<String, CropCategory> Crops = new LinkedHashMap<>();
		public Map<String, TradingCategory> Trading = new LinkedHashMap<>();

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
		}

		public void initializeDefaults() {
			initializeMaps();
			addDefaultCategories();
		}

		private void addDefaultCategories() {
			// Entity items category
			EntitiesCategory xpBottle = new EntitiesCategory();
			xpBottle.entities = new LinkedHashMap<>();
			xpBottle.entities.put("minecraft:experience_bottle", new XpValue(3, 11));
			Entities.put("Items", xpBottle);

			
			// Hostile Mobs 
			EntitiesCategory hostileMobs = new EntitiesCategory();
			hostileMobs.entities = new LinkedHashMap<>();
			hostileMobs.entities.put("minecraft:ender_dragon", new XpValue(2000, 3500));
			hostileMobs.entities.put("minecraft:wither", new XpValue(50, 150));
			hostileMobs.entities.put("minecraft:ghast", new XpValue(5, 10));
			hostileMobs.entities.put("minecraft:magma_cube", new XpValue(2, 5));
			hostileMobs.entities.put("minecraft:blaze", new XpValue(6, 12));
			hostileMobs.entities.put("minecraft:zombified_piglin", new XpValue(3, 8));
			hostileMobs.entities.put("minecraft:stray", new XpValue(3, 8));
			hostileMobs.entities.put("minecraft:husk", new XpValue(3, 8));
			hostileMobs.entities.put("minecraft:zombie", new XpValue(3, 8));
			hostileMobs.entities.put("minecraft:skeleton", new XpValue(3, 8));
			hostileMobs.entities.put("minecraft:creeper", new XpValue(3, 8));
			hostileMobs.entities.put("minecraft:enderman", new XpValue(5, 12));
			hostileMobs.entities.put("minecraft:spider", new XpValue(3, 7));
			hostileMobs.entities.put("minecraft:guardian", new XpValue(10, 15));
			hostileMobs.entities.put("minecraft:elder_guardian", new XpValue(50, 75));
			hostileMobs.entities.put("minecraft:shulker", new XpValue(5, 15));
			hostileMobs.entities.put("minecraft:slime", new XpValue(1, 3));
			hostileMobs.entities.put("minecraft:witch", new XpValue(5, 10));
			Entities.put("Hostile Mobs", hostileMobs);

			// Passive Mobs
			EntitiesCategory passiveMobs = new EntitiesCategory();
			passiveMobs.entities = new LinkedHashMap<>();
			passiveMobs.entities.put("minecraft:cow", new XpValue(1, 3));
			passiveMobs.entities.put("minecraft:sheep", new XpValue(1, 3));
			passiveMobs.entities.put("minecraft:pig", new XpValue(1, 3));
			passiveMobs.entities.put("minecraft:chicken", new XpValue(1, 3));
			passiveMobs.entities.put("minecraft:horse", new XpValue(5, 10));
			passiveMobs.entities.put("minecraft:donkey", new XpValue(5, 10));
			passiveMobs.entities.put("minecraft:mooshroom", new XpValue(1, 3));
			passiveMobs.entities.put("minecraft:rabbit", new XpValue(1, 3));
			passiveMobs.entities.put("minecraft:fox", new XpValue(2, 5));
			Entities.put("Passive Mobs", passiveMobs);

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
			ores.blocks.put("minecraft:redstone_ore", new XpValue(0, 2));
			ores.blocks.put("minecraft:deepslate_redstone_ore", new XpValue(0, 2));
			ores.blocks.put("minecraft:lapis_ore", new XpValue(0));
			ores.blocks.put("minecraft:deepslate_lapis_ore", new XpValue(0));
			ores.blocks.put("minecraft:diamond_ore", new XpValue(2, 6));
			ores.blocks.put("minecraft:deepslate_diamond_ore", new XpValue(2, 6));
			ores.blocks.put("minecraft:emerald_ore", new XpValue(1, 6));
			ores.blocks.put("minecraft:deepslate_emerald_ore", new XpValue(1, 6));
			ores.blocks.put("minecraft:nether_gold_ore", new XpValue(0, 3));
			ores.blocks.put("minecraft:nether_quartz_ore", new XpValue(0, 3));
			ores.blocks.put("minecraft:ancient_debris", new XpValue(2, 6));
			Blocks.put("Ores", ores);

			// Wood
			BlockCategory wood = new BlockCategory();
			wood.blocks = new LinkedHashMap<>();
			Blocks.put("Wood", wood);

			// Stone 
			BlockCategory stone = new BlockCategory();
			stone.blocks = new LinkedHashMap<>();
			Blocks.put("Stone", stone);

			// Nether 
			BlockCategory nether = new BlockCategory();
			nether.blocks = new LinkedHashMap<>();
			Blocks.put("Nether", nether);

			// End 
			BlockCategory end = new BlockCategory();
			end.blocks = new LinkedHashMap<>();
			Blocks.put("End", end);

			// Crops category (XP for mature crops)
			CropCategory crops = new CropCategory();
			crops.crops = new LinkedHashMap<>();
			crops.crops.put("minecraft:wheat", new XpValue(0));
			crops.crops.put("minecraft:carrots", new XpValue(0));
			crops.crops.put("minecraft:potatoes", new XpValue(0));
			crops.crops.put("minecraft:beetroots", new XpValue(0));
			crops.crops.put("minecraft:nether_wart", new XpValue(0));
			crops.crops.put("minecraft:cocoa", new XpValue(0));
			Crops.put("Minecraft Crops", crops);

			// Smelting/cooking xp per recipe based on product
			SmeltingCategory smeltingOres = new SmeltingCategory();
			smeltingOres.smelting = new LinkedHashMap<>();
			smeltingOres.smelting.put("minecraft:iron_ingot", 0.7f);
			smeltingOres.smelting.put("minecraft:copper_ingot", 0.7f);
			smeltingOres.smelting.put("minecraft:gold_ingot", 1.0f);
			smeltingOres.smelting.put("minecraft:redstone_dust", 0.3f);
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
					if (min == null || max == null || min < 0 || max < min) {
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