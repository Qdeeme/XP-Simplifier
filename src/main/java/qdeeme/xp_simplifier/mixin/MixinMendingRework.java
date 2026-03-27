package qdeeme.xp_simplifier.mixin;


import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import qdeeme.xp_simplifier.util.Config;


@Mixin(AnvilScreenHandler.class)
public abstract class MixinMendingRework extends ForgingScreenHandler {
    
    @Shadow
    @Final
    private Property levelCost;
    
    // Constructor required for extending ForgingScreenHandler
    public MixinMendingRework() {
        super(null, 0, null, null);
    }
    
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void xp_simplifier$xpOnlyRepair(CallbackInfo ci) {
        if (!Config.isXpRepairEnabled()) {
            return;
        }
        
        // Must be a server player
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Get input stacks from the input inventory
        ItemStack leftStack = this.input.getStack(0);
        ItemStack rightStack = this.input.getStack(1);
        
        // Only process if right slot is empty and left has damaged item
        if (leftStack.isEmpty() || !rightStack.isEmpty() || !leftStack.isDamaged()) {
            return;
        }
        
        // Check for mending enchantment
        int mendingLevel = EnchantmentHelper.getLevel(Enchantments.MENDING, leftStack);
        if (mendingLevel <= 0) {
            return;
        }
        
        // Create result copy
        ItemStack resultStack = leftStack.copy();
        int currentDamage = resultStack.getDamage();
        
        if (currentDamage <= 0) {
            return;
        }

        // Calculate levels needed for repair 
        int levelsNeeded = MathHelper.ceil((float) currentDamage / 100.0F);
        
        // Get max cost from config
        int maxCost = Config.getMaxAnvilRepairCost();
        int finalCost = Math.min(levelsNeeded, maxCost);
        int repairAmount = finalCost * Config.getDurabilityPerLevel();
        int newDamage = Math.max(0, currentDamage - repairAmount);
        resultStack.setDamage(newDamage);
        
        // Set output stack
        this.output.setStack(0, resultStack);
        // Set the XP cost
        this.levelCost.set(finalCost);
    }
}