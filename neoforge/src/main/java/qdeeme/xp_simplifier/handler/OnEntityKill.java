package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;



public class OnEntityKill {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/OnEntityKill");


    public static void register() {
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, event -> {
            if (XpsConfig.getOrbModeEnum() != OrbMode.SIMPLE || XpsConfig.getEntityXpModeEnum() == XpMode.OFF) {
                return;
            }

            LivingEntity entity = event.getEntity();
            DamageSource source = event.getSource();

            if (!(source.getEntity() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
                return;
            }

            int vanillaXP = entity.getExperienceReward((ServerLevel) entity.level(), source.getEntity());
            switch (XpsConfig.getEntityXpModeEnum()) {
                case ON:
                    if (entity instanceof EnderDragon dragon) {
                        var dragonFight = ((ServerLevel) dragon.level()).getDragonFight();
                        boolean respawned = dragonFight != null && dragonFight.hasPreviouslyKilledDragon();
                        Integer configXp = XpsConfig.getDragonXp(respawned);
                        serverPlayer.giveExperiencePoints(Math.round((configXp != null ? configXp : vanillaXP) * XpsConfig.getEntityXpMultiplier()));
                    } else {
                        Integer configXp = XpsConfig.getEntityXp(BuiltInRegistries.ENTITY_TYPE.getId(entity.getType()));
                        serverPlayer.giveExperiencePoints(Math.round((configXp != null ? configXp : vanillaXP) * XpsConfig.getEntityXpMultiplier()));
                    }
                    break;
                case VANILLA:
                default:
                    serverPlayer.giveExperiencePoints(Math.round(vanillaXP * XpsConfig.getEntityXpMultiplier()));
            }
        });
        LOGGER.info("Registered entity kill XP handler");
    }
}