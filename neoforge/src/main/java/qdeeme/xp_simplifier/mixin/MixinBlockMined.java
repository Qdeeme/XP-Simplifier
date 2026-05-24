package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import qdeeme.xp_simplifier.handler.BlockBreakHandler;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.RegistryCache;

@Mixin(Block.class)
public abstract class MixinBlockMined {


    @Inject(method = "playerDestroy", at = @At("HEAD"))
    private void captureBreakingPlayer(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (!(world instanceof ServerLevel)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (RegistryCache.SILK_TOUCH != null && player.getMainHandItem().getTagEnchantments().getLevel(RegistryCache.SILK_TOUCH) > 0) {
            return;
        }
        if (XpsConfig.getOrbModeEnum() == OrbMode.SIMPLE) {
            BlockBreakHandler.setBreakingPlayer(player);
        }
    }

    @Inject(method = "playerDestroy", at = @At("TAIL"))
    private void onAfterBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            BlockBreakHandler.clearBreakingPlayer();
            return;
        }
        if (RegistryCache.SILK_TOUCH != null && player.getMainHandItem().getTagEnchantments().getLevel(RegistryCache.SILK_TOUCH) > 0) {
            BlockBreakHandler.clearBreakingPlayer();
            return;
        }

        int rawId = BuiltInRegistries.BLOCK.getId(state.getBlock());
        if (XpsConfig.getOrbModeEnum() == OrbMode.VANILLA) {
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
        // Safety clears
        BlockBreakHandler.clearBreakingPlayer();
        BlockBreakHandler.clearPendingVanillaXp();
    }


    @Redirect(method = "popExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void redirectXpOrbSpawn(ServerLevel world, Vec3 pos, int size) {
        BlockBreakHandler.storeVanillaXp(size);
    }
}