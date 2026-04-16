package qdeeme.xp_simplifier.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qdeeme.xp_simplifier.handler.FishingHandler;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobber {
    private static OrbMode ORBMODE;
    private static XpMode  FISHINGXPMODE;

    @Shadow
    public abstract PlayerEntity getPlayerOwner();

    @Unique
    private List<ItemStack> itemCaught = null;

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean captureCaughtItems(World world, Entity entity) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (FISHINGXPMODE == null) {
            FISHINGXPMODE = Config.getFishingXpModeEnum();
        }
        if (FISHINGXPMODE == XpMode.ON) {
            if (entity instanceof ItemEntity itemEntity) {
                if (itemCaught == null) itemCaught = new ArrayList<>(2);
                itemCaught.add(itemEntity.getStack().copy());
            }
        }

        // Intercept XP orb spawn
        if (entity instanceof ExperienceOrbEntity) {
            if (ORBMODE == OrbMode.VANILLA && world instanceof ServerWorld serverWorld) {
                int correctXp = 0;
                int vanillaXP = 1 + world.random.nextInt(6);
                switch (FISHINGXPMODE) {
                    case VANILLA:
                        Integer globalXp = Config.getGlobalFishingXp();
                        correctXp = globalXp != null ? globalXp : vanillaXP;
                        break;
                    case ON:
                        if (itemCaught != null && !itemCaught.isEmpty()) {
                            ItemStack first = itemCaught.get(0);
                            Integer itemXp = Config.getFishingXp(Registries.ITEM.getRawId(first.getItem()));
                            correctXp = itemXp != null ? itemXp : vanillaXP;
                        }
                        break;
                    case OFF:
                        correctXp = 0;
                        break;
                }
                ExperienceOrbEntity.spawn(serverWorld, entity.getPos(), correctXp);
                return true;
            } else {
                return false;
            }
        }

        return world.spawnEntity(entity);
    }

    // "simple" mode: call handler to award player directly.
    @Inject(method = "use", at = @At("RETURN"))
    private void simpleFishing(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        if (ORBMODE == null) {
            ORBMODE = Config.getOrbModeEnum();
        }
        if (ORBMODE == OrbMode.SIMPLE) {
            PlayerEntity player = this.getPlayerOwner();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                FishingHandler.onFishCaught(serverPlayer, itemCaught != null ? itemCaught : Collections.emptyList());
            }
        }
        if (itemCaught != null) {
            itemCaught.clear();
            itemCaught = null;
        }
    }
}