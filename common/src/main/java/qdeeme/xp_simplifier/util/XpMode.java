package qdeeme.xp_simplifier.util;

public enum XpMode {
    ON, VANILLA, OFF;

    /** Case-insensitive factory; returns {@code VANILLA} for unknown/null values. */
    public static XpMode from(String s) {
        if (s == null) return VANILLA;
        return switch (s.toUpperCase()) {
            case "ON"  -> ON;
            case "OFF" -> OFF;
            default    -> VANILLA;
        };
    }
}

