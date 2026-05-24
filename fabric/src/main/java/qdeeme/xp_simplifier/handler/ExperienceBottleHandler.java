package qdeeme.xp_simplifier.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import qdeeme.xp_simplifier.mixin.accessor.ProjectileOwnerAccessor;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.XpMode;


public class ExperienceBottleHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/ExperienceBottleHandler");
    private static int BOTTLE_ENTITY_RAW_ID = -1;

    public static void register() {
        BOTTLE_ENTITY_RAW_ID = Registries.ENTITY_TYPE.getRawId(EntityType.EXPERIENCE_BOTTLE);
        LOGGER.info("Registered experience bottle XP handler");
    }

    public static void onBottleBreak(ExperienceBottleEntity bottle, HitResult hitResult) {
        if (Config.getEntityXpModeEnum() == XpMode.OFF) return;

        UUID ownerUuid = ((ProjectileOwnerAccessor) bottle).getOwnerUuid();
        if (ownerUuid == null) return;

        ServerWorld world = (ServerWorld) bottle.getWorld();
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner != null) {
            int vanillaXp = 3 + owner.getRandom().nextInt(9);
            switch (Config.getEntityXpModeEnum()) {
                case ON:
                    Integer configXp = Config.getEntityXp(BOTTLE_ENTITY_RAW_ID);
                    owner.addExperience(configXp != null ? configXp : vanillaXp);
                    break;
                case VANILLA:
                    owner.addExperience(vanillaXp);
                    break;
                case OFF:
                    break;
            }
        }
    }
}