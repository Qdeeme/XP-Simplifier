package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;
import qdeeme.xp_simplifier.mixin.CookingRecipeXpAccessor;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.XpMode;



public class SmeltingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/SmeltingHandler");
    private static final XpMode SMELTINGMODE = Config.getSmeltingXpModeEnum();

    public static void register() {
        LOGGER.info("Registered smelting XP handler");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

            RecipeManager recipeManager = server.getRecipeManager();
            int modified = 0;

            for (var recipeEntry : recipeManager.values()) {
                Recipe<?> recipe = recipeEntry.value();
                if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                    ItemStack result = cookingRecipe.getResult(server.getRegistryManager());
                    if (result.isEmpty()) {
                        continue;
                    }
                    
                    CookingRecipeXpAccessor accessor = (CookingRecipeXpAccessor) cookingRecipe;
                    float defaultXp = accessor.getExperience();
                    switch (SMELTINGMODE) {
                        case OFF:
                            accessor.setExperience(0);
                            modified++;
                            break;
                        case VANILLA:
                            accessor.setExperience(defaultXp);
                            break;
                        case ON:
                            String productId = Registries.ITEM.getId(result.getItem()).toString();
                            float override = Config.getRecipeXp(productId);
                            if (override >= 0) {
                                accessor.setExperience(override);
                                modified++;
                            }
                            break;
                    }
                }
            }
        });
    }
}