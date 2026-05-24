package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.entity.npc.VillagerProfession;
import qdeeme.xp_simplifier.mixin.accessors.TradeOfferAccessor;
import qdeeme.xp_simplifier.util.MerchantOffersData;
import qdeeme.xp_simplifier.util.config.XpsConfig;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;


public class TradingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/TradingHandler");
    private static final Map<MerchantOffer, Integer> ORIGINAL_OFFER_XP = new IdentityHashMap<>();

    public static void register() {
        LOGGER.info("Registered Trading XP handler");
    }

    public static void onTradeDonePlayer(AbstractVillager merchant, MerchantOffer offer, ServerPlayer player) {
        int vanillaXP = 3 + player.getRandom().nextInt(4);
        switch (XpsConfig.getTradingPlayerXpModeEnum()) {
            case VANILLA:
                player.giveExperiencePoints(Math.round(vanillaXP * XpsConfig.getTradingXpMultiplier()));
                break;
            case ON:
                Integer playerXp = XpsConfig.getPlayerXp(getMerchantRawId(merchant));
                int base = playerXp != null ? playerXp : vanillaXP;
                player.giveExperiencePoints(Math.round(base * XpsConfig.getTradingXpMultiplier()));
                break;
            case OFF:
                break;
        }
    }

    public static void onTradeDoneMerch(AbstractVillager merchant, MerchantOffer offer, ServerPlayer player) {
        if (merchant instanceof Villager villager) {
            UUID uuid = merchant.getUUID();
            TradeOfferAccessor accessor = (TradeOfferAccessor) offer;

            // --- Resolve baseXp --------------------------------------------------
            // Check the in-memory cache first (fast path for repeat trades).
            Integer baseXp = ORIGINAL_OFFER_XP.get(offer);

            if (baseXp == null) {
                int offerIndex = merchant.getOffers().indexOf(offer);
                int persisted = (offerIndex >= 0) ? MerchantOffersData.getOfferXp(uuid, offerIndex) : -1;
                if (persisted != -1) {
                    baseXp = persisted;
                } else {
                    baseXp = accessor.getOfferExperience();

                    // Persist the vanilla snapshot immediately so it survives restarts.
                    if (offerIndex >= 0) {
                        MerchantOffersData.setOfferXpIfAbsent(uuid, offerIndex, baseXp);
                        MerchantOffersData.save();
                    }
                }

                // Populate cache so subsequent trades on this offer skip all of the above.
                ORIGINAL_OFFER_XP.put(offer, baseXp);
            }
            int lastXp = accessor.getOfferExperience();
            int finalXp;

            switch (XpsConfig.getMerchantXpModeEnum()) {
                case ON -> {
                    Integer merchantXp = XpsConfig.getMerchantXp(getMerchantRawId(merchant));
                    finalXp = Math.round((merchantXp != null ? merchantXp : baseXp) * XpsConfig.getMerchantXpMultiplier());
                    accessor.setOfferExperience(finalXp);
                    if (finalXp != lastXp) {
                        merchant.openTradingScreen(player, merchant.getDisplayName(), villager.getVillagerXp());
                    }
                }
                case VANILLA -> {
                    finalXp = Math.round(baseXp * XpsConfig.getMerchantXpMultiplier());
                    accessor.setOfferExperience(finalXp);
                    if (finalXp != lastXp) {
                        merchant.openTradingScreen(player, merchant.getDisplayName(), villager.getVillagerXp());
                    }
                }
                case OFF -> {
                    accessor.setOfferExperience(0);
                    if (lastXp != 0) {
                        merchant.openTradingScreen(player, merchant.getDisplayName(), villager.getVillagerXp());
                    }
                }
            }
        }
    }

    public static int getMerchantRawId(AbstractVillager merchant) {
        if (merchant instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            return BuiltInRegistries.VILLAGER_PROFESSION.getId(profession);
        } else if (merchant instanceof WanderingTrader) {
            return XpsConfig.getWanderingTraderRawId();
        }
        return -1;
    }
}
