package de.artemis.coupon_codes;

import de.artemis.coupon_codes.client.ClientModEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = CouponCodes.MOD_ID, dist = Dist.CLIENT)
public class CouponCodesClient {
    public CouponCodesClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::onClientSetup);
    }
}
