package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
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
    private Integer capturedEnchXp = null;

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
            case ON -> capturedEnchXp;
            case OFF -> 0;
        };

        if (ORBMODE == OrbMode.SIMPLE) {
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
        if (stack.isEmpty()) {
            return 0;
        }
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        int total = 0;
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentsMap()) {
            Enchantment ench = entry.getKey().value();
            if (ench.isCursed()) {
                continue;
            }
            int level = entry.getIntValue();
            Integer configXp = Config.getGrindstoneXp(Registries.ENCHANTMENT.getRawId(ench));
            int uniXP;
            if (configXp != null) {
                uniXP = configXp;
            } else {
                uniXP = ench.getMinLevel() == ench.getMaxLevel() ? ench.getMinLevel() * 2 : (ench.getMinLevel() + ench.getMaxLevel());
            }
            total += uniXP * level;
        }
        return total;
    }
}

