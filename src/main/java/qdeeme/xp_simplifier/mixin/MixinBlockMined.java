package qdeeme.xp_simplifier.mixin;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.registry.Registries;
import qdeeme.xp_simplifier.handler.BlockBreakHandler;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

@Mixin(Block.class)
public abstract class MixinBlockMined {
    private static OrbMode ORBMODE;
    private static XpMode  BLOCKXPMODE;

    // "simple" mode: player is already captured in BEFORE, vanilla size is right here as param — award instantly
    @Inject(method = "dropExperience", at = @At("HEAD"))
    private void onBlockDropExperience(ServerWorld world, BlockPos pos, int size, CallbackInfo ci) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (ORBMODE != OrbMode.SIMPLE) {
            return;
        }
        String blockId = Registries.BLOCK.getId(((Block)(Object)this)).toString();
        BlockBreakHandler.awardRegularBlockXp(size, blockId);
    }

    // "vanilla" mode: overwrite the orb spawn amount with config value before orb is created
    @ModifyArg(method = "dropExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"), index = 2)
    private int modifyBlockXpVanilla(int defaultXPValue) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (BLOCKXPMODE == null) {
            BLOCKXPMODE = Config.getBlockXpModeEnum();
        }
        if (ORBMODE == OrbMode.VANILLA) {
            switch (BLOCKXPMODE) {
                case ON:
                    String blockId = Registries.BLOCK.getId(((Block)(Object)this)).toString();
                    int configXp = Config.getBlockXp(blockId);
                    return configXp >= 0 ? configXp : defaultXPValue;
                case VANILLA:
                    return defaultXPValue;
                case OFF:
                    return 0;
            }
        }
        return defaultXPValue;
    }

}