package qdeeme.xp_simplifier.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.CropXpMode;

public class BlockBreakHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/BlockBreakHandler");

    private static final ThreadLocal<PlayerEntity> breakingPlayer = new ThreadLocal<>();
    private static final ThreadLocal<Integer> pendingVanillaXp = new ThreadLocal<>();
    private static IntOpenHashSet ageBlockRawIds;

    public static void register() {
        ageBlockRawIds = new IntOpenHashSet();
        for (Block block : Registries.BLOCK) {
            for (var prop : block.getDefaultState().getProperties()) {
                if (prop.getName().startsWith("age") && prop instanceof IntProperty) {
                    ageBlockRawIds.add(Registries.BLOCK.getRawId(block));
                    break;
                }
            }
        }
        LOGGER.info("Registered block break XP handler");
    }

    public static void setBreakingPlayer(PlayerEntity player) {
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

    public static void blockSimpleHandling(PlayerEntity player, int rawId) {
        Integer vanilla = pendingVanillaXp.get();
        pendingVanillaXp.remove();
        switch (Config.getBlockXpModeEnum()) {
            case ON:
                Integer configXp = Config.getBlockXp(rawId);
                int xp = configXp != null ? configXp : (vanilla != null ? vanilla : 0);
                player.addExperience(Math.round(xp * Config.getBlockXpMultiplier()));
                break;
            case VANILLA:
                if (vanilla != null && vanilla > 0) {
                    player.addExperience(Math.round(vanilla * Config.getBlockXpMultiplier()));
                }
                break;
            case OFF:
                break;
        }
    }

    public static void blockVanillaHandling(ServerWorld world, BlockPos pos, int rawId) {
        Integer vanilla = pendingVanillaXp.get();
        pendingVanillaXp.remove();
        switch (Config.getBlockXpModeEnum()) {
            case ON:
                Integer configXp = Config.getBlockXp(rawId);
                int xp = configXp != null ? configXp : (vanilla != null ? vanilla : 0);
                if (xp > 0) {
                    ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), Math.round(xp * Config.getBlockXpMultiplier()));
                }
                break;
            case VANILLA:
                if (vanilla != null && vanilla > 0) {
                    ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), Math.round(vanilla * Config.getBlockXpMultiplier()));
                }
                break;
            case OFF:
                break;
        }
    }

    public static void cropVanillaHandling(ServerWorld world, BlockPos pos, BlockState state) {
        if (Config.getCropXpModeEnum() != CropXpMode.ON) return;
        int rawId = Registries.BLOCK.getRawId(state.getBlock());
        boolean mature = isCropMature(state);
        Integer configXp = Config.getBlockXp(rawId, mature);
        if (configXp != null && configXp > 0) {
            ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), Math.round(configXp * Config.getCropXpMultiplier()));
        }
    }

    public static void cropSimpleHandling(PlayerEntity player, BlockState state) {
        if (Config.getCropXpModeEnum() != CropXpMode.ON) return;
        int rawId = Registries.BLOCK.getRawId(state.getBlock());
        boolean mature = isCropMature(state);
        Integer configXp = Config.getBlockXp(rawId, mature);
        if (configXp != null) {
            player.addExperience(Math.round(configXp * Config.getCropXpMultiplier()));
        }
    }

    public static boolean isCrop(int rawId) {
        return ageBlockRawIds.contains(rawId);
    }

    public static boolean isCropMature(BlockState state) {
        if (state.contains(Properties.AGE_2)) return state.get(Properties.AGE_2) == 2;
        if (state.contains(Properties.AGE_7)) return state.get(Properties.AGE_7) == 7;
        if (state.contains(Properties.AGE_3)) return state.get(Properties.AGE_3) == 3;
        if (state.contains(Properties.AGE_4)) return state.get(Properties.AGE_4) == 4;
        if (state.contains(Properties.AGE_5)) return state.get(Properties.AGE_5) == 5;
        if (state.contains(Properties.AGE_15)) return state.get(Properties.AGE_15) == 15;
        if (state.contains(Properties.AGE_25)) return state.get(Properties.AGE_25) == 25;
        return false;
    }
}
