package qdeeme.xp_simplifier.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import qdeeme.xp_simplifier.mixin.ProjectileOwnerAccessor;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.XpMode;


public class ExperienceBottleHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/ExperienceBottleHandler");
	private static final XpMode ENTITYMODE = Config.getEntityXpModeEnum();

	public static void register() {
		LOGGER.info("Registered experience bottle XP handler");
	}

	public static void onBottleBreak(ExperienceBottleEntity bottle, HitResult hitResult) {
		if (ENTITYMODE == XpMode.OFF) {
			return;
		}

		UUID ownerUuid = ((ProjectileOwnerAccessor) bottle).getOwnerUuid();
		if (ownerUuid == null) {
			return;
		}

		ServerWorld world = (ServerWorld) bottle.getWorld();
		ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
		if (owner != null) {
			switch (ENTITYMODE) {
				case VANILLA:
					owner.addExperience(3 + owner.getRandom().nextInt(9));
					break;
				case ON:
					int xp = Config.getEntityXp("minecraft:experience_bottle");
					if (xp < 0) {
						owner.addExperience(xp);
						return;
					}
					owner.addExperience(xp >= 0 ? xp : (3 + owner.getRandom().nextInt(9)));
					break;
				case OFF:
					break;
			}
		}
	}
}