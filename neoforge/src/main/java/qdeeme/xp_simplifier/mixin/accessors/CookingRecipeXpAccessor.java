package qdeeme.xp_simplifier.mixin.accessors;


import net.minecraft.world.item.crafting.AbstractCookingRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(AbstractCookingRecipe.class)
public interface CookingRecipeXpAccessor {
    
    @Accessor("experience")
    float getExperience();
    
    @Mutable
    @Accessor("experience")
    void setExperience(float experience);
}