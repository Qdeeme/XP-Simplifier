package qdeeme.xp_simplifier.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.village.TradeOffer;

@Mixin(TradeOffer.class)
public interface TradeOfferAccessor {
	
	@Accessor("merchantExperience")
	int getOfferExperience();
	
	@Mutable
	@Accessor("merchantExperience")
	void setOfferExperience(int xp);
}