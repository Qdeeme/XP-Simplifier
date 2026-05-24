package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.XpMode;


public class BreedingHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/BreedingHandler");


    public static void register() {
        LOGGER.info("Registered Breeding XP Handler");
    }

    public static void onSimpleBreed(ServerLevel world, ServerPlayer player, Animal babyEntity) {
        if (XpsConfig.getBreedingXpModeEnum() == XpMode.OFF) {
            return;
        }
        int rawId = BuiltInRegistries.ENTITY_TYPE.getId(babyEntity.getType());
        switch (XpsConfig.getBreedingXpModeEnum()) {
            case ON:
                Integer configXp = XpsConfig.getBreedingXp(rawId);
                int base = configXp != null ? configXp : (1 + babyEntity.getRandom().nextInt(7));
                player.giveExperiencePoints(Math.round(base * XpsConfig.getBreedingXpMultiplier()));
                break;
            case VANILLA:
                player.giveExperiencePoints(Math.round((1 + babyEntity.getRandom().nextInt(7)) * XpsConfig.getBreedingXpMultiplier()));
                break;
        }
    }
}