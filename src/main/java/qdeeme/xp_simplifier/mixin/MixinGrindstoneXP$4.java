package qdeeme.xp_simplifier.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

@Mixin(targets = "net.minecraft.screen.GrindstoneScreenHandler$4")
public abstract class MixinGrindstoneXP$4 {

    private static OrbMode ORBMODE;
    private static XpMode  GRINDSTONEMODE;

    @Unique
    private PlayerEntity capturedPlayer = null;

    @Unique
    private int capturedEnchXp = 0;

    /**
     * Capture player + pre-calculate enchantment XP at the top of onTakeItem.
     */
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void captureGrindState(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (GRINDSTONEMODE == null) {
            GRINDSTONEMODE = Config.getGrindstoneXpModeEnum();
        }
        capturedPlayer = player;
        if (GRINDSTONEMODE == XpMode.ON) {
            GrindstoneScreenHandler grindstoneSlots = ((GrindstoneSlotAccessor)(Object)this).getOuterHandler();
            capturedEnchXp = doTheMathEnchantmentXp(grindstoneSlots.slots.get(0).getStack()) + doTheMathEnchantmentXp(grindstoneSlots.slots.get(1).getStack());
        }
    }


    @Inject(method = "getExperience(Lnet/minecraft/world/World;)I", at = @At("RETURN"), cancellable = true)
    private void modifyGrindXp(World world, CallbackInfoReturnable<Integer> cir) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (GRINDSTONEMODE == null) {
            GRINDSTONEMODE = Config.getGrindstoneXpModeEnum();
        }
        int vanillaXp = cir.getReturnValue();
        int modifiedXp = switch (GRINDSTONEMODE) {
            case VANILLA -> vanillaXp;
            case ON -> capturedEnchXp > 0 ? capturedEnchXp : vanillaXp;
            case OFF -> 0;
        };

        if (ORBMODE == OrbMode.SIMPLE) {
            if (modifiedXp > 0 && capturedPlayer instanceof ServerPlayerEntity player) {
                player.addExperience(modifiedXp > 0 ? modifiedXp : vanillaXp);
            }
            cir.setReturnValue(0);
        } else {
            cir.setReturnValue(modifiedXp);
        }
        capturedPlayer = null;
        capturedEnchXp = 0;
    }

    @Unique
    private int doTheMathEnchantmentXp(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        int total = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment.isCursed()) {
                continue;
            }
            int level = entry.getValue();
            Integer configXp = Config.getGrindstoneXp(Registries.ENCHANTMENT.getRawId(enchantment));
            int xpPerLevel;
            if (configXp != null && configXp > 0) {
                xpPerLevel = configXp;
            } else {
                xpPerLevel = enchantment.getMinLevel() == enchantment.getMaxLevel() ? enchantment.getMinLevel() / 2 : (enchantment.getMinLevel() + enchantment.getMaxLevel());
            }
            total += xpPerLevel * level;
        }
        return total;
    }
}

