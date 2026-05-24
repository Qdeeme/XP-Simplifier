package qdeeme.xp_simplifier.data;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import qdeeme.xp_simplifier.data.lang.XpsLangProviderEN;
import qdeeme.xp_simplifier.Xp_Simplifier;
import qdeeme.xp_simplifier.data.lang.XpsLangProviderPL;

@EventBusSubscriber(modid = Xp_Simplifier.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class XpsDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new XpsLangProviderEN(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(event.includeClient(), new XpsLangProviderPL(event.getGenerator().getPackOutput()));
    }
}

