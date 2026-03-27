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

import qdeeme.xp_simplifier.handler.TradingHandler;

@Mixin(MerchantEntity.class)
public abstract class MixinMerchantEntity {

	@Shadow
	public abstract PlayerEntity getCustomer();

	@Shadow
	public abstract int getExperience();


	@Inject(method = "trade", at = @At("TAIL"))
	private void onTradeComplete(TradeOffer offer, CallbackInfo ci) {
		MerchantEntity merchant = (MerchantEntity) (Object) this;
		PlayerEntity customer = merchant.getCustomer();

		if (customer instanceof ServerPlayerEntity serverPlayer) {
			TradingHandler.onTradeDone(merchant, offer, serverPlayer);
		}
	}
}