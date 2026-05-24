package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import qdeeme.xp_simplifier.mixin.accessor.TradeOfferAccessor;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.MerchantOffersData;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;


public class TradingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/TradingHandler");
    private static final Map<TradeOffer, Integer> ORIGINAL_OFFER_XP = new IdentityHashMap<>();

    public static void register() {
        LOGGER.info("Registered Trading XP handler");
    }

    public static void onTradeDonePlayer(MerchantEntity merchant, TradeOffer offer, ServerPlayerEntity player) {
        int vanillaXP = 3 + player.getRandom().nextInt(4);
        switch (Config.getTradingPlayerXpModeEnum()) {
            case VANILLA:
                player.addExperience(Math.round(vanillaXP * Config.getTradingXpMultiplier()));
                break;
            case ON:
                Integer playerXp = Config.getPlayerXp(getMerchantRawId(merchant));
                int base = playerXp != null ? playerXp : vanillaXP;
                player.addExperience(Math.round(base * Config.getTradingXpMultiplier()));
                break;
            case OFF:
                break;
        }
    }

    public static void onTradeDoneMerch(MerchantEntity merchant, TradeOffer offer, ServerPlayerEntity player) {
        if (merchant instanceof VillagerEntity villager) {

            UUID uuid = merchant.getUuid();
            TradeOfferAccessor accessor = (TradeOfferAccessor) offer;

            // --- Resolve baseXp --------------------------------------------------
            Integer baseXp = ORIGINAL_OFFER_XP.get(offer);

            if (baseXp == null) {
                // Cache miss — this offer hasn't been seen yet this session.
                int offerIndex = merchant.getOffers().indexOf(offer);
                int persisted = (offerIndex >= 0) ? MerchantOffersData.getOfferXp(uuid, offerIndex) : -1;
                if (persisted != -1) {
                    // Known merchant, known slot — restore from persistence into cache.
                    baseXp = persisted;
                } else {
                    // vanilla value (nothing has modified it yet this session).
                    baseXp = accessor.getOfferExperience();
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


            switch (Config.getMerchantXpModeEnum()) {
                case ON -> {
                    Integer merchantXp = Config.getMerchantXp(getMerchantRawId(merchant));
                    finalXp = Math.round((merchantXp != null ? merchantXp : baseXp) * Config.getMerchantXpMultiplier());
                    accessor.setOfferExperience(finalXp);
                    if (finalXp != lastXp) {
                        merchant.sendOffers(player, merchant.getDisplayName(), villager.getExperience());
                    }
                }
                case VANILLA -> {
                    finalXp = Math.round(baseXp * Config.getMerchantXpMultiplier());
                    accessor.setOfferExperience(finalXp);
                    if (finalXp != lastXp) {
                        merchant.sendOffers(player, merchant.getDisplayName(), villager.getExperience());
                    }
                }
                case OFF -> {
                    accessor.setOfferExperience(0);
                    if (lastXp != 0) {
                        merchant.sendOffers(player, merchant.getDisplayName(), villager.getExperience());
                    }
                }
            }
        }
    }

    public static int getMerchantRawId(MerchantEntity merchant) {
        if (merchant instanceof VillagerEntity villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            return Registries.VILLAGER_PROFESSION.getRawId(profession);
        } else if (merchant instanceof WanderingTraderEntity) {
            return Config.getWanderingTraderRawId();
        }
        return -1;
    }

    public static void saveMerchantData() {
        MerchantOffersData.save();
        ORIGINAL_OFFER_XP.clear();
    }
}
