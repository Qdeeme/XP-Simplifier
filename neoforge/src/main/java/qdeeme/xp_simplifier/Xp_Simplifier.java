package qdeeme.xp_simplifier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import qdeeme.xp_simplifier.event.ModEvents;
import qdeeme.xp_simplifier.permissions.command.EditPermissionCommand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import qdeeme.xp_simplifier.util.RegistryCache;
import qdeeme.xp_simplifier.util.config.ConfigServerSync;
import qdeeme.xp_simplifier.util.config.XpsConfig;


@Mod(Xp_Simplifier.MOD_ID)
public class Xp_Simplifier {
    public static final String MOD_ID = "xp_simplifier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /**
     * Stored so the client setup class can register client extension points without touching client classes here.
     */
    public static ModContainer CONTAINER;

    public Xp_Simplifier(ModContainer container, IEventBus modEventBus) {
        LOGGER.info("Initializing " + MOD_ID);
        CONTAINER = container;

        // IConfigScreenFactory registration is in Xp_Simplifier_Client — hooks the "Mods → Config" button.
        // Load JSON maps + flags early so ConfigScreen works at the main menu.
        modEventBus.addListener(FMLCommonSetupEvent.class, event ->
                event.enqueueWork(XpsConfig::load));


        // ── Network packets ───────────────────────────────────────────────────
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            // optional() — vanilla clients (no mod) pass the handshake; packets simply
            // don't exist for them. Modded clients still receive/send normally.
            final PayloadRegistrar registrar = event.registrar("1").optional();
            registrar.playToClient(
                    ConfigSync.TYPE,
                    ConfigSync.STREAM_CODEC,
                    ConfigSync::handle);
            registrar.playToClient(
                    ConfigDelta.TYPE,
                    ConfigDelta.STREAM_CODEC,
                    ConfigDelta::handle);
            registrar.playToServer(
                    ConfigUpdate.TYPE,
                    ConfigUpdate.STREAM_CODEC,
                    ConfigUpdate::handle);
        });

        // ── Server lifecycle ──────────────────────────────────────────────────
        NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class, event ->
                ConfigServerSync.initServer(event.getServer().getWorldPath(LevelResource.ROOT)));

        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event ->
                ConfigServerSync.clearServer());

        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event ->
                RegistryCache.init(event.getServer()));

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                EditPermissionCommand.register(event.getDispatcher()));

        // ── Sync config to players on join ────────────────────────────────────
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                player.getServer().execute(() -> {
                    try {
                        PacketDistributor.sendToPlayer(player, ConfigSync.forPlayer(player));
                    } catch (UnsupportedOperationException ignored) {
                        // Vanilla client — mod not installed, skip sync
                    } catch (java.util.concurrent.CompletionException e) {
                        if (!(e.getCause() instanceof UnsupportedOperationException)) {
                            LOGGER.error("Unexpected error syncing config to {}", player.getName().getString(), e);
                        }
                        // else vanilla client, ignore
                    }
                });
            }
        });

        // Register handlers
        BlockBreakHandler.register();
        ExperienceBottleHandler.register();
        ModEvents.register();
        OnEntityKill.register();
        SmeltingHandler.register();
        TradingHandler.register();
        BreedingHandler.register();
        FishingHandler.register();
        LOGGER.info("Successfully initialized " + MOD_ID);
    }
}