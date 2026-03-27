package qdeeme.xp_simplifier.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.CropBlock;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import qdeeme.xp_simplifier.util.Config;


public class BlockBreakHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger("xp_simplifier/BlockBreakHandler");

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {

			if (!Config.isBlockBreakXpEnabled()) {
				return;
			}

			if (!(world instanceof ServerWorld)) {
				return;
			}

			if (!(player instanceof ServerPlayerEntity serverPlayer)) {
				return;
			}

			if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
				return;
			}

			// Check for silk touch enchantment
			var enchantments = world.getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            if (enchantments.getEntry(Enchantments.SILK_TOUCH).isPresent() && !(state.getBlock() instanceof CropBlock)) {
                if (serverPlayer.getMainHandStack().getEnchantments().getLevel(enchantments.getEntry(Enchantments.SILK_TOUCH).get()) > 0) {
                    return;
                }
            }

			if (state instanceof BlockState) {
				handleCropBreak(serverPlayer, state);
			} else {
				handleRegularBlockBreak(serverPlayer, state);
			}
		});

		LOGGER.info("Registered block break XP handler");
	}

	private static void handleCropBreak(ServerPlayerEntity player, BlockState state) {
		if (!Config.isCropXpEnabled()) {
			return;
		}

		// Check if crop is fully grown
		boolean isMaxAge = false;
		int age = -1;

		if (state.contains(Properties.AGE_15)) {
			age = state.get(Properties.AGE_15);
			isMaxAge = (age == 15);
		} else if (state.contains(Properties.AGE_7)) {
			age = state.get(Properties.AGE_7);
			isMaxAge = (age == 7);
		} else if (state.contains(Properties.AGE_5)) {
			age = state.get(Properties.AGE_5);
			isMaxAge = (age == 5);
		} else if (state.contains(Properties.AGE_4)) {
			age = state.get(Properties.AGE_4);
			isMaxAge = (age == 4);
		} else if (state.contains(Properties.AGE_3)) {
			age = state.get(Properties.AGE_3);
			isMaxAge = (age == 3);
		} else if (state.contains(Properties.AGE_2)) {
			age = state.get(Properties.AGE_2);
			isMaxAge = (age == 2);
		}

		String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
		int xp = Config.getBlockXp(blockId, isMaxAge);
		if (xp > 0) {
			player.addExperience(xp);
		}
	}

	private static void handleRegularBlockBreak(ServerPlayerEntity player, BlockState state) {
		String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
		int xp = Config.getBlockXp(blockId);
		if (xp > 0) {
			player.addExperience(xp);
		}
	}
}