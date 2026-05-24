package qdeeme.xp_simplifier.permissions;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central authority for XP-Simplifier permission checks (Fabric port).
 *
 * <p>Three-way priority:</p>
 * <ol>
 *   <li>Explicit file entry {@code false} → deny (hard veto)</li>
 *   <li>Explicit file entry {@code true}  → allow (hard grant)</li>
 * </ol>
 */
public final class XpsPermissions {

    private XpsPermissions() {
    }

    public static boolean canEdit(ServerPlayerEntity player) {
        Boolean explicit = XpsEditPermission.canEditExplicit(player.getGameProfile().getName());
        if (explicit == null) return false;
        if (!explicit) return false;
        return (explicit && player.hasPermissionLevel(2));
    }
}

