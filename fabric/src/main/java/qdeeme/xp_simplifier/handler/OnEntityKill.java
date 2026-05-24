package qdeeme.xp_simplifier.handler;


import net.minecraft.entity.passive.MerchantEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.MerchantOffersData;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

import java.util.UUID;


public class OnEntityKill {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/OnEntityKill");

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
            if (entity instanceof MerchantEntity merchant) {
                UUID uuid = merchant.getUuid();
                MerchantOffersData.removeMerchant(uuid);
                MerchantOffersData.save();
            }
            if (Config.getOrbModeEnum() != OrbMode.SIMPLE || Config.getEntityXpModeEnum() == XpMode.OFF) {
                return;
            }
            if (!(source.getAttacker() instanceof ServerPlayerEntity serverPlayer)) return;
            if (serverPlayer.isCreative() || serverPlayer.isSpectator()) return;

            int vanillaXP = entity.getXpToDrop((ServerWorld) entity.getWorld(), source.getAttacker());
            switch (Config.getEntityXpModeEnum()) {
                case ON:
                    if (entity instanceof EnderDragonEntity dragon) {
                        EnderDragonFight dragonFight = ((ServerWorld) dragon.getWorld()).getEnderDragonFight();
                        boolean respawned = dragonFight != null && dragonFight.hasPreviouslyKilled();
                        Integer configXp = Config.getDragonXp(respawned);
                        serverPlayer.addExperience(Math.round((configXp != null ? configXp : vanillaXP) * Config.getEntityXpMultiplier()));
                    } else {
                        Integer configXp = Config.getEntityXp(Registries.ENTITY_TYPE.getRawId(entity.getType()));
                        serverPlayer.addExperience(Math.round((configXp != null ? configXp : vanillaXP) * Config.getEntityXpMultiplier()));
                    }
                    break;
                case VANILLA:
                default:
                    serverPlayer.addExperience(Math.round(vanillaXP * Config.getEntityXpMultiplier()));
            }
        });
        LOGGER.info("Registered entity kill XP handler");
    }
}