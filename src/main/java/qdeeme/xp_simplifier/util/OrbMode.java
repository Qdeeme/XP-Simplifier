package qdeeme.xp_simplifier.util;

public enum OrbMode {
    SIMPLE, VANILLA;

    public static OrbMode from(String s) {
        if (s == null) return VANILLA;
        return switch (s.toUpperCase()) {
            case "SIMPLE" -> SIMPLE;
            case "VANILLA" -> VANILLA;
            default -> VANILLA;
        };
    }
}
