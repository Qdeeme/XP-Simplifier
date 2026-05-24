package qdeeme.xp_simplifier.mixin.accessors;

import net.minecraft.world.entity.projectile.Projectile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;


@Mixin(Projectile.class)
public interface ProjectileOwnerAccessor {

    @Accessor("ownerUUID")
    UUID getOwnerUuid();
}
