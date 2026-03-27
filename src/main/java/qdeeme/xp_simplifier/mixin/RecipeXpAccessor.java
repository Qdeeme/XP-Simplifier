package qdeeme.xp_simplifier.mixin;


import net.minecraft.recipe.AbstractCookingRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(AbstractCookingRecipe.class)
public interface RecipeXpAccessor {
    
    @Accessor("experience")
    float getExperience();
    
    @Mutable
    @Accessor("experience")
    void setExperience(float experience);
}