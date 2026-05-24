package qdeeme.xp_simplifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.WorldSavePath;

import qdeeme.xp_simplifier.handler.BlockBreakHandler;
import qdeeme.xp_simplifier.handler.BreedingHandler;
import qdeeme.xp_simplifier.handler.ExperienceBottleHandler;
import qdeeme.xp_simplifier.handler.FishingHandler;
import qdeeme.xp_simplifier.handler.OnEntityKill;
import qdeeme.xp_simplifier.handler.SmeltingHandler;
import qdeeme.xp_simplifier.handler.TradingHandler;
import qdeeme.xp_simplifier.network.ConfigDelta;
import qdeeme.xp_simplifier.network.ConfigSync;
import qdeeme.xp_simplifier.network.ConfigUpdate;
import qdeeme.xp_simplifier.permissions.command.EditPermissionCommand;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.ConfigServerSync;
import qdeeme.xp_simplifier.util.MerchantOffersData;
import qdeeme.xp_simplifier.util.RegistryCache;


public class xp_simplifier implements ModInitializer {
    public static final String MOD_ID = "xp_simplifier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing " + MOD_ID);

        // ── Config load (must happen EARLY for main menu config screen) ─────────
        try {
            Config.load();
            LOGGER.info("Config loaded successfully at mod initialization");
        } catch (Exception e) {
            LOGGER.error("Failed to load config at initialization", e);
        }

        // ── Network packets (G10/G12) ─────────────────────────────────────────
        // Register payload types early (must happen before server communication)
        PayloadTypeRegistry.playS2C().register(ConfigSync.ID, ConfigSync.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigDelta.ID, ConfigDelta.CODEC);
        PayloadTypeRegistry.playC2S().register(ConfigUpdate.ID, ConfigUpdate.CODEC);

        // Server-side receiver:
        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdate.ID, (payload, context) -> {
            context.server().execute(() -> ConfigUpdate.handle(payload, context));
        });

        // ── Server lifecycle (G10) ────────────────────────────────────────────
        // Reload config when server starts (to pick up any runtime changes)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, reloading config");
            Config.load();
            ConfigServerSync.initServer(server.getSavePath(WorldSavePath.ROOT));
            RegistryCache.init(server);
            MerchantOffersData.load();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TradingHandler.saveMerchantData();
            ConfigServerSync.clearServer();
        });

        // ── Player join sync (G11) ────────────────────────────────────────────
        // Deferred by one server tick so permission providers have time to initialise.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> {
                    if (ServerPlayNetworking.canSend(handler.player, ConfigSync.ID)) {
                        ServerPlayNetworking.send(handler.player, ConfigSync.forPlayer(handler.player));
                    }
                }));

        // ── Commands ──────────────────────────────────────────────────────────
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EditPermissionCommand.register(dispatcher));

        // ── Game-event handlers ───────────────────────────────────────────────
        BlockBreakHandler.register();
        ExperienceBottleHandler.register();
        OnEntityKill.register();
        SmeltingHandler.register();
        TradingHandler.register();
        BreedingHandler.register();
        FishingHandler.register();
        LOGGER.info("Successfully initialized " + MOD_ID);
    }
}