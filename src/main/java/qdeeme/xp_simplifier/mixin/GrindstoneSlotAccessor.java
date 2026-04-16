package qdeeme.xp_simplifier.mixin;

import net.minecraft.screen.GrindstoneScreenHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.screen.GrindstoneScreenHandler$4")
public interface GrindstoneSlotAccessor {

    @Accessor("field_16780")
    GrindstoneScreenHandler getOuterHandler();
}
