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

@Mixin(ExperienceBottleEntity.class)
public class MixinExperienceBottle {

    // "simple" mode: cancel orb via MixinServerWorld, call owner and handle via handler
    @Inject(method = "onCollision", at = @At("HEAD"))
    private void onSimpleCollision(HitResult hitResult, CallbackInfo ci) {
        ExperienceBottleEntity bottle = (ExperienceBottleEntity) (Object) this;
        if (!bottle.getWorld().isClient() && Config.getOrbModeEnum() == OrbMode.SIMPLE) {
            ExperienceBottleHandler.onBottleBreak(bottle, hitResult);
        }
    }

    // "vanilla" mode: overwrite the orb spawn amount with config value before orb is created
    @ModifyArg(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"), index = 2)
    private int modifyBottleXP(int defaultXPValue) {
        if (Config.getOrbModeEnum() == OrbMode.VANILLA) {
            return switch (Config.getEntityXpModeEnum()) {
                case VANILLA -> Math.round(defaultXPValue * Config.getEntityXpMultiplier());
                case ON -> {
                    Integer configXp = Config.getEntityXp(Registries.ENTITY_TYPE.getRawId(EntityType.EXPERIENCE_BOTTLE));
                    yield Math.round((configXp != null ? configXp : defaultXPValue) * Config.getEntityXpMultiplier());
                }
                case OFF -> 0;
            };
        }
        return defaultXPValue;
    }
}