package qdeeme.xp_simplifier.util.config;

import java.util.Map;

/** Holds player-side and merchant-side XP values for a single trader/profession. Pure POJO. */
public class TradingXpValues {
    public XpValue playerXp;
    public XpValue merchantXp;

    public TradingXpValues() {
        this.playerXp   = new XpValue();
        this.merchantXp = new XpValue();
    }

    public TradingXpValues(int playerMin, int playerMax, int merchantMin, int merchantMax) {
        this.playerXp   = playerMin   == playerMax   ? new XpValue(playerMin)   : new XpValue(playerMin,   playerMax);
        this.merchantXp = merchantMin == merchantMax ? new XpValue(merchantMin) : new XpValue(merchantMin, merchantMax);
    }

    /** Parses a GSON-decoded raw object ({@code Map}) into a typed {@code TradingXpValues}. */
    public static TradingXpValues fromRaw(Object raw) {
        if (!(raw instanceof Map<?,?> m)) return new TradingXpValues();
        TradingXpValues tv = new TradingXpValues();
        tv.playerXp   = XpValue.fromRaw(m.get("playerXp"));
        tv.merchantXp = XpValue.fromRaw(m.get("merchantXp"));
        return tv;
    }
}

