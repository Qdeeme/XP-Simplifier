package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.projectile.ProjectileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;


@Mixin(ProjectileEntity.class)
public interface ProjectileOwnerAccessor {

    @Accessor("ownerUuid")
    UUID getOwnerUuid();
}
