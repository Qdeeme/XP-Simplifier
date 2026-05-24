package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.passive.MerchantEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;

import qdeeme.xp_simplifier.handler.TradingHandler;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin({VillagerEntity.class, WanderingTraderEntity.class})
public class MixinVillagerTradeXp {

    // "vanilla" orb mode: modify the XP amount passed into the ExperienceOrbEntity constructor
    @ModifyArg(
            method = "afterUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;<init>(Lnet/minecraft/world/World;DDDI)V"),
            index = 4
    )
    private int modifyTradePlayerXp(int originalXp) {
        if (Config.getOrbModeEnum() == OrbMode.VANILLA) {
            MerchantEntity merchant = (MerchantEntity) (Object) this;
            int rawId = TradingHandler.getMerchantRawId(merchant);
            return switch (Config.getTradingPlayerXpModeEnum()) {
                case ON -> {
                    Integer configXp = Config.getPlayerXp(rawId);
                    yield Math.round((configXp != null ? configXp : originalXp) * Config.getTradingXpMultiplier());
                }
                case VANILLA -> Math.round(originalXp * Config.getTradingXpMultiplier());
                case OFF -> 0;
            };
        } else {
            return originalXp;
        }
    }
}
