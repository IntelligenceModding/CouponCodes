package de.artemis.coupon_codes;

import de.artemis.coupon_codes.common.registry.ModCreativeModeTabs;
import de.artemis.coupon_codes.common.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CouponCodes.MOD_ID)
public class CouponCodes {
    public static final String MOD_ID = "coupon_codes";

    public CouponCodes(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
    }
}
