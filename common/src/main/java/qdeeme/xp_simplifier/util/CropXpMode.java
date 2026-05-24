package qdeeme.xp_simplifier.util;

/**
 * Controls crop XP behaviour.
 * Fabric historically used {@code XpMode} for crops (including VANILLA);
 * NeoForge uses this two-value enum. {@link #from} maps VANILLA → OFF.
 */
public enum CropXpMode {
    ON, OFF;

    /** Case-insensitive factory; VANILLA and unknown values map to {@code OFF}. */
    public static CropXpMode from(String s) {
        if (s == null) return OFF;
        return switch (s.toUpperCase()) {
            case "ON" -> ON;
            default   -> OFF; // covers OFF and legacy VANILLA
        };
    }
}

