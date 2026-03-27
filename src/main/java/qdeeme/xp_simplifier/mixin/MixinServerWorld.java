package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qdeeme.xp_simplifier.util.Config;

@Mixin(ServerWorld.class)
public class MixinServerWorld {

    @Inject(method = "spawnEntity(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Only affect XP orbs
        if (!(entity instanceof ExperienceOrbEntity)) {
            return;
        }

        // Block all XP orbs if orbsEnabled is false (global setting)
        // Handlers are responsible for checking their own enabled flags
        if (!Config.isOrbsEnabled()) {
            cir.setReturnValue(false);
        }
    }
}