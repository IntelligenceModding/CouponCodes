package de.artemis.coupon_codes.common.registry;

import de.artemis.coupon_codes.CouponCodes;
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
                    .icon(() -> ModItems.BASIC_COUPON.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.coupon_codes"))
                    .displayItems((parameters, output) -> output.accept(ModItems.BASIC_COUPON.get()))
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
