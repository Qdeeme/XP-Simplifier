package qdeeme.xp_simplifier.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import qdeeme.xp_simplifier.util.config.XpsConfig;

public class FishingHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/FishingHandler");


    public static void register() {
        LOGGER.info("Registered Fishing XP Handler");
    }

    public static void onFishCaught(Player player, List<ItemStack> caughtItems) {
        switch (XpsConfig.getFishingXpModeEnum()) {
            case ON:
                for (ItemStack stack : caughtItems) {
                    int rawId = BuiltInRegistries.ITEM.getId(stack.getItem());
                    Integer catchXp = XpsConfig.getFishingXp(rawId);
                    int xp = catchXp != null ? catchXp : (1 + player.getRandom().nextInt(6));
                    player.giveExperiencePoints(Math.round(xp * XpsConfig.getFishingXpMultiplier()));
                }
                break;
            case VANILLA:
                Integer globalXp = XpsConfig.getGlobalFishingXp();
                int base = globalXp != null ? globalXp : (1 + player.getRandom().nextInt(6));
                player.giveExperiencePoints(Math.round(base * XpsConfig.getFishingXpMultiplier()));
                break;
            case OFF:
                break;
        }
    }
}