package qdeeme.xp_simplifier.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

import qdeeme.xp_simplifier.handler.TradingHandler;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin({Villager.class, WanderingTrader.class})
public class MixinVillagerTradeXp {

    // "vanilla" orb mode: modify the XP amount passed into the ExperienceOrb constructor
    @ModifyArg(
            method = "rewardTradeXp",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"),
            index = 4
    )
    private int modifyTradePlayerXp(int originalXp) {
        if (XpsConfig.getOrbModeEnum() == OrbMode.VANILLA) {
            AbstractVillager merchant = (AbstractVillager) (Object) this;
            int rawId = TradingHandler.getMerchantRawId(merchant);
            return switch (XpsConfig.getTradingPlayerXpModeEnum()) {
                case ON -> {
                    Integer configXp = XpsConfig.getPlayerXp(rawId);
                    yield Math.round((configXp != null ? configXp : originalXp) * XpsConfig.getTradingXpMultiplier());
                }
                case VANILLA -> Math.round(originalXp * XpsConfig.getTradingXpMultiplier());
                case OFF -> 0;
            };
        } else {
            return originalXp;
        }
    }
}
