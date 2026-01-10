package qdeeme.xp_simplifier.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import qdeeme.xp_simplifier.util.Config;

import java.util.UUID;

public class ExperienceBottleHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/ExperienceBottleHandler");

	public static void register() {
		// Listen for experience bottle entities being loaded/spawned
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ExperienceBottleEntity bottle) {
				handleExperienceBottle(bottle, (ServerWorld) world);
			}
		});

		LOGGER.info("Registered experience bottle XP handler");
	}

	private static void handleExperienceBottle(ExperienceBottleEntity bottle, ServerWorld world) {
		// We need to intercept when the bottle breaks
		// See MixinExperienceBottle for the actual implementation
	}

	public static void onBottleBreak(ExperienceBottleEntity bottle, HitResult hitResult) {
		if (!Config.isEntityKillXpEnabled()) {
			return;
		}

		try {
			// Get the owner UUID from NBT
			NbtCompound nbt = new NbtCompound();
			bottle.writeNbt(nbt);

			if (nbt.contains("Owner")) {
				int[] ownerUuidArray = nbt.getIntArray("Owner");
				
				if (ownerUuidArray.length == 4) {
					// Convert int array to UUID
					UUID ownerUuid = new UUID(
						((long) ownerUuidArray[0] << 32) | (ownerUuidArray[1] & 0xFFFFFFFFL),
						((long) ownerUuidArray[2] << 32) | (ownerUuidArray[3] & 0xFFFFFFFFL)
					);

					// Find the player
					ServerWorld world = (ServerWorld) bottle.getWorld();
					ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);

					if (owner != null) {
						// Get XP from config (use a special entry or fixed value)
						int xp = Config.getEntityXp("minecraft:experience_bottle");
						
						if (xp <= 0) {
							// Default XP if not configured (vanilla gives 3-11)
							xp = 3 + world.random.nextInt(9);
						}

						owner.addExperience(xp);
						LOGGER.debug("Awarded {} XP to {} from experience bottle", xp, owner.getName().getString());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to handle experience bottle XP", e);
		}
	}
}