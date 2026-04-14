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
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

@Mixin(AnimalEntity.class)
public abstract class MixinBreeding {

    private static OrbMode ORBMODE;
    private static XpMode BREEDINGXPMODE;

    // "simple" mode: cancel orb via MixinServerWorld, call handler
    @Inject(method = "breed", at = @At("TAIL"), cancellable = false)
    private void onSimpleBreed(ServerWorld world, AnimalEntity babyEntity, CallbackInfo ci) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (ORBMODE != OrbMode.SIMPLE) {
            return;
        }

        LovingPlayerAccessor lovingAccessor = (LovingPlayerAccessor) (Object) this;
        UUID lovingPlayerUuid = lovingAccessor.getLovingPlayer();

        if (lovingPlayerUuid != null) {
            ServerPlayerEntity lovingPlayer = world.getServer().getPlayerManager().getPlayer(lovingPlayerUuid);
            if (lovingPlayer != null) {
                BreedingHandler.onSimpleBreed(world, lovingPlayer, babyEntity);
            }
        }
    }

    // "vanilla" mode: intercept the spawnEntity call in breed and target orbs
    @Redirect(
        method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;Lnet/minecraft/entity/passive/PassiveEntity;)V",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z")
    )
    private boolean redirectBreedingXpValue(ServerWorld world, Entity entity) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (BREEDINGXPMODE == null) {
            BREEDINGXPMODE = Config.getBreedingXpModeEnum();
        }

        if (entity instanceof ExperienceOrbEntity orb) {
            if (ORBMODE == OrbMode.VANILLA) {
                int vanillaXP = 1 + world.random.nextInt(6);
                switch (BREEDINGXPMODE) {
                    case ON:
                        String entityId = Registries.ENTITY_TYPE.getId(((AnimalEntity)(Object)this).getType()).toString();
                        int configXp = Config.getBreedingXp(entityId);
                        int totalXP = configXp >= 0 ? configXp : vanillaXP;
                        ExperienceOrbEntity.spawn(world, orb.getPos(), totalXP);
                        return true;
                    case VANILLA:
                        ExperienceOrbEntity.spawn(world, orb.getPos(), vanillaXP);
                        return true;
                    case OFF:
                        return false;
                    default:
                        return true;
                }
            }
        }
        // breedingXpMode == "vanilla" or  fallback
        return world.spawnEntity(entity);
    }
}
