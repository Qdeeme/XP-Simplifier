package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import qdeeme.xp_simplifier.util.Config;




public class OnEntityKill {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/OnEntityKill");

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {

			if (!Config.isEntityKillXpEnabled()) {
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
			int xp = 0;

		// If entity kill XP is enabled, use config system
		if (Config.isEntityKillXpEnabled()) {
			if (Config.hasEntityXpConfig(entityId)) {
				xp = Config.getEntityXp(entityId);
			} else {
				// Entity not in config, give vanilla
				xp = entity.getXpToDrop();
			}
				if (xp > 0) {
					serverPlayer.addExperience(xp);
				}
			}
		});

		LOGGER.info("Registered entity kill XP handler");
	}
}