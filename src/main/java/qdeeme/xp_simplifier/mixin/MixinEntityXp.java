package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

@Mixin(LivingEntity.class)
public abstract class MixinEntityXp {

    private static OrbMode ORBMODE;
    private static XpMode  ENTITYXPMODE;

    @ModifyArg(
        method = "dropXp",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"),
        index = 2
    )
    private int modifyEntityXpOrb(int defaultValue) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (ENTITYXPMODE == null) {
            ENTITYXPMODE = Config.getEntityXpModeEnum();
        }
        if (ORBMODE != OrbMode.VANILLA) {
            return defaultValue;
        }

        LivingEntity entity = (LivingEntity)(Object)this;

        if (entity instanceof EnderDragonEntity dragon) {
            EnderDragonFight dragonFight = ((ServerWorld) dragon.getWorld()).getEnderDragonFight();
            boolean respawned = dragonFight != null && dragonFight.hasPreviouslyKilled();
            return switch (ENTITYXPMODE) {
                case ON -> {
                    Integer dragonXp = Config.getDragonXp(respawned);
                    yield dragonXp != null ? dragonXp : defaultValue;
                }
                case OFF -> 0;
                default -> defaultValue;
            };
        }

        return switch (ENTITYXPMODE) {
            case VANILLA -> defaultValue;
            case ON -> {
                Integer configXp = Config.getEntityXp(Registries.ENTITY_TYPE.getRawId(entity.getType()));
                yield configXp != null ? configXp : defaultValue;
            }
            case OFF -> 0;
        };
    }
}

