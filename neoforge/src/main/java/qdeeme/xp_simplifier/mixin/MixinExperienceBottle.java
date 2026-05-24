package qdeeme.xp_simplifier.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qdeeme.xp_simplifier.handler.ExperienceBottleHandler;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(ThrownExperienceBottle.class)
public class MixinExperienceBottle {

    // "simple" mode: cancel orb via MixinServerLevel, call owner and handle via handler
    @Inject(method = "onHit", at = @At("HEAD"))
    private void onSimpleCollision(HitResult hitResult, CallbackInfo ci) {
        ThrownExperienceBottle bottle = (ThrownExperienceBottle) (Object) this;
        if (!bottle.level().isClientSide() && XpsConfig.getOrbModeEnum() == OrbMode.SIMPLE) {
            ExperienceBottleHandler.onBottleBreak(bottle, hitResult);
        }
    }

    // "vanilla" mode: overwrite the orb spawn amount with config value before orb is created
    @ModifyArg(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"), index = 2)
    private int modifyBottleXP(int defaultXPValue) {
        if (XpsConfig.getOrbModeEnum() == OrbMode.VANILLA) {
            return switch (XpsConfig.getEntityXpModeEnum()) {
                case VANILLA -> Math.round(defaultXPValue * XpsConfig.getEntityXpMultiplier());
                case ON -> {
                    Integer configXp = XpsConfig.getEntityXp(BuiltInRegistries.ENTITY_TYPE.getId(EntityType.EXPERIENCE_BOTTLE));
                    yield Math.round((configXp != null ? configXp : defaultXPValue) * XpsConfig.getEntityXpMultiplier());
                }
                case OFF -> 0;
            };
        }
        return defaultXPValue;
    }
}