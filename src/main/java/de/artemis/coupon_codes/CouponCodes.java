package de.artemis.coupon_codes;

import de.artemis.coupon_codes.common.command.CouponCommands;
import de.artemis.coupon_codes.common.event.CouponBossBars;
import de.artemis.coupon_codes.common.event.CouponEvents;
import de.artemis.coupon_codes.common.registry.ModCreativeModeTabs;
import de.artemis.coupon_codes.common.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CouponCodes.MOD_ID)
public class CouponCodes {
    public static final String MOD_ID = "coupon_codes";

    public CouponCodes(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onPlayerEnchantItem);
        NeoForge.EVENT_BUS.addListener(CouponBossBars::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(CouponCommands::register);
    }
}
