package qdeeme.xp_simplifier.util.config;

import qdeeme.xp_simplifier.util.CropXpMode;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

/**
 * Hard-coded default values for all XP Simplifier flags.
 * Applied on first run and as a fallback for any key absent from persisted JSON.
 * No loader API references — safe to use from common.
 */
public final class XpsConfigInit {

    private XpsConfigInit() {
    }

    // ── Orb behaviour ───────────────────────────────────────────────────────
    public static final OrbMode ORB_MODE = OrbMode.VANILLA;

    // ── Per-system XP modes ─────────────────────────────────────────────────
    public static final XpMode ENTITY_XP_MODE = XpMode.VANILLA;
    public static final XpMode BLOCK_XP_MODE = XpMode.VANILLA;
    public static final CropXpMode CROP_XP_MODE = CropXpMode.OFF;
    public static final XpMode BREEDING_XP_MODE = XpMode.VANILLA;
    public static final XpMode FISHING_XP_MODE = XpMode.VANILLA;
    public static final XpMode GRINDSTONE_XP_MODE = XpMode.VANILLA;
    public static final XpMode SMELTING_XP_MODE = XpMode.VANILLA;
    public static final XpMode TRADING_PLAYER_XP = XpMode.VANILLA;
    public static final XpMode MERCHANT_XP = XpMode.VANILLA;

    // ── Anvil / XP repair ───────────────────────────────────────────────────
    public static final boolean XP_REPAIR_ENABLED = true;
    /**
     * Durability points restored per XP point spent on mending (float).
     */
    public static final float DURABILITY_PER_POINT = 2.0f;
    public static final int MAX_ANVIL_REPAIR_COST = 40;

    // ── Per-system XP multipliers ───────────────────────────────────────────
    public static final float ENTITY_XP_MULTIPLIER = 1.0f;
    public static final float BLOCK_XP_MULTIPLIER = 1.0f;
    public static final float CROP_XP_MULTIPLIER = 1.0f;
    public static final float SMELTING_XP_MULTIPLIER = 1.0f;
    public static final float TRADING_XP_MULTIPLIER = 1.0f;
    public static final float MERCHANT_XP_MULTIPLIER = 1.0f;
    public static final float BREEDING_XP_MULTIPLIER = 1.0f;
    public static final float FISHING_XP_MULTIPLIER = 1.0f;
    public static final float GRINDSTONE_XP_MULTIPLIER = 1.0f;
}

