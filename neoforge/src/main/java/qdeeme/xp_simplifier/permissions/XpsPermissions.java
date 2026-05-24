package qdeeme.xp_simplifier.permissions;

import net.minecraft.server.level.ServerPlayer;

/**
 * Central authority for XP-Simplifier permission checks.
 *
 * <p>Command-level access (add/edit/delete/mapview) is controlled by vanilla op levels,
 * which LuckPerms intercepts automatically via its own command-node system.
 * The {@link #canEdit} method additionally applies the per-world file override layer.</p>
 */
public final class XpsPermissions {

    private XpsPermissions() {
    }

    /**
     * Three-way priority check for config editing:
     * <ol>
     *   <li>Explicit file entry {@code false} → deny (hard veto)</li>
     *   <li>Explicit file entry {@code true}  → allow (hard grant)</li>
     *   <li>No file entry                     → requires op level 4</li>
     * </ol>
     */
    public static boolean canEdit(ServerPlayer player) {
        Boolean explicit = XpsEditPermission.canEditExplicit(player.getGameProfile().getName());
        if (explicit == null) return false;
        if (!explicit) return false;
        return explicit && player.hasPermissions(2);
    }
}


