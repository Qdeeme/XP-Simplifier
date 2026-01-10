package qdeeme.xp_simplifier.handler;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import qdeeme.xp_simplifier.util.Config;


public class OnEntityKill {
    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer) {
                // Don't give XP in creative or spectator mode
                if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
                    return;
                }
                
                String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                int xp = 0;
                
                // If entity kill XP is enabled
                if (Config.isEntityKillXpEnabled()) {
                    // Check if entity has config
                    if (Config.hasEntityXpConfig(entityId)) {
                        // Use config value (could be 0 or positive)
                        xp = Config.getEntityXp(entityId);
                    } else {
                        // Entity not in config, use vanilla
                        xp = entity.getXpToDrop((ServerWorld) entity.getWorld(), source.getAttacker());
                    }
                } 
                // If entity kill XP is disabled
                else {
                    // Always use vanilla XP
                    xp = entity.getXpToDrop((ServerWorld) entity.getWorld(), source.getAttacker());
                }
                
                // Give XP if any
                if (xp > 0) {
                    serverPlayer.addExperience(xp);
                }
            }
        });
    }
}
