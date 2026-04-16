package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
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
        RegistryEntry<Enchantment> mendingEntry = RegistryCache.MENDING;
        if (mendingEntry == null) {
            return;
        }
        
        // Create result copy
        ItemStack resultStack = leftStack.copy();
        int currentDamage = resultStack.getDamage();
        
        if (currentDamage <= 0) {
            return;
        }

        int levelsNeeded = MathHelper.ceil((float) currentDamage / 100.0F);
        int maxCost = Config.getMaxAnvilRepairCost();
        int finalCost = Math.min(levelsNeeded, maxCost);
        int repairAmount = finalCost * Config.getDurabilityPerLevel(); 
        
        // Apply repair
        int newDamage = Math.max(0, currentDamage - repairAmount);
        resultStack.setDamage(newDamage);
        
        this.output.setStack(0, resultStack);
        this.levelCost.set(finalCost);
    }
}

