package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qdeeme.xp_simplifier.handler.ExperienceBottleHandler;

@Mixin(ExperienceBottleEntity.class)
public class MixinExperienceBottle {

	@Inject(method = "onCollision", at = @At("HEAD"))
	private void onCollision(HitResult hitResult, CallbackInfo ci) {
		ExperienceBottleEntity bottle = (ExperienceBottleEntity) (Object) this;
		
		// Only process on server side
		if (!bottle.getWorld().isClient()) {
			ExperienceBottleHandler.onBottleBreak(bottle, hitResult);
		}
	}
}