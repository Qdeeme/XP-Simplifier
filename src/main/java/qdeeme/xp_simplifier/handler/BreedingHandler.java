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
    private static final XpMode BREEDINMODE = Config.getBreedingXpModeEnum();

    public static void register () {
        LOGGER.info("Registered Breeding XP Handler");
    }

    public static void onSimpleBreed(ServerWorld world, ServerPlayerEntity player, AnimalEntity babyEntity) {
        if (BREEDINMODE == XpMode.OFF) {
            return;
        }
        int rawId = Registries.ENTITY_TYPE.getRawId(babyEntity.getType());
        switch (BREEDINMODE) {
        case VANILLA:
            player.addExperience(1 + babyEntity.getRandom().nextInt(7));
            return;
        case ON:
            Integer configXp = Config.getBreedingXp(rawId);
            player.addExperience(configXp != null ? configXp : (1 + babyEntity.getRandom().nextInt(7)));
            break;
        case OFF:
            break;
        }
    }
}