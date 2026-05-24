package qdeeme.xp_simplifier.handler;


import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;
import qdeeme.xp_simplifier.mixin.accessor.CookingRecipeXpAccessor;
import qdeeme.xp_simplifier.util.Config;

import java.util.IdentityHashMap;
import java.util.Map;


public class SmeltingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/SmeltingHandler");
    private static MinecraftServer currentServer = null;
    private static final Map<AbstractCookingRecipe, Float> ORIGINAL_XP = new IdentityHashMap<>();

    public static void register() {
        LOGGER.info("Registered smelting XP handler");

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            clearOriginalXpCache();
            reloadRecipeXp();
        });
    }

    public static void clearOriginalXpCache() {
        ORIGINAL_XP.clear();
        LOGGER.info("Cleared recipes XP cache");
    }

    /**
     * Reload all recipe XP values from current config.
     * Call this when config changes at runtime.
     */
    public static void reloadRecipeXp() {


        MinecraftServer server = currentServer;
        RecipeManager recipeManager = server.getRecipeManager();
        int modified = 0;

        for (var recipeEntry : recipeManager.values()) {
            Recipe<?> recipe = recipeEntry.value();
            if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                ItemStack result = cookingRecipe.getResult(server.getRegistryManager());
                if (result.isEmpty()) continue;
                CookingRecipeXpAccessor accessor = (CookingRecipeXpAccessor) cookingRecipe;


                float baseXp = ORIGINAL_XP.computeIfAbsent(cookingRecipe, r -> accessor.getExperience());


                switch (Config.getSmeltingXpModeEnum()) {
                    case ON -> {
                        String productId = Registries.ITEM.getId(result.getItem()).toString();
                        float overrideXp = Config.getRecipeXp(productId);
                        float finalXp = (overrideXp >= 0 ? overrideXp : baseXp) * Config.getSmeltingXpMultiplier();
                        accessor.setExperience(finalXp);
                        modified++;
                    }
                    case VANILLA -> {
                        float finalXp = baseXp * Config.getSmeltingXpMultiplier();
                        accessor.setExperience(finalXp);
                        modified++;
                    }
                    case OFF -> {
                        accessor.setExperience(0.0F);
                        modified++;
                    }
                }
            }
        }
        LOGGER.info("Reloaded {} smelting recipe XP values", modified);
    }
}