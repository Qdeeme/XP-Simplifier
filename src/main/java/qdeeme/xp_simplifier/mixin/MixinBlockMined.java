package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.block.entity.BlockEntity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qdeeme.xp_simplifier.handler.BlockBreakHandler;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.RegistryCache;

@Mixin(Block.class)
public abstract class MixinBlockMined {
    private static OrbMode ORBMODE;


    @Inject(method = "afterBreak", at = @At("HEAD"))
    private void captureBreakingPlayer(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (!(world instanceof ServerWorld)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (RegistryCache.SILK_TOUCH != null && player.getMainHandStack().getEnchantments().getLevel(RegistryCache.SILK_TOUCH) > 0) {
            return;
        }
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (ORBMODE == OrbMode.SIMPLE) {
            BlockBreakHandler.setBreakingPlayer(player);
        }
    }

    @Inject(method = "afterBreak", at = @At("TAIL"))
    private void onAfterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            BlockBreakHandler.clearBreakingPlayer();
            return;
        }
        if (RegistryCache.SILK_TOUCH != null && player.getMainHandStack().getEnchantments().getLevel(RegistryCache.SILK_TOUCH) > 0) {
            BlockBreakHandler.clearBreakingPlayer();
            return;
        }
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }

        int rawId = Registries.BLOCK.getRawId(state.getBlock());
        if (ORBMODE == OrbMode.VANILLA) {
            if (BlockBreakHandler.isCrop(rawId)) {
                BlockBreakHandler.cropVanillaHandling(serverWorld, pos, state);
            } else {
                BlockBreakHandler.blockVanillaHandling(serverWorld, pos, rawId);
            }
        } else {
            if (BlockBreakHandler.isCrop(rawId)) {
                BlockBreakHandler.cropSimpleHandling(player, state);
            } else {
                BlockBreakHandler.blockSimpleHandling(player, rawId);
            }
        }
        // Safety setters
        BlockBreakHandler.clearBreakingPlayer();
        BlockBreakHandler.clearPendingVanillaXp();
    }


    @Redirect(method = "dropExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"))
    private void redirectXpOrbSpawn(ServerWorld world, Vec3d pos, int size) {
        BlockBreakHandler.storeVanillaXp(size);
    }
}