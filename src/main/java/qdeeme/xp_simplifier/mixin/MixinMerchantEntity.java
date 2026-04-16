package qdeeme.xp_simplifier.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;


import qdeeme.xp_simplifier.handler.TradingHandler;

@Mixin(MerchantEntity.class)
public abstract class MixinMerchantEntity {
	private static OrbMode ORBMODE;

	@Shadow
	public abstract PlayerEntity getCustomer();

	@Shadow
	public abstract int getExperience();

	@Inject(method = "trade", at = @At("TAIL"))
	private void onSimpleTradeComplete(TradeOffer offer, CallbackInfo ci) {
		
		if (ORBMODE == null) {
			ORBMODE = Config.getOrbModeEnum();
		}

		MerchantEntity merchant = (MerchantEntity) (Object) this;
		PlayerEntity customer = merchant.getCustomer();
		if (customer instanceof ServerPlayerEntity serverPlayer) {
			if (ORBMODE == OrbMode.SIMPLE) {
				TradingHandler.onTradeDonePlayer(merchant, offer, serverPlayer);
				TradingHandler.onTradeDoneMerch(merchant, offer, serverPlayer);
			} else {
				TradingHandler.onTradeDoneMerch(merchant, offer, serverPlayer);
			}
		}
	}
}