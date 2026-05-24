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

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.server.network.ServerPlayerEntity;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.RegistryCache;

@Mixin(AnvilScreenHandler.class)
public abstract class MixinMendingRework extends ForgingScreenHandler {

    @Shadow
    @Final
    private Property levelCost;

    public MixinMendingRework() {
        super(null, 0, null, null);
    }

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void mendingXpOnlyRepair(CallbackInfo ci) {
        if (!Config.isXpRepairEnabled()) return;
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemStack leftStack = this.input.getStack(0);
        ItemStack rightStack = this.input.getStack(1);

        // Only solo-mending: left slot has item, right slot empty, item is damaged, item has Mending
        if (leftStack.isEmpty() || !rightStack.isEmpty() || !leftStack.isDamaged()) return;
        if (RegistryCache.MENDING == null || leftStack.getEnchantments().getLevel(RegistryCache.MENDING) <= 0) return;

        ItemStack resultStack = leftStack.copy();
        int currentDamage = resultStack.getDamage();
        if (currentDamage <= 0) return;

        float durPerPoint = Config.getDurabilityPerPoint();
        int maxLevelCost = Config.getMaxAnvilRepairCost();

        // XP points needed for a full repair
        int xpNeeded = (int) Math.ceil((float) currentDamage / durPerPoint);
        int level = serverPlayer.experienceLevel;

        int levelCostNeeded = Math.max(1, xps_levelCost(xpNeeded, level));
        // min() handles capped cost AND player not having enough levels
        int xpSpent = Math.min(xpNeeded, xps_xpForLevels(level, levelCostNeeded));

        if (levelCostNeeded >= maxLevelCost) {
            resultStack.setDamage(0);
            this.output.setStack(0, resultStack);
            this.levelCost.set(maxLevelCost);
        } else {
            resultStack.setDamage(Math.max(0, currentDamage - (int) (xpSpent * durPerPoint)));
            this.output.setStack(0, resultStack);
            this.levelCost.set(levelCostNeeded);
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
     * XP points required to go from {@code level} to {@code level+1} — vanilla formula.
     */
    @Unique
    private static int xps_xpPerLevel(int level) {
        if (level < 16) return 2 * level + 7;
        if (level < 31) return 5 * level - 38;
        return 9 * level - 158;
    }
}
