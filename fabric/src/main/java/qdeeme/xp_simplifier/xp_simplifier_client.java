package qdeeme.xp_simplifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import qdeeme.xp_simplifier.network.ConfigDelta;
import qdeeme.xp_simplifier.network.ConfigSync;
import qdeeme.xp_simplifier.util.ConfigServerSync;

/**
 * Client-only initializer. Registers S2C packet receivers for config sync.
 * Safe to reference client-only classes here.
 */
public class xp_simplifier_client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Receive full config snapshot on login
        ClientPlayNetworking.registerGlobalReceiver(ConfigSync.ID, (payload, context) ->
                context.client().execute(() -> ConfigServerSync.setClientShadow(payload.values())));

        // Receive partial config updates after an op changes a setting
        ClientPlayNetworking.registerGlobalReceiver(ConfigDelta.ID, (payload, context) ->
                context.client().execute(() -> ConfigServerSync.applyClientDelta(payload.changes())));
    }
}

