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
	private static final Path SMELTING_CONFIG_PATH = CONFIG_DIR.resolve("SmeltingXp.json");
	private static final Random RANDOM = new Random();
	private static final ReadWriteLock lock = new ReentrantReadWriteLock();

	private static ConfigData data = new ConfigData();

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
						data.durabilityPerLevel = mainConfig.durabilityPerLevel;
						data.maxAnvilRepairCost = mainConfig.maxAnvilRepairCost;
						data.xpRepairEnabled = mainConfig.xpRepairEnabled;
						data.blockBreakXpEnabled = mainConfig.blockBreakXpEnabled;
						data.entityKillXpEnabled = mainConfig.entityKillXpEnabled;
						data.xpSmeltingEnabled = mainConfig.xpSmeltingEnabled;
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
			loadCategoryFile(BLOCKS_CONFIG_PATH, "Blocks", data.Blocks);
			loadCategoryFile(CROPS_CONFIG_PATH, "Crops", null);
			loadCategoryFile(ENTITIES_CONFIG_PATH, "Entities", data.Entities);
			loadCategoryFile(SMELTING_CONFIG_PATH, "SmeltingXp", data.SmeltingXp);

			// Check if files are empty and need defaults
			if ((data.Blocks == null || data.Blocks.isEmpty()) &&
			    (data.Crops == null || data.Crops.isEmpty()) &&
			    (data.Entities == null || data.Entities.isEmpty()) &&
			    (data.SmeltingXp == null || data.SmeltingXp.isEmpty())) {
				LOGGER.info("Config files empty or not found, creating default config");
				data.initializeDefaults();
				needsSave = true;
			}

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

	private static void loadCategoryFile(Path path, String categoryName, Object target) {
		if (!Files.exists(path)) {
			return;
		}

		try {
			String json = Files.readString(path);
			if (categoryName.equals("Blocks")) {
				Map<String, BlockCategory> loaded = GSON.fromJson(json, 
					new TypeToken<Map<String, BlockCategory>>(){}.getType());
				if (loaded != null) {
					data.Blocks = loaded;
					LOGGER.debug("Loaded Blocks config");
				}
			} else if (categoryName.equals("Crops")) {
				Map<String, CropCategory> loaded = GSON.fromJson(json,
					new TypeToken<Map<String, CropCategory>>(){}.getType());
				if (loaded != null) {
					data.Crops = loaded;
					LOGGER.debug("Loaded Crops config");
				}
			} else if (categoryName.equals("Entities")) {
				Map<String, EntitiesCategory> loaded = GSON.fromJson(json,
					new TypeToken<Map<String, EntitiesCategory>>(){}.getType());
				if (loaded != null) {
					data.Entities = loaded;
					LOGGER.debug("Loaded Entities config");
				}
			} else if (categoryName.equals("SmeltingXp")) {
				Map<String, SmeltingCategory> loaded = GSON.fromJson(json,
					new TypeToken<Map<String, SmeltingCategory>>(){}.getType());
				if (loaded != null) {
					data.SmeltingXp = loaded;
					LOGGER.debug("Loaded SmeltingXp config");
				}
			}
		} catch (JsonSyntaxException e) {
			LOGGER.error("Invalid JSON in {} config file", categoryName, e);
		} catch (IOException e) {
			LOGGER.error("Failed to load {} config", categoryName, e);
		}
	}

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
			mainConfig.durabilityPerLevel = data.durabilityPerLevel;
			mainConfig.maxAnvilRepairCost = data.maxAnvilRepairCost;
			mainConfig.xpRepairEnabled = data.xpRepairEnabled;
			mainConfig.blockBreakXpEnabled = data.blockBreakXpEnabled;
			mainConfig.entityKillXpEnabled = data.entityKillXpEnabled;
			mainConfig.xpSmeltingEnabled = data.xpSmeltingEnabled;
			
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
			if (data.SmeltingXp != null) {
				String smeltingJson = GSON.toJson(data.SmeltingXp);
				Files.writeString(SMELTING_CONFIG_PATH, smeltingJson);
			}

			LOGGER.debug("Saved all config files");
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
		}
	}

	public static boolean isSmeltingXpEnabled() {
		lock.readLock().lock();
		try {
			return data.xpSmeltingEnabled;
		} finally {
			lock.readLock().unlock();
		}
	}

	public static boolean isXpRepairEnabled() {
		lock.readLock().lock();
		try {
			return data.xpRepairEnabled;
		} finally {
			lock.readLock().unlock();
		}
	}

	public static int getMaxAnvilRepairCost() {
		lock.readLock().lock();
		try {
			return data.maxAnvilRepairCost;
		} finally {
			lock.readLock().unlock();
		}
	}

	public static boolean isBlockBreakXpEnabled() {
		lock.readLock().lock();
		try {
			return data.blockBreakXpEnabled;
		} finally {
			lock.readLock().unlock();
		}
	}

	public static boolean isEntityKillXpEnabled() {
		lock.readLock().lock();
		try {
			return data.entityKillXpEnabled;
		} finally {
			lock.readLock().unlock();
		}
	}


	public static float getRecipeXp(String productId) {
		if (productId == null) {
			return -1.0f;
		}

		lock.readLock().lock();
		try {
			if (data.SmeltingXp != null) {
				for (SmeltingCategory category : data.SmeltingXp.values()) {
					if (category.smelting != null && category.smelting.containsKey(productId)) {
						return category.smelting.get(productId);
					}
				}
			}
			return -1.0f;
		} finally {
			lock.readLock().unlock();
		}
	}

	// Get block XP with age check (for crops)
	public static int getBlockXp(String blockId, int currentAge) {
		if (blockId == null) {
			return -1;
		}

		lock.readLock().lock();
		try {
			// First check crops (which have age requirements)
			if (data.Crops != null) {
				for (CropCategory category : data.Crops.values()) {
					if (category.crops != null && category.crops.containsKey(blockId)) {
						CropXpData cropData = category.crops.get(blockId);
						
						// Check if crop is mature enough
						if (currentAge < cropData.matureAge) {
							return 0;
						}
						
						return cropData.xp.calculateXp();
					}
				}
			}
			
			// Then check regular blocks (no age requirement)
			if (data.Blocks != null) {
				for (BlockCategory category : data.Blocks.values()) {
					if (category.blocks != null && category.blocks.containsKey(blockId)) {
						return category.blocks.get(blockId).calculateXp();
					}
				}
			}

			return -1;
		} finally {
			lock.readLock().unlock();
		}
	}

	// Overload for blocks without age
	public static int getBlockXp(String blockId) {
		return getBlockXp(blockId, -1);
	}

	// Get entity XP - returns config value if entity is in config, or -1 if not in config
	public static int getEntityXp(String entityId) {
		if (entityId == null) {
			return -1; // Not in config
		}

		lock.readLock().lock();
		try {
			if (data.Entities != null) {
				for (EntitiesCategory category : data.Entities.values()) {
					if (category.entities != null && category.entities.containsKey(entityId)) {
						XpValue xpValue = category.entities.get(entityId);
						return xpValue.calculateXp();
					}
				}
			}

			return -1; // Entity not found in config
		} finally {
			lock.readLock().unlock();
		}
	}


	// Check if entity exists in config
	public static boolean hasEntityXpConfig(String entityId) {
		if (entityId == null) {
			return false;
		}

		lock.readLock().lock();
		try {
			if (data.Entities != null) {
				for (EntitiesCategory category : data.Entities.values()) {
					if (category.entities != null && category.entities.containsKey(entityId)) {
						return true;
					}
				}
			}
			return false;
		} finally {
			lock.readLock().unlock();
		}
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
			
			BlockCategory blockCategory = data.Blocks.computeIfAbsent(
				category, 
				k -> new BlockCategory()
			);
			
			if (blockCategory.blocks == null) {
				blockCategory.blocks = new LinkedHashMap<>();
			}
			
			if (min == max) {
				blockCategory.blocks.put(blockId, new XpValue(min));
			} else {
				blockCategory.blocks.put(blockId, new XpValue(min, max));
			}
			save();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void setCropXp(String category, String blockId, int matureAge, int min, int max) {
		if (category == null || blockId == null) {
			throw new IllegalArgumentException("Category and Crop ID cannot be null");
		}

		lock.writeLock().lock();
		try {
			if (data.Crops == null) {
				data.Crops = new LinkedHashMap<>();
			}
			
			CropCategory cropCategory = data.Crops.computeIfAbsent(
				category, 
				k -> new CropCategory()
			);
			
			if (cropCategory.crops == null) {
				cropCategory.crops = new LinkedHashMap<>();
			}
			
			if (min == max) {
				cropCategory.crops.put(blockId, new CropXpData(matureAge, min));
			} else {
				cropCategory.crops.put(blockId, new CropXpData(matureAge, min, max));
			}
			save();
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
			
			EntitiesCategory entitiesCategory = data.Entities.computeIfAbsent(
				category, 
				k -> new EntitiesCategory()
			);
			
			if (entitiesCategory.entities == null) {
				entitiesCategory.entities = new LinkedHashMap<>();
			}
			
			if (min == max) {
				entitiesCategory.entities.put(entityId, new XpValue(min));
			} else {
				entitiesCategory.entities.put(entityId, new XpValue(min, max));
			}
			save();
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
			if (data.SmeltingXp == null) {
				data.SmeltingXp = new LinkedHashMap<>();
			}

			SmeltingCategory smeltingCategory = data.SmeltingXp.computeIfAbsent(
				category,
				k -> new SmeltingCategory());
			
				if (smeltingCategory.smelting == null) {
					smeltingCategory.smelting = new LinkedHashMap<>();
				}
				smeltingCategory.smelting.put(productId, xp);
				save();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static int getDurabilityPerLevel() {
		lock.readLock().lock();
		try {
			return data.durabilityPerLevel;
		} finally {
			lock.readLock().unlock();
		}
	}

	public static class ConfigData {
		// Mending: move durability repair config to separate section?
		public int durabilityPerLevel = 100;
		public int maxAnvilRepairCost = 40;
		public boolean xpRepairEnabled = true;

		// boolean flags to enable/disable XP sources
		public boolean blockBreakXpEnabled = true;
		public boolean entityKillXpEnabled = true;
		public boolean xpSmeltingEnabled = true;

		// Category maps - each category has a name and a map of entries (blocks, entities, or recipes)
		public String _MAPS = " - DO NOT EDIT THIS SECTION - use the specific JSON files instead - ";
		public Map<String, SmeltingCategory> SmeltingXp = new LinkedHashMap<>();
		public Map<String, BlockCategory> Blocks = new LinkedHashMap<>();
		public Map<String, EntitiesCategory> Entities = new LinkedHashMap<>();
		public Map<String, CropCategory> Crops = new LinkedHashMap<>();

		public ConfigData() {
			if (Blocks == null) {
				Blocks = new LinkedHashMap<>();
			}
			if (Crops == null) {
				Crops = new LinkedHashMap<>();
			}
			if (Entities == null) {
				Entities = new LinkedHashMap<>();
			}
			if (SmeltingXp == null) {
				SmeltingXp = new LinkedHashMap<>();
			}
		}

		public void initializeDefaults() {
			if (Blocks == null) {
				Blocks = new LinkedHashMap<>();
			}
			if (Crops == null) {
				Crops = new LinkedHashMap<>();
			}
			if (Entities == null) {
				Entities = new LinkedHashMap<>();
			}
			if (SmeltingXp == null) {
				SmeltingXp = new LinkedHashMap<>();
			}

			addDefaultCategories();
		}

		private void addDefaultCategories() {
			// Items category
			EntitiesCategory xpBottle = new EntitiesCategory();
			xpBottle.entities = new LinkedHashMap<>();
			xpBottle.entities.put("minecraft:experience_bottle", new XpValue(3, 11));
			Entities.put("Items", xpBottle);

			
			// Hostile Mobs - ONLY Fixed/Random values (no passive mobs - they'll use vanilla XP)
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

			// Crops category (separate structure with age)
			CropCategory crops = new CropCategory();
			crops.crops = new LinkedHashMap<>();
			crops.crops.put("minecraft:wheat", new CropXpData(7, 0));
			crops.crops.put("minecraft:carrots", new CropXpData(7, 0));
			crops.crops.put("minecraft:potatoes", new CropXpData(7, 0));
			crops.crops.put("minecraft:beetroots", new CropXpData(3, 0));
			crops.crops.put("minecraft:nether_wart", new CropXpData(3, 0));
			crops.crops.put("minecraft:cocoa", new CropXpData(2, 0));
			Crops.put("Minecraft Crops", crops);


			// Smelting/cooking xp per recipe 
			SmeltingCategory smeltingOres = new SmeltingCategory();
			smeltingOres.smelting = new LinkedHashMap<>();
			smeltingOres.smelting.put("minecraft:iron_ingot", 0.7f);
			smeltingOres.smelting.put("minecraft:copper_ingot", 0.7f);
			smeltingOres.smelting.put("minecraft:gold_ingot", 1.0f);
			smeltingOres.smelting.put("minecraft:redstone_dust", 0.3f);
			smeltingOres.smelting.put("minecraft:netherite_scrap", 2.0f);
			SmeltingXp.put("Smelting Ores", smeltingOres);

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
			SmeltingXp.put("Cooking", cooking);

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
			SmeltingXp.put("Smelting Blocks", smeltingBlocks);
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

	// Crops (with age requirement)
	public static class CropCategory {
		public Map<String, CropXpData> crops = new LinkedHashMap<>();

		public CropCategory() {
			if (crops == null) {
				crops = new LinkedHashMap<>();
			}
		}
	}

	// Crop XP data includes mature age and XP values
	public static class CropXpData {
		public int matureAge;
		public XpValue xp;

		// Default constructor for GSON
		public CropXpData() {
			this.matureAge = 7;
			this.xp = new XpValue();
		}

		public CropXpData(int matureAge, int fixed) {
			this.matureAge = matureAge;
			this.xp = new XpValue(fixed);
		}

		public CropXpData(int matureAge, int min, int max) {
			this.matureAge = matureAge;
			this.xp = new XpValue(min, max);
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
			if (type == null) {
				type = "Fixed";
			}

			switch (type) {
				case "Fixed" -> {
					return fixed != null ? fixed : 0;
				}
				case "Random" -> {
					if (min == null || max == null || min < 0 || max < min) {
						return 0;
					}
					if (min.equals(max)) {
						return min;
					}
					return min + RANDOM.nextInt(max - min + 1);
				}
				default -> {
					LOGGER.warn("Unknown XP value type: {}, defaulting to 0", type);
					return 0;
				}
			}
		}
	}
}