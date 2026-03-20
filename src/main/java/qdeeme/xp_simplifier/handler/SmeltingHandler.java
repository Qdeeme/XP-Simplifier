package qdeeme.xp_simplifier.handler;


import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qdeeme.xp_simplifier.mixin.RecipeXpAccess;
import qdeeme.xp_simplifier.util.Config;



public class SmeltingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/SmeltingHandler");

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!Config.isSmeltingXpEnabled()) {
                return;
            }

            RecipeManager recipeManager = server.getRecipeManager();
            int modified = 0;

            for (Recipe<?> recipe : recipeManager.values()) {
                if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                    ItemStack output = cookingRecipe.getOutput(server.getRegistryManager());
                    if (output.isEmpty()) {
                        continue;
                    }

                    String productId = Registries.ITEM.getId(output.getItem()).toString();
                    float override = Config.getRecipeXp(productId);

                    if (override >= 0) {
                        RecipeXpAccess accessor = (RecipeXpAccess) cookingRecipe;
                        float original = accessor.getExperience();
                        accessor.setExperience(override);
                        modified++;
                    }
                }
            }
        });
    }
}