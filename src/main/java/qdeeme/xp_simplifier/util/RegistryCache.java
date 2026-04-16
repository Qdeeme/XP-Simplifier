package qdeeme.xp_simplifier.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;


public class RegistryCache {

    // Cache registry entry enchantment
    public static RegistryEntry<Enchantment> SILK_TOUCH = null;
    public static RegistryEntry<Enchantment> MENDING = null;

    public static void init(MinecraftServer server) {
        var reg = server.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        reg.getEntry(Enchantments.SILK_TOUCH).ifPresent(e -> SILK_TOUCH = e);
        reg.getEntry(Enchantments.MENDING).ifPresent(e -> MENDING = e);
    }
}
