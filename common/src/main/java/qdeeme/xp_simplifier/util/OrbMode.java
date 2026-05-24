package qdeeme.xp_simplifier.util;

public enum OrbMode {
    SIMPLE, VANILLA;

    /** Case-insensitive factory; returns {@code VANILLA} for unknown/null values. */
    public static OrbMode from(String s) {
        if (s == null) return VANILLA;
        return switch (s.toUpperCase()) {
            case "SIMPLE" -> SIMPLE;
            default -> VANILLA;
        };
    }
}

