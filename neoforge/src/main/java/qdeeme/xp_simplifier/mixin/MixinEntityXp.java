package qdeeme.xp_simplifier.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(LivingEntity.class)
public abstract class MixinEntityXp {

    @ModifyArg(
            method = "dropExperience",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
            index = 2
    )
    private int modifyEntityXpOrb(int defaultValue) {
        if (XpsConfig.getOrbModeEnum() != OrbMode.VANILLA) {
            return defaultValue;
        }

        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof EnderDragon dragon) {
            var dragonFight = ((ServerLevel) dragon.level()).getDragonFight();
            boolean respawned = dragonFight != null && dragonFight.hasPreviouslyKilledDragon();
            return switch (XpsConfig.getEntityXpModeEnum()) {
                case ON -> {
                    Integer dragonXp = XpsConfig.getDragonXp(respawned);
                    yield Math.round((dragonXp != null ? dragonXp : defaultValue) * XpsConfig.getEntityXpMultiplier());
                }
                case OFF -> 0;
                default -> Math.round(defaultValue * XpsConfig.getEntityXpMultiplier());
            };
        }

        return switch (XpsConfig.getEntityXpModeEnum()) {
            case VANILLA -> Math.round(defaultValue * XpsConfig.getEntityXpMultiplier());
            case ON -> {
                Integer configXp = XpsConfig.getEntityXp(BuiltInRegistries.ENTITY_TYPE.getId(entity.getType()));
                yield Math.round((configXp != null ? configXp : defaultValue) * XpsConfig.getEntityXpMultiplier());
            }
            case OFF -> 0;
        };
    }
}

