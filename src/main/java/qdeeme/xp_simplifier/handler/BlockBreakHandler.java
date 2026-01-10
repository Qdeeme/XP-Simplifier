package qdeeme.xp_simplifier.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.Registries;
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

			if (!(world instanceof ServerWorld serverWorld)) {
				return;
			}

			if (!(player instanceof ServerPlayerEntity serverPlayer)) {
				return;
			}

			// Don't give XP in creative or spectator mode
			if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
				return;
			}

			var enchantments = serverWorld.getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            if (enchantments.getEntry(Enchantments.SILK_TOUCH).isPresent()) {
                if (serverPlayer.getMainHandStack().getEnchantments().getLevel(enchantments.getEntry(Enchantments.SILK_TOUCH).get()) > 0) {
                    return;
                }
            }

			// Get XP from block
			String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
			int age = -1;
			if (state.contains(Properties.AGE_7)) {
				age = state.get(Properties.AGE_7);
			} else if (state.contains(Properties.AGE_3)) {
				age = state.get(Properties.AGE_3);
			} else if (state.contains(Properties.AGE_2)) {
				age = state.get(Properties.AGE_2);
			}
			int xp = Config.getBlockXp(blockId, age);
			
			if (xp > 0) {
				serverPlayer.addExperience(xp);
				LOGGER.debug("Awarded {} XP to {} for breaking {}", xp, serverPlayer.getName().getString(), blockId);
			}
		});
	}
}