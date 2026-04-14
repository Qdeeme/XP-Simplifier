package qdeeme.xp_simplifier.mixin;

import net.minecraft.screen.GrindstoneScreenHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the outer GrindstoneScreenHandler reference stored in the anonymous
 * output slot class ($4) as field_16780.
 */
@Mixin(targets = "net.minecraft.screen.GrindstoneScreenHandler$4")
public interface GrindstoneSlotAccessor {

    @Accessor("field_16780")
    GrindstoneScreenHandler getOuterHandler();
}
