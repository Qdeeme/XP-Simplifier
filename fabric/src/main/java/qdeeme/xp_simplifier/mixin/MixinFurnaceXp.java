package qdeeme.xp_simplifier.mixin;


import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;


@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinFurnaceXp {

    @Shadow
    @Final
    private Object2IntOpenHashMap<Identifier> recipesUsed;


    /**
     * Intercepts dropExperienceForRecipesUsed and gives XP directly in non-vanilla orb modes.
     */
    @Inject(method = "dropExperienceForRecipesUsed", at = @At("HEAD"), cancellable = true)
    private void giveDirectXp(ServerPlayerEntity serverPlayer, CallbackInfo ci) {
        if (Config.getOrbModeEnum() == OrbMode.VANILLA) {
            return;
        }

        // Calculate total XP from all recipes used
        float totalXp = 0.0f;

        if (this.recipesUsed != null && !this.recipesUsed.isEmpty()) {
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;

            for (Object2IntMap.Entry<Identifier> entry : this.recipesUsed.object2IntEntrySet()) {
                Identifier recipeId = entry.getKey();
                int count = entry.getIntValue();

                var recipeEntry = furnace.getWorld().getServer().getRecipeManager().get(recipeId).orElse(null);
                if (recipeEntry == null) {
                    continue;
                }
                Recipe<?> recipe = recipeEntry.value();
                if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                    totalXp += cookingRecipe.getExperience() * count;
                }
            }
        }

        serverPlayer.addExperience(MathHelper.floor(totalXp));
        this.recipesUsed.clear();
        ci.cancel();
    }
}