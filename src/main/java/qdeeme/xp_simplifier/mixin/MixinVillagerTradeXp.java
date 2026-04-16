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
import qdeeme.xp_simplifier.util.XpMode;

@Mixin({VillagerEntity.class, WanderingTraderEntity.class})
public class MixinVillagerTradeXp {

    private static OrbMode ORBMODE;
    private static XpMode PLAYERMODE;

    // "vanilla" orb mode: modify the XP amount passed into the ExperienceOrbEntity constructor
    @ModifyArg(
        method = "afterUsing",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;<init>(Lnet/minecraft/world/World;DDDI)V"),
        index = 4
    )
    private int modifyTradePlayerXp(int originalXp) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (PLAYERMODE == null) {
            PLAYERMODE = Config.getTradingPlayerXpModeEnum();
        }
        if (ORBMODE == OrbMode.VANILLA) {
            MerchantEntity merchant = (MerchantEntity) (Object) this;
            int rawId = TradingHandler.getMerchantRawId(merchant);
            return switch (PLAYERMODE) {
                case ON -> {
                    Integer configXp = Config.getPlayerXp(rawId);
                    yield configXp != null ? configXp : originalXp;
                }
                case VANILLA -> originalXp;
                case OFF -> 0;
            };
        } else {
            return originalXp;
        }
    }
}
