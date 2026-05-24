package qdeeme.xp_simplifier.util;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;


public class RegistryCache {

    // Cache registry entry enchantment
    public static Holder<Enchantment> SILK_TOUCH = null;
    public static Holder<Enchantment> MENDING = null;

    public static void init(MinecraftServer server) {
        var reg = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        reg.getHolder(Enchantments.SILK_TOUCH).ifPresent(e -> SILK_TOUCH = e);
        reg.getHolder(Enchantments.MENDING).ifPresent(e -> MENDING = e);
    }
}
