package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(ServerWorld.class)
public class MixinServerWorld {
    private static OrbMode ORBMODE;

    @Inject(method = "spawnEntity(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void cancelAllOrbs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }

        if (!(entity instanceof ExperienceOrbEntity)) return;
        // "simple": suppress all XP orbs — handlers call addExperience() directly
        // "vanilla": orbs spawn normally but with values overwritten at source via @ModifyArg
        if (ORBMODE == OrbMode.SIMPLE) {
            cir.setReturnValue(false);
        }
    }
}