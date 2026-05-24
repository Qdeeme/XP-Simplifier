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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qdeeme.xp_simplifier.handler.FishingHandler;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(FishingHook.class)
public abstract class MixinFishingBobber {


    @Shadow
    public abstract Player getPlayerOwner();

    @Unique
    private List<ItemStack> xp_simplifier$itemCaught = null;

    @Unique
    private void xp_simplifier$clearCaughtItems() {
        if (xp_simplifier$itemCaught != null) {
            xp_simplifier$itemCaught.clear();
        }
    }

    @Redirect(method = "retrieve", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean captureCaughtItems(Level world, Entity entity) {
        if (XpsConfig.getFishingXpModeEnum() == XpMode.ON) {
            if (entity instanceof ItemEntity itemEntity) {
                if (xp_simplifier$itemCaught == null) {
                    xp_simplifier$itemCaught = new ArrayList<>(2);
                }
                xp_simplifier$itemCaught.add(itemEntity.getItem().copy());
            }
        }

        // Intercept XP orb spawn
        if (entity instanceof ExperienceOrb) {
            if (world instanceof ServerLevel serverWorld) {
                if (XpsConfig.getOrbModeEnum() == OrbMode.VANILLA) {
                    int correctXp = 0;
                    int vanillaXP = 1 + world.random.nextInt(6);
                    switch (XpsConfig.getFishingXpModeEnum()) {
                        case VANILLA:
                            Integer globalXp = XpsConfig.getGlobalFishingXp();
                            correctXp = Math.round((globalXp != null ? globalXp : vanillaXP) * XpsConfig.getFishingXpMultiplier());
                            break;
                        case ON:
                            if (xp_simplifier$itemCaught != null && !xp_simplifier$itemCaught.isEmpty()) {
                                ItemStack first = xp_simplifier$itemCaught.getFirst();
                                Integer itemXp = XpsConfig.getFishingXp(BuiltInRegistries.ITEM.getId(first.getItem()));
                                correctXp = Math.round((itemXp != null ? itemXp : vanillaXP) * XpsConfig.getFishingXpMultiplier());
                            }
                            break;
                        case OFF:
                            break;
                    }
                    ExperienceOrb.award(serverWorld, entity.position(), correctXp);
                    xp_simplifier$clearCaughtItems();
                } else {
                    Player player = this.getPlayerOwner();
                    if (player instanceof ServerPlayer serverPlayer) {
                        FishingHandler.onFishCaught(serverPlayer, xp_simplifier$itemCaught != null ? xp_simplifier$itemCaught : Collections.emptyList());
                    }
                    xp_simplifier$clearCaughtItems();
                }
            }
        } else {
            if (entity instanceof ItemEntity) {
                return world.addFreshEntity(entity);
            }
        }
        return false;
    }
}