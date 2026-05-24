package qdeeme.xp_simplifier.util.config;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;

/**
 * Immutable-ish XP value — either a fixed amount or a random range. Pure POJO, no loader deps.
 */
public class XpValue {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/XpValue");
    private static final Random RANDOM = new Random();

    public enum Type {
        @SerializedName(value = "Fixed", alternate = {"FIXED", "fixed"}) FIXED,
        @SerializedName(value = "Random", alternate = {"RANDOM", "random"}) RANDOM
    }

    public Type type;
    public Integer min;
    public Integer max;
    public Integer fixed;

    /**
     * Default constructor for GSON.
     */
    public XpValue() {
        this.type = Type.FIXED;
        this.fixed = 0;
    }

    public XpValue(int min, int max) {
        this.type = Type.RANDOM;
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public XpValue(int fixed) {
        this.type = Type.FIXED;
        this.fixed = fixed;
    }

    public int calculateXp() {
        if (type == null) {
            LOGGER.warn("XpValue has null type (unknown value in JSON), defaulting to 0");
            return 0;
        }
        return switch (type) {
            case FIXED -> fixed == null ? 0 : fixed;
            case RANDOM -> {
                if (min == null || max == null) yield 0;
                if (min.equals(max)) yield min;
                yield min + RANDOM.nextInt(max - min + 1);
            }
        };
    }

    /**
     * Parses a GSON-decoded raw object ({@code Map}) into a typed {@code XpValue}.
     */
    public static XpValue fromRaw(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) return new XpValue(0);
        Number fixedN = (Number) m.get("fixed");
        Number minN = (Number) m.get("min");
        Number maxN = (Number) m.get("max");
        String type = m.get("type") instanceof String s ? s : null;
        if ("RANDOM".equalsIgnoreCase(type) && minN != null && maxN != null)
            return new XpValue(minN.intValue(), maxN.intValue());
        if (fixedN != null) return new XpValue(fixedN.intValue());
        if (minN != null && maxN != null) return new XpValue(minN.intValue(), maxN.intValue());
        if (minN != null) return new XpValue(minN.intValue());
        return new XpValue(0);
    }
}

