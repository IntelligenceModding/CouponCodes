package de.artemis.coupons.common.registry;

import de.artemis.coupons.Coupons;
import de.artemis.coupons.common.item.CouponItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Coupons.MOD_ID);

    public static final DeferredItem<CouponItem> BASIC_COUPON = ITEMS.registerItem(
            "basic_coupon",
            CouponItem::new,
            new Item.Properties()
    );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
