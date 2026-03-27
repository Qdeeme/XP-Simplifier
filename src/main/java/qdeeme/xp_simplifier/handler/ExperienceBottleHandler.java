package qdeeme.xp_simplifier.handler;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import qdeeme.xp_simplifier.util.Config;


public class ExperienceBottleHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/ExperienceBottleHandler");

	public static void register() {
		LOGGER.info("Registered experience bottle XP handler");
	}

	public static void onBottleBreak(ExperienceBottleEntity bottle, HitResult hitResult) {
		if (!Config.isEntityKillXpEnabled()) {
			return;
		}

		try {
			NbtCompound nbt = new NbtCompound();
			bottle.writeNbt(nbt);

			UUID ownerUuid = null;

			if (nbt.contains("Owner")) {
				int[] ownerUuidArray = nbt.getIntArray("Owner");
				
				if (ownerUuidArray.length == 4) {
					// Convert int array to UUID
					ownerUuid = new UUID(
						((long) ownerUuidArray[0] << 32) | (ownerUuidArray[1] & 0xFFFFFFFFL),
						((long) ownerUuidArray[2] << 32) | (ownerUuidArray[3] & 0xFFFFFFFFL)
					);
				}
			}

			if (ownerUuid == null) {
				return;
			}

			// Find the player
			ServerWorld world = (ServerWorld) bottle.getWorld();
			ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);

			if (owner != null) {
				int xp = Config.getEntityXp("minecraft:experience_bottle");
				
				if (xp <= 0) {
					// Default XP if not configured (vanilla gives 3-11)
					xp = 3 + world.random.nextInt(9);
				}

				owner.addExperience(xp);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to handle experience bottle XP", e);
		}
	}
}