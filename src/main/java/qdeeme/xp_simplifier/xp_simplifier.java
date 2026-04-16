package qdeeme.xp_simplifier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import qdeeme.xp_simplifier.handler.BlockBreakHandler;
import qdeeme.xp_simplifier.handler.BreedingHandler;
import qdeeme.xp_simplifier.handler.ExperienceBottleHandler;
import qdeeme.xp_simplifier.handler.FishingHandler;
import qdeeme.xp_simplifier.handler.OnEntityKill;
import qdeeme.xp_simplifier.handler.SmeltingHandler;
import qdeeme.xp_simplifier.handler.TradingHandler;
import qdeeme.xp_simplifier.util.Config;


public class xp_simplifier implements ModInitializer {
    public static final String MOD_ID = "xp_simplifier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing " + MOD_ID);
        
        Config.load();

        
        // Register handlers
        BlockBreakHandler.register();
        ExperienceBottleHandler.register();
        OnEntityKill.register();
        SmeltingHandler.register();
        TradingHandler.register();
        BreedingHandler.register();
        FishingHandler.register();
        LOGGER.info("Successfully initialized " + MOD_ID);
    }
}