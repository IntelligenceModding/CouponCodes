package de.doomedartemis.couponcodes.compat.curios;

import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.ICurioRenderer;

final class CuriosClientRuntimeCompat {
    private CuriosClientRuntimeCompat() {
    }

    static void register(IEventBus modEventBus) {
        modEventBus.addListener(CuriosClientRuntimeCompat::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModItems.allCouponPouches().forEach(item -> ICurioRenderer.register(item.get(), CouponPouchCurioRenderer::new)));
    }
}
