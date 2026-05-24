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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import qdeeme.xp_simplifier.mixin.accessors.GrindstoneSlotAccessor;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class MixinGrindstoneXP$4 {

    @Unique
    private Player xp_simplifier$capturedPlayer = null;

    @Unique
    private Integer xp_simplifier$capturedEnchXp = null;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void captureGrindState(Player player, ItemStack stack, CallbackInfo ci) {
        xp_simplifier$capturedPlayer = player;
        if (XpsConfig.getGrindstoneXpModeEnum() == XpMode.ON) {
            GrindstoneMenu grindstoneSlots = ((GrindstoneSlotAccessor) this).getOuterHandler();
            xp_simplifier$capturedEnchXp = xp_simplifier$doTheMathEnchantmentXp(grindstoneSlots.slots.get(0).getItem()) + xp_simplifier$doTheMathEnchantmentXp(grindstoneSlots.slots.get(1).getItem());
        }
    }

    @Inject(method = "getExperienceAmount(Lnet/minecraft/world/level/Level;)I", at = @At("RETURN"), cancellable = true)
    private void modifyGrindXp(Level world, CallbackInfoReturnable<Integer> cir) {
        int vanillaXp = cir.getReturnValue();
        int modifiedXp = switch (XpsConfig.getGrindstoneXpModeEnum()) {
            case VANILLA -> vanillaXp;
            case ON -> xp_simplifier$capturedEnchXp;
            case OFF -> 0;
        };

        // Apply grindstone multiplier to the final value (skip for OFF = 0)
        if (modifiedXp > 0) {
            modifiedXp = Math.round(modifiedXp * XpsConfig.getGrindstoneXpMultiplier());
        }

        if (XpsConfig.getOrbModeEnum() == OrbMode.SIMPLE) {
            if (xp_simplifier$capturedPlayer instanceof ServerPlayer player) {
                player.giveExperiencePoints(modifiedXp);
            }
            cir.setReturnValue(0);
        } else {
            cir.setReturnValue(modifiedXp);
        }
        xp_simplifier$capturedPlayer = null;
        xp_simplifier$capturedEnchXp = null;
    }

    @Unique
    private int xp_simplifier$doTheMathEnchantmentXp(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        int total = 0;
        for (Holder<Enchantment> entry : enchantments.keySet()) {
            if (entry.is(EnchantmentTags.CURSE)) {
                continue;
            }
            if (!(entry instanceof Holder.Reference<Enchantment> ench)) {
                continue;
            }
            int level = enchantments.getLevel(entry);
            Integer configXp = XpsConfig.getGrindstoneXp(ench.key().location().toString());
            int uniXP;
            if (configXp != null) {
                uniXP = configXp;
            } else {
                uniXP = ench.value().getMinLevel() == ench.value().getMaxLevel() ? ench.value().getMinLevel() * 2 : (ench.value().getMinLevel() + ench.value().getMaxLevel());
            }
            total += uniXP * level;
        }
        return total;
    }
}


