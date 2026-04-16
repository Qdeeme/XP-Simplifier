package qdeeme.xp_simplifier.util;

public enum XpMode {
    ON, VANILLA, OFF;

    public static XpMode from(String s) {
        if (s == null) {
            return VANILLA;
        }
        return switch (s.toUpperCase()) {
            case "ON" -> ON;
            case "OFF" -> OFF;
            case "VANILLA" -> VANILLA;
            default -> VANILLA;
        };
    }
}
