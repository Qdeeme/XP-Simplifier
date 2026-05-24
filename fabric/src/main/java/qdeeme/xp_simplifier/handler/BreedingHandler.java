package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.XpMode;


public class BreedingHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/BreedingHandler");

    public static void register() {
        LOGGER.info("Registered Breeding XP Handler");
    }

    public static void onSimpleBreed(ServerWorld world, ServerPlayerEntity player, AnimalEntity babyEntity) {
        if (Config.getBreedingXpModeEnum() == XpMode.OFF) {
            return;
        }
        int rawId = Registries.ENTITY_TYPE.getRawId(babyEntity.getType());
        switch (Config.getBreedingXpModeEnum()) {
            case ON:
                Integer configXp = Config.getBreedingXp(rawId);
                int base = configXp != null ? configXp : (1 + babyEntity.getRandom().nextInt(7));
                player.addExperience(Math.round(base * Config.getBreedingXpMultiplier()));
                break;
            case VANILLA:
                player.addExperience(Math.round((1 + babyEntity.getRandom().nextInt(7)) * Config.getBreedingXpMultiplier()));
                break;
        }
    }
}