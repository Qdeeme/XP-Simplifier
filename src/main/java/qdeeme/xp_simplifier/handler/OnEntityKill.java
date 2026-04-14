package qdeeme.xp_simplifier.handler;


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
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;




public class OnEntityKill {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/OnEntityKill");
	private static final OrbMode ORBMODE    = Config.getOrbModeEnum();
	private static final XpMode  ENTITYMODE = Config.getEntityXpModeEnum();

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
			if (ORBMODE != OrbMode.SIMPLE) {
				return;
			}

			if (ENTITYMODE == XpMode.OFF) {
				return;
			}
			// Check if killed by a player
			if (!(source.getAttacker() instanceof ServerPlayerEntity serverPlayer)) {
				return;
			}

			// Don't give XP in creative or spectator mode
			if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
				return;
			}

			String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
			int vanillaXP = entity.getXpToDrop((ServerWorld) entity.getWorld(), source.getAttacker());
			switch (ENTITYMODE) {
				case VANILLA:
					serverPlayer.addExperience(vanillaXP);
					break;
				case ON:
					if (entity instanceof EnderDragonEntity dragon) {
            			EnderDragonFight dragonFight = ((ServerWorld) dragon.getWorld()).getEnderDragonFight();
            			if (dragonFight != null && dragonFight.hasPreviouslyKilled()) {
                			int dragonRespawnedXP = Config.getEntityXp("minecraft:ender_dragon_respawned");
               				serverPlayer.addExperience(dragonRespawnedXP >= 0 ? dragonRespawnedXP : vanillaXP);
							return;
						} else {
							int dragonXP = Config.getEntityXp("minecraft:ender_dragon_first");
							serverPlayer.addExperience(dragonXP >= 0 ? dragonXP : vanillaXP);
							return;
						}
					}
					int xp = Config.getEntityXp(entityId);
					if (xp < 0) {
						serverPlayer.addExperience(xp);
						return;
					}
					int totalXP = xp >= 0 ? xp : vanillaXP;
					serverPlayer.addExperience(totalXP);
					break;
				case OFF:
					break;
				default:
					serverPlayer.addExperience(vanillaXP);
			}
		});
		LOGGER.info("Registered entity kill XP handler");
	}
}