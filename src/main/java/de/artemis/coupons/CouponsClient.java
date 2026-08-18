package de.artemis.coupons;

import de.artemis.coupons.client.ClientModEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = Coupons.MOD_ID, dist = Dist.CLIENT)
public class CouponsClient {
    public CouponsClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::onClientSetup);
    }
}
