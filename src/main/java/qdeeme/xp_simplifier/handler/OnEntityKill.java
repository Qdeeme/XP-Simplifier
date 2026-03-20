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
				// Check if entity has custom config
				if (Config.hasEntityXpConfig(entityId)) {
					// Use config value (could be 0 to disable, or positive for custom)
					xp = Config.getEntityXp(entityId);
					LOGGER.debug("Using config XP for {}: {}", entityId, xp);
				} else {
					// Entity not in config, fallback to vanilla XP
					xp = entity.getXpToDrop();
					LOGGER.debug("Using vanilla XP for {}: {}", entityId, xp);
				}
			} else {
				// Entity kill XP is disabled, always use vanilla
				xp = entity.getXpToDrop();
			}

			// Give XP if any
			if (xp > 0) {
				serverPlayer.addExperience(xp);
			}
		});

		LOGGER.info("Registered entity kill XP handler");
	}
}