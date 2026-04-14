package qdeeme.xp_simplifier.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.RegistryCache;
import qdeeme.xp_simplifier.util.XpMode;

public class BlockBreakHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/BlockBreakHandler");

    private static final OrbMode ORBMODE   = Config.getOrbModeEnum();
    private static final XpMode  BLOCKMODE = Config.getBlockXpModeEnum();
    private static final XpMode  CROPMODE  = Config.getCropXpModeEnum();
    private static final ThreadLocal<ServerPlayerEntity> breakingPlayer = new ThreadLocal<>();
    private static IntOpenHashSet ageBlockRawIds;

    public static void register() {

        // Build cache of raw IDs for blocks with an "age" property 
        ageBlockRawIds = new IntOpenHashSet();
        for (Block block : Registries.BLOCK) {
            for (var prop : block.getDefaultState().getProperties()) {
                if (prop.getName().startsWith("age") && prop instanceof IntProperty) {
                    ageBlockRawIds.add(Registries.BLOCK.getRawId(block));
                    break;
                }
            }
        }
        LOGGER.debug("Indexed {} crop block types", ageBlockRawIds.size());

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {

            breakingPlayer.remove();
            if (BLOCKMODE == XpMode.OFF && CROPMODE == XpMode.OFF) {
                return;
            }
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }
            if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
                return;
            }

            if (ORBMODE == OrbMode.VANILLA) {
                if (isCrop(state) && CROPMODE == XpMode.ON) {
                    handleCropBreakVanilla(serverWorld, pos, state);

                }
                return;
            }

            if (!isCrop(state)) {
                // Use cached RegistryEntry; fall back to live lookup on first server tick if cache not yet set
                var silkTouchEntry = RegistryCache.SILK_TOUCH != null ? Optional.of(RegistryCache.SILK_TOUCH) : world.getServer().getRegistryManager()
                .get(net.minecraft.registry.RegistryKeys.ENCHANTMENT).getEntry(net.minecraft.enchantment.Enchantments.SILK_TOUCH);

                if (silkTouchEntry.isPresent() && serverPlayer.getMainHandStack().getEnchantments().getLevel(silkTouchEntry.get()) > 0) {
                    return;
                }
            }

            if (isCrop(state) && CROPMODE == XpMode.ON) {
                handleCropBreak(serverPlayer, state);
            } else if (BLOCKMODE != XpMode.OFF) {
                breakingPlayer.set(serverPlayer);
            }
        });

        LOGGER.info("Registered block break XP handler");
    }

    public static void awardRegularBlockXp(int vanillaXP, String blockId) {

        ServerPlayerEntity player = breakingPlayer.get();
        breakingPlayer.remove();
        if (player == null) {
            return;
        }
        switch (BLOCKMODE) {
            case VANILLA:
                player.addExperience(vanillaXP);
                break;
            case ON:
                int configXp = Config.getBlockXp(blockId);
                if (configXp < 0) {
                    player.addExperience(configXp);
                    return;
                }
                player.addExperience(configXp >= 0 ? configXp : vanillaXP);
                break;
            case OFF:
                break;
        }
    }

    private static void handleCropBreakVanilla(ServerWorld world, BlockPos pos, BlockState state) {
        String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
        int xp = Config.getBlockXp(blockId, isCropMature(state));
        if (xp > 0) {
            ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), xp);
        }
    }

    public static void handleCropBreak(ServerPlayerEntity player, BlockState state) {
        String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
        int xp = Config.getBlockXp(blockId, isCropMature(state));
        if (xp > 0 || xp < 0) {
            player.addExperience(xp);
        } else {
            return;
        }
    }

    // O(1) checks the pre-built raw-ID set instead of streaming over block properties
    private static boolean isCrop(BlockState state) {
        return ageBlockRawIds.contains(Registries.BLOCK.getRawId(state.getBlock()));
    }

    private static boolean isCropMature(BlockState state) {
        boolean isMaxAge = false;
        if (state.contains(Properties.AGE_2)) {
            isMaxAge = state.get(Properties.AGE_2) == 2;
        }
        else if (state.contains(Properties.AGE_7)) {
            isMaxAge = state.get(Properties.AGE_7)  == 7;
        }
        else if (state.contains(Properties.AGE_3)) {
            isMaxAge = state.get(Properties.AGE_3)  == 3;
        }
        else if (state.contains(Properties.AGE_4)) {
            isMaxAge = state.get(Properties.AGE_4)  == 4;
        }
        else if (state.contains(Properties.AGE_5)) {
            isMaxAge = state.get(Properties.AGE_5)  == 5;
        }
        else if (state.contains(Properties.AGE_15)) {
            isMaxAge = state.get(Properties.AGE_15) == 15;
        }
        else if (state.contains(Properties.AGE_25)) {
            isMaxAge = state.get(Properties.AGE_25) == 25;
        }
        return isMaxAge;
    }
}
