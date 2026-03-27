package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.village.TradeOffer;

@Mixin(TradeOffer.class)
public interface TradeOfferAccessor {
	
	@Accessor("merchantExperience")
	int getMerchantExperience();
	
	@Mutable
	@Accessor("merchantExperience")
	void setMerchantExperience(int xp);
}