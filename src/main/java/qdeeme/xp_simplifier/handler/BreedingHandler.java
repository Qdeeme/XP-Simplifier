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
        String entityTypeId = Registries.ENTITY_TYPE.getId(babyEntity.getType()).toString();
        switch (BREEDINMODE) {
        case VANILLA:
            player.addExperience(1 + babyEntity.getRandom().nextInt(7));
            return;
        case ON:
            int xp = Config.getBreedingXp(entityTypeId);
            if (xp < 0) {
                player.addExperience(xp);
                return;
            }
            int xpAmount = xp >= 0 ? xp : (1 + babyEntity.getRandom().nextInt(7));
            player.addExperience(xpAmount);
            break;
        case OFF:
            break;
        }
    }
}