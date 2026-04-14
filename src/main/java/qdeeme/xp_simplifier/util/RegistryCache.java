package qdeeme.xp_simplifier.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;

/**
 * Caches registry entries that require a live server registry manager.
 * Populated once via SERVER_STARTED — safe to use in any subsequent event handler.
 */
public class RegistryCache {

    /** Silk Touch enchantment registry entry. Null until SERVER_STARTED fires. */
    public static RegistryEntry<Enchantment> SILK_TOUCH = null;

    /** Mending enchantment registry entry. Null until SERVER_STARTED fires. */
    public static RegistryEntry<Enchantment> MENDING = null;

    public static void init(MinecraftServer server) {
        var reg = server.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        reg.getEntry(Enchantments.SILK_TOUCH).ifPresent(e -> SILK_TOUCH = e);
        reg.getEntry(Enchantments.MENDING).ifPresent(e -> MENDING = e);
    }
}
