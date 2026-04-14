package qdeeme.xp_simplifier.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.XpMode;

public class FishingHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/FishingHandler");
    private static final XpMode FISHINGMODE = Config.getFishingXpModeEnum();

    public static void register() {
        LOGGER.info("Registered Fishing XP Handler");
    }

    public static void onFishCaught(PlayerEntity player, List<ItemStack> caughtItems) {
        
        switch (FISHINGMODE) {
            case VANILLA:
                int xp = Config.getFishingXp("Global_Fishing");
                player.addExperience(xp);
                break;
            case ON:
                for (ItemStack stack : caughtItems) {
                    String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                    int catchXp = Config.getFishingXp(itemId);
                    if (catchXp < 0) {
                        player.addExperience(catchXp);
                        break;
                    }
                    int xpAmount = catchXp >= 0 ? catchXp : (1 + player.getRandom().nextInt(6));
                    player.addExperience(xpAmount);
                }
                break;
            case OFF:
                break;
        }
    }
}