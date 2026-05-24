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

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import qdeeme.xp_simplifier.mixin.accessor.GrindstoneSlotAccessor;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;

@Mixin(targets = "net.minecraft.screen.GrindstoneScreenHandler$4")
public abstract class MixinGrindstoneXP$4 {

    @Unique
    private PlayerEntity capturedPlayer = null;

    @Unique
    private Integer capturedEnchXp = null;

    /**
     * Capture player + pre-calculate enchantment XP at the top of onTakeItem.
     */
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void captureGrindState(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        capturedPlayer = player;
        if (Config.getGrindstoneXpModeEnum() == qdeeme.xp_simplifier.util.XpMode.ON) {
            GrindstoneScreenHandler grindstoneSlots = ((GrindstoneSlotAccessor) this).getOuterHandler();
            capturedEnchXp = doTheMathEnchantmentXp(grindstoneSlots.slots.get(0).getStack())
                    + doTheMathEnchantmentXp(grindstoneSlots.slots.get(1).getStack());
        }
    }


    @Inject(method = "getExperience(Lnet/minecraft/world/World;)I", at = @At("RETURN"), cancellable = true)
    private void modifyGrindXp(World world, CallbackInfoReturnable<Integer> cir) {
        int vanillaXp = cir.getReturnValue();
        int modifiedXp = switch (Config.getGrindstoneXpModeEnum()) {
            case VANILLA -> vanillaXp;
            case ON -> capturedEnchXp != null ? capturedEnchXp : vanillaXp;
            case OFF -> 0;
        };

        // Apply grindstone multiplier (skip for OFF = 0)
        if (modifiedXp > 0) {
            modifiedXp = Math.round(modifiedXp * Config.getGrindstoneXpMultiplier());
        }

        if (Config.getOrbModeEnum() == OrbMode.SIMPLE) {
            if (capturedPlayer instanceof ServerPlayerEntity player) {
                player.addExperience(modifiedXp);
            }
            cir.setReturnValue(0);
        } else {
            cir.setReturnValue(modifiedXp);
        }
        capturedPlayer = null;
        capturedEnchXp = null;
    }

    @Unique
    private int doTheMathEnchantmentXp(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        int total = 0;
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.isIn(EnchantmentTags.CURSE)) continue;
            if (!(entry instanceof RegistryEntry.Reference<Enchantment> ench)) continue;
            int level = enchantments.getLevel(entry);
            Integer configXp = Config.getGrindstoneXp(ench.registryKey().getValue().toString());
            int uniXP = configXp != null ? configXp
                    : (ench.value().getMinLevel() == ench.value().getMaxLevel()
                    ? ench.value().getMinLevel() * 2
                    : (ench.value().getMinLevel() + ench.value().getMaxLevel()));
            total += uniXP * level;
        }
        return total;
    }
}

