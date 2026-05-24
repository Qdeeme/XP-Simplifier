package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(LivingEntity.class)
public abstract class MixinEntityXp {

    @ModifyArg(
            method = "dropXp",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"),
            index = 2
    )
    private int modifyEntityXpOrb(int defaultValue) {
        if (Config.getOrbModeEnum() != OrbMode.VANILLA) {
            return defaultValue;
        }

        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof EnderDragonEntity dragon) {
            EnderDragonFight dragonFight = ((ServerWorld) dragon.getWorld()).getEnderDragonFight();
            boolean respawned = dragonFight != null && dragonFight.hasPreviouslyKilled();
            return switch (Config.getEntityXpModeEnum()) {
                case ON -> {
                    Integer dragonXp = Config.getDragonXp(respawned);
                    yield Math.round((dragonXp != null ? dragonXp : defaultValue) * Config.getEntityXpMultiplier());
                }
                case OFF -> 0;
                default -> Math.round(defaultValue * Config.getEntityXpMultiplier());
            };
        }

        return switch (Config.getEntityXpModeEnum()) {
            case VANILLA -> Math.round(defaultValue * Config.getEntityXpMultiplier());
            case ON -> {
                Integer configXp = Config.getEntityXp(Registries.ENTITY_TYPE.getRawId(entity.getType()));
                yield Math.round((configXp != null ? configXp : defaultValue) * Config.getEntityXpMultiplier());
            }
            case OFF -> 0;
        };
    }
}
