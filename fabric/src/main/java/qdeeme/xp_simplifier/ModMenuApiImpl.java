package qdeeme.xp_simplifier;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import qdeeme.xp_simplifier.screen.XpsConfigScreen;

/**
 * ModMenu integration — wires the "Mods → Config" button to our custom screen.
 */
public class ModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return XpsConfigScreen::new;
    }
}

