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

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.server.level.ServerPlayer;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.RegistryCache;

@Mixin(AnvilMenu.class)
public abstract class MixinMendingRework extends ItemCombinerMenu {

    @Shadow
    private final DataSlot cost = DataSlot.standalone();

    public MixinMendingRework() {
        super(null, 0, null, null);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void mendingXpOnlyRepair(CallbackInfo ci) {
        if (!XpsConfig.isXpRepairEnabled()) return;
        if (!(this.player instanceof ServerPlayer serverPlayer)) return;

        ItemStack leftStack = this.inputSlots.getItem(0);
        ItemStack rightStack = this.inputSlots.getItem(1);

        // Only solo-mending: item in left slot, right slot empty, item is damaged, has Mending
        if (leftStack.isEmpty() || !rightStack.isEmpty() || !leftStack.isDamaged()) return;
        if (EnchantmentHelper.getTagEnchantmentLevel(RegistryCache.MENDING, leftStack) <= 0) return;

        ItemStack resultStack = leftStack.copy();
        int currentDamage = resultStack.getDamageValue();
        if (currentDamage <= 0) return;

        float durPerPoint = XpsConfig.getDurabilityPerPoint();
        int maxLevelCost = XpsConfig.getMaxAnvilRepairCost();

        // XP points needed for a full repair
        int xpNeeded = (int) Math.ceil((float) currentDamage / durPerPoint);
        int level = serverPlayer.experienceLevel;

        int levelCost = Math.max(1, xps_levelCost(xpNeeded, level));
        // min() handles both cases: capped AND player not having enough levels (ran out of levels → xpForLevels < xpNeeded)
        int xpSpent = Math.min(xpNeeded, xps_xpForLevels(level, levelCost));

        if (levelCost >= maxLevelCost) {
            resultStack.setDamageValue(0);
            this.resultSlots.setItem(0, resultStack);
            this.cost.set(maxLevelCost);
        } else {
            resultStack.setDamageValue(Math.max(0, currentDamage - (int) (xpSpent * durPerPoint)));
            this.resultSlots.setItem(0, resultStack);
            this.cost.set(levelCost);
        }
    }

    /**
     * How many full levels must be spent to accumulate {@code xpNeeded} XP points (iterative ladder walk).
     */
    @Unique
    private static int xps_levelCost(int xpNeeded, int level) {
        int accumulated = 0;
        int levels = 0;
        int lvl = level;
        while (accumulated < xpNeeded && lvl > 0) {
            accumulated += xps_xpPerLevel(lvl - 1);
            levels++;
            lvl--;
        }
        return levels;
    }

    /**
     * Total XP points accumulated by spending exactly {@code n} levels downward from {@code level}.
     */
    @Unique
    private static int xps_xpForLevels(int level, int n) {
        int xp = 0;
        int lvl = level;
        for (int i = 0; i < n && lvl > 0; i++) {
            xp += xps_xpPerLevel(lvl - 1);
            lvl--;
        }
        return xp;
    }

    /**
     * XP points required to go from {@code level} to {@code level + 1} — vanilla formula.
     */
    @Unique
    private static int xps_xpPerLevel(int level) {
        if (level < 16) return 2 * level + 7;
        if (level < 31) return 5 * level - 38;
        return 9 * level - 158;
    }
}
