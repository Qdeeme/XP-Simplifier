package qdeeme.xp_simplifier.mixin;


import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;


@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinFurnaceXp {

    @Shadow
    @Final
    private Object2IntOpenHashMap<ResourceLocation> recipesUsed;

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"), cancellable = true)
    private void giveDirectXp(ServerPlayer serverPlayer, CallbackInfo ci) {
        if (XpsConfig.getOrbModeEnum() == OrbMode.VANILLA) {
            return;
        }

        // Calculate total XP from all recipes used
        float totalXp = 0.0f;

        if (this.recipesUsed != null && !this.recipesUsed.isEmpty()) {
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;

            for (Object2IntMap.Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
                ResourceLocation recipeId = entry.getKey();
                int timesUsed = entry.getIntValue();

                var recipeEntry = furnace.getLevel().getServer().getRecipeManager().byKey(recipeId).orElse(null);
                if (recipeEntry == null) {
                    continue;
                }
                Recipe<?> recipe = recipeEntry.value();
                if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                    totalXp += cookingRecipe.getExperience() * timesUsed;
                }
            }
        }

        serverPlayer.giveExperiencePoints(Mth.floor(totalXp));
        this.recipesUsed.clear();
        ci.cancel();
    }
}
