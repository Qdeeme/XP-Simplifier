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

import net.fabricmc.loader.api.FabricLoader;

public class Config {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("xp_simplifier.json");
	private static final Random RANDOM = new Random();
	private static final ReadWriteLock lock = new ReentrantReadWriteLock();

	private static ConfigData data = new ConfigData();

	public static void load() {
		lock.writeLock().lock();
		try {
			boolean needsSave = false;
			
			if (Files.exists(CONFIG_PATH)) {
				String json = Files.readString(CONFIG_PATH);
				ConfigData loadedData = GSON.fromJson(json, ConfigData.class);
				
				if (loadedData != null) {
					data = loadedData;
					// Check if we need to add missing categories
					if ((data.Blocks == null || data.Blocks.isEmpty()) &&
					    (data.Crops == null || data.Crops.isEmpty()) &&
					    (data.Entities == null || data.Entities.isEmpty())) {
						data.initializeDefaults();
						needsSave = true;
					}
					LOGGER.info("Loaded xp_simplifier config");
				} else {
					LOGGER.warn("Config file was empty, using defaults");
					data = new ConfigData();
					data.initializeDefaults();
					needsSave = true;
				}
			} else {
				LOGGER.info("Config file not found, creating default config");
				data = new ConfigData();
				data.initializeDefaults();
				needsSave = true;
			}
			
			if (needsSave) {
				saveInternal();
			}
		} catch (JsonSyntaxException e) {
			LOGGER.error("Invalid JSON in config file, using defaults", e);
			data = new ConfigData();
			data.initializeDefaults();
			saveInternal();
		} catch (IOException e) {
			LOGGER.error("Failed to load config, using defaults", e);
			data = new ConfigData();
			data.initializeDefaults();
		} finally {
			lock.writeLock().unlock();
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
			String json = GSON.toJson(data);
			Files.writeString(CONFIG_PATH, json);
			LOGGER.debug("Saved config");
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
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

	// Get block XP with age check (for crops)
	public static int getBlockXp(String blockId, int currentAge) {
		if (blockId == null) {
			return 0;
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

			return 0;
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
			throw new IllegalArgumentException("Category and block ID cannot be null");
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
	public static int getDurabilityPerLevel() {
		lock.readLock().lock();
		try {
			return data.durabilityPerLevel;
		} finally {
			lock.readLock().unlock();
		}
	}

	public static class ConfigData {
		public int durabilityPerLevel = 100;
		public int maxAnvilRepairCost = 40;
		public boolean xpRepairEnabled = true;
		public boolean blockBreakXpEnabled = true;
		public boolean entityKillXpEnabled = true;
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

			addDefaultCategories();
		}

		private void addDefaultCategories() {
			// Items category
			EntitiesCategory xpBottle = new EntitiesCategory();
			xpBottle.entities = new LinkedHashMap<>();
			xpBottle.entities.put("minecraft:experience_bottle", new XpValue(3, 11));
			Entities.put("items", xpBottle);

			
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
			Entities.put("hostile_mobs", hostileMobs);

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
			Entities.put("passive_mobs", passiveMobs);

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
			Blocks.put("ores", ores);

			// Wood
			BlockCategory wood = new BlockCategory();
			wood.blocks = new LinkedHashMap<>();
			Blocks.put("wood", wood);

			// Stone 
			BlockCategory stone = new BlockCategory();
			stone.blocks = new LinkedHashMap<>();
			Blocks.put("stone", stone);

			// Nether 
			BlockCategory nether = new BlockCategory();
			nether.blocks = new LinkedHashMap<>();
			Blocks.put("nether", nether);

			// End 
			BlockCategory end = new BlockCategory();
			end.blocks = new LinkedHashMap<>();
			Blocks.put("end", end);

			// Crops category (separate structure with age)
			CropCategory crops = new CropCategory();
			crops.crops = new LinkedHashMap<>();
			crops.crops.put("minecraft:wheat", new CropXpData(7, 0));
			crops.crops.put("minecraft:carrots", new CropXpData(7, 0));
			crops.crops.put("minecraft:potatoes", new CropXpData(7, 0));
			crops.crops.put("minecraft:beetroots", new CropXpData(3, 0));
			crops.crops.put("minecraft:nether_wart", new CropXpData(3, 0));
			crops.crops.put("minecraft:cocoa", new CropXpData(2, 0));
			Crops.put("minecraft", crops);
		}
	}

	// Entities category
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