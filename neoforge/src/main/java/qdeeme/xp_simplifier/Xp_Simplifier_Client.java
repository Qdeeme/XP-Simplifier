package qdeeme.xp_simplifier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import qdeeme.xp_simplifier.screen.ConfigScreen;

/**
 * Client-only setup. Annotated with {@code Dist.CLIENT} so NeoForge never loads
 * this class on a dedicated server — safe to reference client-only classes here.
 */
@EventBusSubscriber(modid = Xp_Simplifier.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Xp_Simplifier_Client {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        Xp_Simplifier.CONTAINER.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new ConfigScreen(parent));
    }
}
