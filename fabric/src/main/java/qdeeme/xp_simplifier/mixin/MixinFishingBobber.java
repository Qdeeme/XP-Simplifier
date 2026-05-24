/*
 * Copyright (C) 2026 Qdeeme.
 *
 * This file is part of "Xp Simplifier".
 *
 * "Xp Simplifier" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */


package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qdeeme.xp_simplifier.handler.FishingHandler;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobber {

    @Shadow
    public abstract PlayerEntity getPlayerOwner();

    @Unique
    private List<ItemStack> xp_simplifier$itemCaught = null;

    @Unique
    private void xp_simplifier$clearCaughtItems() {
        if (xp_simplifier$itemCaught != null) {
            xp_simplifier$itemCaught.clear();
        }
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean captureCaughtItems(World world, Entity entity) {
        if (Config.getFishingXpModeEnum() == XpMode.ON) {
            if (entity instanceof ItemEntity itemEntity) {
                if (xp_simplifier$itemCaught == null) {
                    xp_simplifier$itemCaught = new ArrayList<>(2);
                }
                xp_simplifier$itemCaught.add(itemEntity.getStack());
            }
        }

        if (entity instanceof ExperienceOrbEntity) {
            if (world instanceof ServerWorld serverWorld) {
                if (Config.getOrbModeEnum() == OrbMode.VANILLA) {
                    int correctXp = 0;
                    int vanillaXP = 1 + world.random.nextInt(6);
                    switch (Config.getFishingXpModeEnum()) {
                        case VANILLA:
                            Integer globalXp = Config.getGlobalFishingXp();
                            correctXp = Math.round((globalXp != null ? globalXp : vanillaXP) * Config.getFishingXpMultiplier());
                            break;
                        case ON:
                            if (xp_simplifier$itemCaught != null && !xp_simplifier$itemCaught.isEmpty()) {
                                ItemStack first = xp_simplifier$itemCaught.getFirst();
                                Integer itemXp = Config.getFishingXp(Registries.ITEM.getRawId(first.getItem()));
                                correctXp = Math.round((itemXp != null ? itemXp : vanillaXP) * Config.getFishingXpMultiplier());
                            }
                            break;
                        case OFF:
                            break;
                    }
                    ExperienceOrbEntity.spawn(serverWorld, entity.getPos(), correctXp);
                    xp_simplifier$clearCaughtItems();
                } else {
                    PlayerEntity player = this.getPlayerOwner();
                    if (player instanceof PlayerEntity serverPlayer) {
                        FishingHandler.onFishCaught(serverPlayer, xp_simplifier$itemCaught != null ? xp_simplifier$itemCaught : Collections.emptyList());
                    }
                    xp_simplifier$clearCaughtItems();
                }
            }
        } else {
            if (entity instanceof ItemEntity) {
                return world.spawnEntity(entity);
            }
        }
        return false;
    }
}

