package de.artemis.coupons;

import de.artemis.coupons.common.registry.ModCreativeModeTabs;
import de.artemis.coupons.common.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Coupons.MOD_ID)
public class Coupons {
    public static final String MOD_ID = "coupons";

    public Coupons(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
    }
}
