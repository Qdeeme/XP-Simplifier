package qdeeme.xp_simplifier.mixin.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.trading.MerchantOffer;

@Mixin(MerchantOffer.class)
public interface TradeOfferAccessor {
	
	@Accessor("xp")
	int getOfferExperience();
	
	@Mutable
	@Accessor("xp")
	void setOfferExperience(int xp);
}