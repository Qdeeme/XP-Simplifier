package qdeeme.xp_simplifier.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qdeeme.xp_simplifier.util.config.ConfigServerSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent storage for per-merchant, per-offer vanilla XP values.
 *
 * <p>JSON layout:
 * <pre>
 * {
 *   "uuid-string": { "0": 5, "1": 10, "2": 2 },
 *   ...
 * }
 * </pre>
 * Keys are always strings because GSON only supports String-keyed JSON objects.
 * All UUID/int conversions are handled here; callers use UUID and int directly.
 */
public class MerchantOffersData {

    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/MerchantPersistence");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // String keys throughout — avoids the silent GSON UUID/Integer key deserialise bug.
    static Map<String, Map<String, Integer>> merchantXpData = new HashMap<>();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public static void load() {
        Path DATA_DIR = ConfigServerSync.getWorldDir().resolve("xp_simplifier");
        Path DATA_FILE = DATA_DIR.resolve("merchants_offerxp_data.json");
        merchantXpData = new HashMap<>();
        if (!Files.exists(DATA_FILE)) {
            save();
            LOGGER.debug("No merchant XP file found, starting fresh");
            return;
        }
        try {
            String json = Files.readString(DATA_FILE);
            Map<String, Map<String, Integer>> loaded = GSON.fromJson(json,
                    new TypeToken<Map<String, Map<String, Integer>>>() {
                    }.getType());
            if (loaded != null) {
                merchantXpData = loaded;
                LOGGER.info("Loaded vanilla XP data for {} merchants", merchantXpData.size());
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("Corrupt merchant XP file, starting fresh", e);
        } catch (IOException e) {
            LOGGER.error("Failed to read merchant XP file", e);
        }
    }

    public static void save() {
        Path DATA_DIR = ConfigServerSync.getWorldDir().resolve("xp_simplifier");
        Path DATA_FILE = DATA_DIR.resolve("merchants_offerxp_data.json");
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            Files.writeString(DATA_FILE, GSON.toJson(merchantXpData));
            LOGGER.debug("Saved vanilla XP data for {} merchants", merchantXpData.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save merchant XP data", e);
        }
    }

    // -------------------------------------------------------------------------
    // Access
    // -------------------------------------------------------------------------

    /**
     * Returns the persisted vanilla XP for a single offer slot, or -1 if absent.
     * -1 is the sentinel: "this slot has never been traded on a known merchant".
     */
    public static int getOfferXp(UUID uuid, int offerIndex) {
        Map<String, Integer> offers = merchantXpData.get(uuid.toString());
        if (offers == null) return -1;
        return offers.getOrDefault(String.valueOf(offerIndex), -1);
    }

    /**
     * Stores vanilla XP for one offer slot. Uses putIfAbsent so a modified value
     * can never overwrite the vanilla snapshot once it is written.
     */
    public static void setOfferXpIfAbsent(UUID uuid, int offerIndex, int xpValue) {
        merchantXpData.computeIfAbsent(uuid.toString(), k -> new HashMap<>())
                .putIfAbsent(String.valueOf(offerIndex), xpValue);
    }

    /**
     * Removes all data for a merchant (e.g. on death). Call save() after.
     */
    public static void removeMerchant(UUID uuid) {
        merchantXpData.remove(uuid.toString());
    }
}