package qdeeme.xp_simplifier.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;
import qdeeme.xp_simplifier.mixin.accessors.ProjectileOwnerAccessor;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.XpMode;


public class ExperienceBottleHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/ExperienceBottleHandler");
    private static int BOTTLE_ENTITY_RAW_ID = -1;

    public static void register() {
        BOTTLE_ENTITY_RAW_ID = BuiltInRegistries.ENTITY_TYPE.getId(EntityType.EXPERIENCE_BOTTLE);
        LOGGER.info("Registered experience bottle XP handler");
    }

    public static void onBottleBreak(ThrownExperienceBottle bottle, HitResult hitResult) {
        if (XpsConfig.getEntityXpModeEnum() == XpMode.OFF) {
            return;
        }

        UUID ownerUuid = ((ProjectileOwnerAccessor) bottle).getOwnerUuid();
        if (ownerUuid == null) {
            return;
        }

        ServerLevel world = (ServerLevel) bottle.level();
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null) {
            int vanillaXp = 3 + owner.getRandom().nextInt(9);
            switch (XpsConfig.getEntityXpModeEnum()) {
                case ON:
                    Integer configXp = XpsConfig.getEntityXp(BOTTLE_ENTITY_RAW_ID);
                    owner.giveExperiencePoints(configXp != null ? configXp : vanillaXp);
                    break;
                case VANILLA:
                    owner.giveExperiencePoints(vanillaXp);
                    break;
            }
        }
    }
}