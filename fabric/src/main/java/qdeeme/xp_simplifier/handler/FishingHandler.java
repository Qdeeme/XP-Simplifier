package qdeeme.xp_simplifier.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import qdeeme.xp_simplifier.util.Config;

public class FishingHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/FishingHandler");

    public static void register() {
        LOGGER.info("Registered Fishing XP Handler");
    }

    public static void onFishCaught(PlayerEntity player, List<ItemStack> caughtItems) {
        switch (Config.getFishingXpModeEnum()) {
            case ON:
                for (ItemStack stack : caughtItems) {
                    int rawId = Registries.ITEM.getRawId(stack.getItem());
                    Integer catchXp = Config.getFishingXp(rawId);
                    int xp = catchXp != null ? catchXp : (1 + player.getRandom().nextInt(6));
                    player.addExperience(Math.round(xp * Config.getFishingXpMultiplier()));
                }
                break;
            case VANILLA:
                Integer globalXp = Config.getGlobalFishingXp();
                int base = globalXp != null ? globalXp : (1 + player.getRandom().nextInt(6));
                player.addExperience(Math.round(base * Config.getFishingXpMultiplier()));
                break;
            case OFF:
                break;
        }
    }
}