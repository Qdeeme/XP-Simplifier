package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import qdeeme.xp_simplifier.handler.BreedingHandler;
import qdeeme.xp_simplifier.mixin.accessor.LovingPlayerAccessor;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(AnimalEntity.class)
public abstract class MixinBreeding {

    // "simple" mode: orb cancelled by MixinServerWorld; handler awards XP directly
    @Inject(method = "breed", at = @At("TAIL"))
    private void onSimpleBreed(ServerWorld world, AnimalEntity babyEntity, CallbackInfo ci) {
        if (Config.getOrbModeEnum() != OrbMode.SIMPLE) return;

        LovingPlayerAccessor lovingAccessor = (LovingPlayerAccessor) this;
        UUID lovingPlayerUuid = lovingAccessor.getLovingPlayer();
        if (lovingPlayerUuid != null) {
            ServerPlayerEntity lovingPlayer = world.getServer().getPlayerManager().getPlayer(lovingPlayerUuid);
            if (lovingPlayer != null) {
                BreedingHandler.onSimpleBreed(world, lovingPlayer, babyEntity);
            }
        }
    }

    // "vanilla" mode: intercept the spawnEntity call and replace the orb with the configured XP amount
    @Redirect(
            method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;Lnet/minecraft/entity/passive/PassiveEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z")
    )
    private boolean redirectBreedingXpValue(ServerWorld world, Entity entity) {
        if (entity instanceof ExperienceOrbEntity orb) {
            if (Config.getOrbModeEnum() == OrbMode.VANILLA) {
                int vanillaXP = 1 + world.random.nextInt(7);
                switch (Config.getBreedingXpModeEnum()) {
                    case ON:
                        int rawId = Registries.ENTITY_TYPE.getRawId(((AnimalEntity) (Object) this).getType());
                        Integer configXp = Config.getBreedingXp(rawId);
                        int totalXP = configXp != null ? configXp : vanillaXP;
                        ExperienceOrbEntity.spawn(world, orb.getPos(), Math.round(totalXP * Config.getBreedingXpMultiplier()));
                        return true;
                    case VANILLA:
                        ExperienceOrbEntity.spawn(world, orb.getPos(), Math.round(vanillaXP * Config.getBreedingXpMultiplier()));
                        return true;
                    case OFF:
                        return false;
                    default:
                        return true;
                }
            }
        }
        return world.spawnEntity(entity);
    }
}
