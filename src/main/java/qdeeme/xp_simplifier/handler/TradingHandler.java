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




public class TradingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/TradingHandler");

    public static void register() {
        LOGGER.info("Registered Trading XP handler");
    }

    public static void onTradeDone(MerchantEntity merchant, TradeOffer offer, ServerPlayerEntity player) {
        if (!Config.isTradingXpEnabled()) {
            return;
        }

        // Handle player XP based on mode
        String playerXpMode = Config.getTradingPlayerXpMode();
        if (!"off".equalsIgnoreCase(playerXpMode)) {
            if ("vanilla".equalsIgnoreCase(playerXpMode)) {
                 // 3-6 XP, same as vanilla
                player.addExperience(3 + player.getRandom().nextInt(4));
            } else if ("on".equalsIgnoreCase(playerXpMode)) {
                int playerXp = Config.getPlayerXp(getMerchantType(merchant));
                if (playerXp >= 0) {
                    player.addExperience(playerXp);
                }
            }
        }

        if (merchant instanceof VillagerEntity villager) {
            // Handle merchant XP based on mode
            String merchantMode = Config.getMerchantXpMode();
            TradeOfferAccessor accessor = (TradeOfferAccessor) offer;
            if (!"off".equalsIgnoreCase(merchantMode)) {
                if ("on".equalsIgnoreCase(merchantMode)) {
                    int merchantXp = Config.getMerchantXp(getMerchantType(merchant));
                    if (merchantXp >= 0) {
                        int currentXp = accessor.getMerchantExperience();
                        accessor.setMerchantExperience(merchantXp);
                        if (merchantXp != currentXp) {
                            accessor.setMerchantExperience(merchantXp);
                            merchant.sendOffers(player, merchant.getDisplayName(), villager.getExperience());
                        }
                    }
                }
            }
        }
    }

    public static String getMerchantType(MerchantEntity merchant) {
        if (merchant instanceof VillagerEntity villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            return Registries.VILLAGER_PROFESSION.getId(profession).toString();
        } else if (merchant instanceof WanderingTraderEntity) {
            return "minecraft:wandering_trader";
        }
        return "minecraft:unemployed";
    }
}
