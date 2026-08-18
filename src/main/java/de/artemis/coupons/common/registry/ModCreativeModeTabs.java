package de.artemis.coupons.common.registry;

import de.artemis.coupons.Coupons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Coupons.MOD_ID);

    public static final Supplier<CreativeModeTab> COUPONS_TAB = CREATIVE_MODE_TABS.register("coupons_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.BASIC_COUPON.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.coupons"))
                    .displayItems((parameters, output) -> output.accept(ModItems.BASIC_COUPON.get()))
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
