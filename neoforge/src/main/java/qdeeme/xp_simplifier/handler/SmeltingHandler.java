package qdeeme.xp_simplifier.handler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qdeeme.xp_simplifier.mixin.accessors.CookingRecipeXpAccessor;
import qdeeme.xp_simplifier.util.config.XpsConfig;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class SmeltingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/SmeltingHandler");

    private static MinecraftServer currentServer = null;

    /**
     * Stores original vanilla XP values per recipe instance.
     * Prevents XP multiplying on runtime reloads.
     */
    private static final Map<AbstractCookingRecipe, Float> ORIGINAL_XP = new IdentityHashMap<>();

    public static void register() {
        LOGGER.info("Registered smelting XP handler");

        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
            currentServer = event.getServer();

            // Fresh server = fresh recipe instances
            clearOriginalXpCache();
            reloadRecipeXp();
        });
    }

    /**
     * Clear cached original XP values.
     * Useful for datapack reloads (/reload).
     */
    public static void clearOriginalXpCache() {
        ORIGINAL_XP.clear();
        LOGGER.info("Cleared recipes XP cache");
    }

    /**
     * Reload all recipe XP values from current config.
     * Safe to call multiple times at runtime.
     */
    public static void reloadRecipeXp() {
        if (currentServer == null) {
            LOGGER.warn("Cannot reload recipe XP - server not started");
            return;
        }

        RecipeManager recipeManager = currentServer.getRecipeManager();

        int modified = 0;

        var cookingTypes = List.of(
                RecipeType.SMELTING,
                RecipeType.BLASTING,
                RecipeType.SMOKING,
                RecipeType.CAMPFIRE_COOKING
        );
        for (var recipeType : cookingTypes) {
            for (var recipeHolder : recipeManager.getAllRecipesFor(recipeType)) {
                AbstractCookingRecipe recipe = recipeHolder.value();
                ItemStack result = recipe.getResultItem(currentServer.registryAccess());
                if (result.isEmpty()) {
                    continue;
                }
                CookingRecipeXpAccessor accessor = (CookingRecipeXpAccessor) recipe;

                /*
                 * Save original XP once.
                 * Never read mutated values again.
                 */
                float baseXp = ORIGINAL_XP.computeIfAbsent(recipe, r -> accessor.getExperience());
                switch (XpsConfig.getSmeltingXpModeEnum()) {

                    case ON -> {
                        String productId = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
                        float overrideXp = XpsConfig.getRecipeXp(productId);
                        float finalXp = (overrideXp >= 0 ? overrideXp : baseXp) * XpsConfig.getSmeltingXpMultiplier();

                        accessor.setExperience(finalXp);
                        modified++;
                    }
                    case VANILLA -> {
                        float finalXp = baseXp * XpsConfig.getSmeltingXpMultiplier();
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