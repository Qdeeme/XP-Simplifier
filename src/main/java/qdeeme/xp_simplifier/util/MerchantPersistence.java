package qdeeme.xp_simplifier.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Persistent storage for merchant XP accumulated through trades across sessions.
 * Stores merchant UUIDs and their total accumulated XP, allowing merchants to
 * retain their XP gains even when config values change.
 */
public class MerchantPersistence {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/MerchantPersistence");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path MERCHANT_DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("Xp Simplifier");
	private static final Path MERCHANT_XP_PATH = MERCHANT_DATA_DIR.resolve("merchant_xp.json");
	private static final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private static Map<String, Integer> merchantXpData = new HashMap<>();

	/**
	 * Load merchant XP data from persistent storage.
	 */
	public static void load() {
		lock.writeLock().lock();
		try {
			merchantXpData = new HashMap<>();
			
			if (!Files.exists(MERCHANT_XP_PATH)) {
				LOGGER.debug("No merchant XP file found, starting fresh");
				return;
			}

			try {
				String json = Files.readString(MERCHANT_XP_PATH);
				Map<String, Integer> loaded = GSON.fromJson(json, 
					new TypeToken<Map<String, Integer>>(){}.getType());
				if (loaded != null) {
					merchantXpData = loaded;
					LOGGER.info("Loaded merchant XP data for {} merchants", merchantXpData.size());
				}
			} catch (JsonSyntaxException e) {
				LOGGER.error("Invalid JSON in merchant XP file", e);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load merchant XP data", e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Save merchant XP data to persistent storage.
	 */
	public static void save() {
		lock.writeLock().lock();
		try {
			// Ensure config directory exists
			if (!Files.exists(MERCHANT_DATA_DIR)) {
				Files.createDirectories(MERCHANT_DATA_DIR);
			}

			String json = GSON.toJson(merchantXpData);
			Files.writeString(MERCHANT_XP_PATH, json);
			LOGGER.debug("Saved merchant XP data");
		} catch (IOException e) {
			LOGGER.error("Failed to save merchant XP data", e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Get the stored XP for a merchant UUID.
	 * Returns 0 if no data exists for this merchant.
	 */
	public static int getMerchantXp(UUID uuid) {
		lock.readLock().lock();
		try {
			return merchantXpData.getOrDefault(uuid.toString(), 0);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Add XP to a merchant's accumulated total. This adds to existing XP.
	 */
	public static void addMerchantXp(UUID uuid, int xpToAdd) {
		lock.writeLock().lock();
		try {
			String uuidStr = uuid.toString();
			int currentXp = merchantXpData.getOrDefault(uuidStr, 0);
			merchantXpData.put(uuidStr, currentXp + xpToAdd);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Set a merchant's total accumulated XP (overwrites existing value).
	 */
	public static void setMerchantXp(UUID uuid, int xp) {
		lock.writeLock().lock();
		try {
			merchantXpData.put(uuid.toString(), xp);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Check if we have stored XP data for a specific merchant.
	 */
	public static boolean hasMerchantXp(UUID uuid) {
		lock.readLock().lock();
		try {
			return merchantXpData.containsKey(uuid.toString());
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Clear a merchant's stored XP (e.g., if they're removed).
	 */
	public static void removeMerchantXp(UUID uuid) {
		lock.writeLock().lock();
		try {
			merchantXpData.remove(uuid.toString());
		} finally {
			lock.writeLock().unlock();
		}
	}
}
