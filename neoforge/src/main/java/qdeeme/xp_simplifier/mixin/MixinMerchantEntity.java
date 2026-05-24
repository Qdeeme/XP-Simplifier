package qdeeme.xp_simplifier.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffer;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;


import qdeeme.xp_simplifier.handler.TradingHandler;


@Mixin(AbstractVillager.class)
public abstract class MixinMerchantEntity {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void onSimpleTradeComplete(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager merchant = (AbstractVillager) (Object) this;
        Player customer = merchant.getTradingPlayer();
        if (customer instanceof ServerPlayer serverPlayer) {
            if (XpsConfig.getOrbModeEnum() == OrbMode.SIMPLE) {
                TradingHandler.onTradeDonePlayer(merchant, offer, serverPlayer);
                TradingHandler.onTradeDoneMerch(merchant, offer, serverPlayer);
            } else {
                TradingHandler.onTradeDoneMerch(merchant, offer, serverPlayer);
            }
        }
    }
}