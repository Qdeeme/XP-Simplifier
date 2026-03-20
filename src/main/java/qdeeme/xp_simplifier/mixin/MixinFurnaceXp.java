package qdeeme.xp_simplifier.mixin;


import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinFurnaceXp {

    @Shadow
    @Final
    private Object2IntOpenHashMap<Identifier> recipesUsed;


    /**
     * Intercepts dropExperience and gives XP directly instead of spawning orbs.
     */
    @Inject(method = "dropExperienceForRecipesUsed", at = @At("HEAD"), cancellable = true)
    private void giveDirectXp(ServerPlayerEntity serverPlayer, CallbackInfo ci) {

        // Calculate total XP from all recipes used
        float totalXp = 0.0f;
        
        if (this.recipesUsed != null && !this.recipesUsed.isEmpty()) {
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
            
            for (Object2IntMap.Entry<Identifier> entry : this.recipesUsed.object2IntEntrySet()) {
                Identifier recipeId = entry.getKey();
                int count = entry.getIntValue();
                
                Recipe<?> recipe = furnace.getWorld().getServer().getRecipeManager().get(recipeId).orElse(null);
                
                if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                    totalXp += cookingRecipe.getExperience() * count;
                }
            }
        }

        if (totalXp > 0) {
            // Round to nearest integer
            int xpAmount = MathHelper.floor(totalXp);
            
            // Give XP directly to player
            serverPlayer.addExperience(xpAmount);
        }

        // Clear the recipes used
        this.recipesUsed.clear();
    }
}