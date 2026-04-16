package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qdeeme.xp_simplifier.handler.ExperienceBottleHandler;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

@Mixin(ExperienceBottleEntity.class)
public class MixinExperienceBottle {

	private static OrbMode ORBMODE;
	private static XpMode  ENTITYXPMODE;

	// "simple" mode: cancel orb via MixinServerWorld, call owner and handle via handler
	@Inject(method = "onCollision", at = @At("HEAD"))
	private void onSimpleCollision(HitResult hitResult, CallbackInfo ci) {
		if (ORBMODE == null) {
			ORBMODE = Config.getOrbModeEnum();
		}
		ExperienceBottleEntity bottle = (ExperienceBottleEntity) (Object) this;
		if (!bottle.getWorld().isClient() && ORBMODE == OrbMode.SIMPLE) {
			ExperienceBottleHandler.onBottleBreak(bottle, hitResult);
		}
	}

	// "vanilla" mode: overwrite the orb spawn amount with config value before orb is created
	@ModifyArg(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"), index = 2)
	private int modifyBottleXP(int defaultXPValue) {
		if (ORBMODE == null) {
			ORBMODE = Config.getOrbModeEnum();
		}
		if (ENTITYXPMODE == null) {
			ENTITYXPMODE = Config.getEntityXpModeEnum();
		}
		if (ORBMODE == OrbMode.VANILLA) {
			return switch (ENTITYXPMODE) {
				case VANILLA -> defaultXPValue;
				case ON -> {
					Integer configXp = Config.getEntityXp(Registries.ENTITY_TYPE.getRawId(EntityType.EXPERIENCE_BOTTLE));
					yield configXp != null ? configXp : defaultXPValue;
				}
				case OFF -> 0;
			};
		}
		return defaultXPValue;
	}
}