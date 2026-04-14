package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

import net.minecraft.entity.passive.AnimalEntity;



@Mixin(AnimalEntity.class)
public interface LovingPlayerAccessor {

    @Accessor("lovingPlayer")
    UUID getLovingPlayer();

    @Accessor("lovingPlayer")
    void setLovingPlayer(UUID lovingPlayer);

}
