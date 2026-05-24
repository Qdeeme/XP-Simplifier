package qdeeme.xp_simplifier.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(ServerLevel.class)
public class MixinServerWorld {

    @Inject(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void cancelAllOrbs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ExperienceOrb)) return;
        // "simple": suppress all XP orbs — handlers call giveExperiencePoints() directly
        // "vanilla": orbs spawn normally but with values overwritten at source via @ModifyArg
        if (XpsConfig.getOrbModeEnum() == OrbMode.SIMPLE) {
            cir.setReturnValue(false);
        }
    }
}