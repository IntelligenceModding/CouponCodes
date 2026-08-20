package de.doomedartemis.couponcodes.common.registry;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CouponCodes.MOD_ID);

    public static final Supplier<CreativeModeTab> COUPON_CODES_TAB = CREATIVE_MODE_TABS.register("coupon_codes_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.couponItem(CouponEffectType.DURABILITY, CouponMode.TIMED).get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.coupon_codes"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COUPON_POUCH.get());
                        output.accept(ModItems.EMPTY_COUPON.get());
                        ModItems.allCoupons().stream()
                                .filter(item -> CouponConfig.isCouponEnabled(item.get().effect(), item.get().mode()))
                                .forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
