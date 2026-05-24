package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import qdeeme.xp_simplifier.handler.BreedingHandler;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(Animal.class)
public abstract class MixinBreeding {

    // "simple" mode: cancel orb via MixinServerWorld, call handler
    @Inject(method = "spawnChildFromBreeding", at = @At("TAIL"))
    private void onSimpleBreed(ServerLevel world, Animal mateEntity, CallbackInfo ci) {
        if (XpsConfig.getOrbModeEnum() != OrbMode.SIMPLE) {
            return;
        }

        Animal self = (Animal) (Object) this;
        ServerPlayer lovingPlayer = self.getLoveCause();
        if (lovingPlayer != null) {
            BreedingHandler.onSimpleBreed(world, lovingPlayer, mateEntity);
        }
    }

    // "vanilla" mode: intercept the ExperienceOrb constructor call in finalizeSpawnChildFromBreeding
    @ModifyArg(
            method = "finalizeSpawnChildFromBreeding(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/Animal;Lnet/minecraft/world/entity/AgeableMob;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"),
            index = 4
    )
    private int redirectBreedingXpValue(int originalAmount) {
        if (XpsConfig.getOrbModeEnum() == OrbMode.VANILLA) {
            switch (XpsConfig.getBreedingXpModeEnum()) {
                case ON:
                    Animal self = (Animal) (Object) this;
                    int rawId = BuiltInRegistries.ENTITY_TYPE.getId(self.getType());
                    Integer configXp = XpsConfig.getBreedingXp(rawId);
                    return Math.round((configXp != null ? configXp : originalAmount) * XpsConfig.getBreedingXpMultiplier());
                case OFF:
                    return 0;
                case VANILLA:
                default:
                    return Math.round(originalAmount * XpsConfig.getBreedingXpMultiplier());
            }
        }
        return 0;
    }
}
