package qdeeme.xp_simplifier.mixin.accessors;

import net.minecraft.world.inventory.GrindstoneMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public interface GrindstoneSlotAccessor {

    @Accessor("this$0")
    GrindstoneMenu getOuterHandler();
}
