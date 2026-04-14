package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

@Mixin(LivingEntity.class)
public abstract class MixinEntityXp {

    private static OrbMode ORBMODE;
    private static XpMode  ENTITYXPMODE;

    // "vanilla" mode: overwrite the XP amount returned before the orb is spawned
    @Inject(method = "getXpToDrop", at = @At("RETURN"), cancellable = true)
    private void modifyEntitiesXP(ServerWorld world, Entity attacker, CallbackInfoReturnable<Integer> cir) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (ENTITYXPMODE == null) {
            ENTITYXPMODE = Config.getEntityXpModeEnum();
        }

        if ((LivingEntity)(Object)this instanceof EnderDragonEntity dragon && ENTITYXPMODE == XpMode.ON) {
            EnderDragonFight dragonFight = ((ServerWorld) dragon.getWorld()).getEnderDragonFight();
            if (dragonFight != null && dragonFight.hasPreviouslyKilled()) {
                int dragonRespawnedXP = Config.getEntityXp("minecraft:ender_dragon_respawned");
                cir.setReturnValue(dragonRespawnedXP >= 0 ? dragonRespawnedXP : cir.getReturnValue());
                return;
            } else {
                int dragonXP = Config.getEntityXp("minecraft:ender_dragon_first");
                cir.setReturnValue(dragonXP >= 0 ? dragonXP : cir.getReturnValue());
                return;
            }
        }

        if (ORBMODE == OrbMode.VANILLA) {
            String entityId = Registries.ENTITY_TYPE.getId(((LivingEntity)(Object)this).getType()).toString();
            switch (ENTITYXPMODE) {
                case VANILLA:
                    break;
                case ON:
                    int configXp = Config.getEntityXp(entityId);
                    int totalXP = configXp >= 0 ? configXp : cir.getReturnValue();
                    cir.setReturnValue(totalXP);
                    break;
                case OFF:
                    cir.setReturnValue(0);
                    cir.cancel();
                    break;
                }
            }
        }
        
    }

