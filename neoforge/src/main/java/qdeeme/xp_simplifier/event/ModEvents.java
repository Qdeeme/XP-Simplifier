package qdeeme.xp_simplifier.event;

import java.util.UUID;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.trading.Merchant;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import qdeeme.xp_simplifier.util.MerchantOffersData;

public class ModEvents {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModEvents::onMerchantDeath);
    }

    private static void onMerchantDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity entity = event.getEntity();

        // Check if the dead entity is a merchant
        if (entity instanceof Merchant) {
            UUID uuid = entity.getUUID();
            MerchantOffersData.removeMerchant(uuid);
            MerchantOffersData.save();
        }
    }
}