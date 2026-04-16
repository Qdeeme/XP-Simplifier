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
import qdeeme.xp_simplifier.mixin.TradeOfferAccessor;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.XpMode;




public class TradingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/TradingHandler");
    private static final XpMode PLAYERMODE  = Config.getTradingPlayerXpModeEnum();
    private static final XpMode MERCHANTMODE = Config.getMerchantXpModeEnum();

    public static void register() {
        LOGGER.info("Registered Trading XP handler");
    }

    public static void onTradeDonePlayer(MerchantEntity merchant, TradeOffer offer, ServerPlayerEntity player) {

        // Handle player XP based on mode
        int vanillaXP = 3 + player.getRandom().nextInt(4);
        switch (PLAYERMODE) {
            case VANILLA:
                player.addExperience(vanillaXP);
                break;
            case ON:
                Integer playerXp = Config.getPlayerXp(getMerchantRawId(merchant));
                player.addExperience(playerXp != null ? playerXp : vanillaXP);
                break;
            case OFF:
                break;
        }
    }
        
    public static void onTradeDoneMerch(MerchantEntity merchant, TradeOffer offer, ServerPlayerEntity player) {
        if (merchant instanceof VillagerEntity villager) {
            // Handle merchant XP based on mode
            TradeOfferAccessor accessor = (TradeOfferAccessor) offer;
            switch (MERCHANTMODE) {
                case ON -> {
                    Integer merchantXp = Config.getMerchantXp(getMerchantRawId(merchant));
                    if (merchantXp != null) {
                        int currentXp = accessor.getMerchantExperience();
                        accessor.setMerchantExperience(merchantXp);
                        if (merchantXp != currentXp) {
                            merchant.sendOffers(player, merchant.getDisplayName(), villager.getExperience());
                        }
                    }
                }
                case VANILLA -> {
                    villager.getExperience();
                    break;
                }
                case OFF -> {
                    int currentXp = accessor.getMerchantExperience();
                    accessor.setMerchantExperience(0);
                    if (currentXp != 0) {
                        merchant.sendOffers(player, merchant.getDisplayName(), villager.getExperience());
                        break;
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
}
