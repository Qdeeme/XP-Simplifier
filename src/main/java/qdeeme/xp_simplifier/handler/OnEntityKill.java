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

			int vanillaXP = entity.getXpToDrop();
			switch (ENTITYMODE) {
				case VANILLA:
					serverPlayer.addExperience(vanillaXP);
					break;
				case ON:
					if (entity instanceof EnderDragonEntity dragon) {
						EnderDragonFight dragonFight = ((ServerWorld) dragon.getWorld()).getEnderDragonFight();
						boolean respawned = dragonFight != null && dragonFight.hasPreviouslyKilled();
						Integer configXp = Config.getDragonXp(respawned);
						serverPlayer.addExperience(configXp != null ? configXp : vanillaXP);
					} else {
						Integer configXp = Config.getEntityXp(Registries.ENTITY_TYPE.getRawId(entity.getType()));
						serverPlayer.addExperience(configXp != null ? configXp : vanillaXP);

					}
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