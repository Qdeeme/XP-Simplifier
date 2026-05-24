package qdeeme.xp_simplifier.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

import net.minecraft.entity.passive.AnimalEntity;



@Mixin(AnimalEntity.class)
public interface LovingPlayerAccessor {

    @Accessor("lovingPlayer")
    UUID getLovingPlayer();

}
