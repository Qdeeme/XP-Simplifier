package qdeeme.xp_simplifier.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import qdeeme.xp_simplifier.util.config.XpsConfig;
import qdeeme.xp_simplifier.util.CropXpMode;

public class BlockBreakHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/BlockBreakHandler");
    private static final ThreadLocal<Player> breakingPlayer = new ThreadLocal<>();
    private static final ThreadLocal<Integer> pendingVanillaXp = new ThreadLocal<>();
    private static IntOpenHashSet ageBlockRawIds;

    public static void register() {

        // pre-map of age-based block IDs (crops)
        ageBlockRawIds = new IntOpenHashSet();
        for (Block block : BuiltInRegistries.BLOCK) {
            for (var prop : block.defaultBlockState().getProperties()) {
                if (prop.getName().startsWith("age") && prop instanceof IntegerProperty) {
                    ageBlockRawIds.add(BuiltInRegistries.BLOCK.getId(block));
                    break;
                }
            }
        }

        LOGGER.info("Registered block break XP handler");
    }

    public static void setBreakingPlayer(Player player) {
        breakingPlayer.set(player);
    }

    public static void clearBreakingPlayer() {
        breakingPlayer.remove();
    }

    public static void storeVanillaXp(int size) {
        pendingVanillaXp.set(size);
    }

    public static void clearPendingVanillaXp() {
        pendingVanillaXp.remove();
    }

    public static void blockSimpleHandling(Player player, int rawId) {
        Integer vanilla = pendingVanillaXp.get();
        pendingVanillaXp.remove();
        switch (XpsConfig.getBlockXpModeEnum()) {
            case ON:
                Integer configXp = XpsConfig.getBlockXp(rawId);
                int xp = configXp != null ? configXp : (vanilla != null ? vanilla : 0);
                player.giveExperiencePoints(Math.round(xp * XpsConfig.getBlockXpMultiplier()));
                break;
            case VANILLA:
                if (vanilla != null && vanilla > 0) {
                    player.giveExperiencePoints(Math.round(vanilla * XpsConfig.getBlockXpMultiplier()));
                }
                break;
            case OFF:
                break;
        }
    }

    public static void blockVanillaHandling(ServerLevel world, BlockPos pos, int rawId) {
        Integer vanilla = pendingVanillaXp.get();
        pendingVanillaXp.remove();
        switch (XpsConfig.getBlockXpModeEnum()) {
            case ON:
                Integer configXp = XpsConfig.getBlockXp(rawId);
                int xp = configXp != null ? configXp : (vanilla != null ? vanilla : 0);
                if (xp > 0) {
                    ExperienceOrb.award(world, Vec3.atCenterOf(pos), Math.round(xp * XpsConfig.getBlockXpMultiplier()));
                }
                break;
            case VANILLA:
                if (vanilla != null && vanilla > 0) {
                    ExperienceOrb.award(world, Vec3.atCenterOf(pos), Math.round(vanilla * XpsConfig.getBlockXpMultiplier()));
                }
                break;
            case OFF:
                break;
        }
    }

    public static void cropVanillaHandling(ServerLevel world, BlockPos pos, BlockState state) {
        if (XpsConfig.getCropXpModeEnum() != CropXpMode.ON) {
            return;
        }
        int rawId = BuiltInRegistries.BLOCK.getId(state.getBlock());
        boolean mature = isCropMature(state);
        Integer configXp = XpsConfig.getBlockXp(rawId, mature);
        if (configXp != null && configXp > 0) {
            ExperienceOrb.award(world, Vec3.atCenterOf(pos), Math.round(configXp * XpsConfig.getCropXpMultiplier()));
        }
    }

    public static void cropSimpleHandling(Player player, BlockState state) {
        if (XpsConfig.getCropXpModeEnum() != CropXpMode.ON) {
            return;
        }
        int rawId = BuiltInRegistries.BLOCK.getId(state.getBlock());
        boolean mature = isCropMature(state);
        Integer configXp = XpsConfig.getBlockXp(rawId, mature);
        if (configXp != null) {
            player.giveExperiencePoints(Math.round(configXp * XpsConfig.getCropXpMultiplier()));
        }
    }

    public static boolean isCrop(int rawId) {
        return ageBlockRawIds.contains(rawId);
    }

    public static boolean isCropMature(BlockState state) {
        boolean isMaxAge = false;
        if (state.hasProperty(BlockStateProperties.AGE_2)) {
            isMaxAge = state.getValue(BlockStateProperties.AGE_2) == 2;
        } else if (state.hasProperty(BlockStateProperties.AGE_7)) {
            isMaxAge = state.getValue(BlockStateProperties.AGE_7) == 7;
        } else if (state.hasProperty(BlockStateProperties.AGE_3)) {
            isMaxAge = state.getValue(BlockStateProperties.AGE_3) == 3;
        } else if (state.hasProperty(BlockStateProperties.AGE_4)) {
            isMaxAge = state.getValue(BlockStateProperties.AGE_4) == 4;
        } else if (state.hasProperty(BlockStateProperties.AGE_5)) {
            isMaxAge = state.getValue(BlockStateProperties.AGE_5) == 5;
        } else if (state.hasProperty(BlockStateProperties.AGE_15)) {
            isMaxAge = state.getValue(BlockStateProperties.AGE_15) == 15;
        } else if (state.hasProperty(BlockStateProperties.AGE_25)) {
            isMaxAge = state.getValue(BlockStateProperties.AGE_25) == 25;
        }
        return isMaxAge;
    }
}
