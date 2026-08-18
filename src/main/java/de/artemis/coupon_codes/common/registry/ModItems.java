package de.artemis.coupon_codes.common.registry;

import de.artemis.coupon_codes.CouponCodes;
import de.artemis.coupon_codes.common.coupon.CouponEffectType;
import de.artemis.coupon_codes.common.coupon.CouponMode;
import de.artemis.coupon_codes.common.item.CouponItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CouponCodes.MOD_ID);

    public static final DeferredItem<CouponItem> DURABILITY_ONCE_COUPON = registerCoupon(
            "durability_once_coupon",
            CouponEffectType.DURABILITY,
            CouponMode.SINGLE_USE);

    public static final DeferredItem<CouponItem> DURABILITY_MULTI_COUPON = registerCoupon(
            "durability_multi_coupon",
            CouponEffectType.DURABILITY,
            CouponMode.USES);

    public static final DeferredItem<CouponItem> DURABILITY_TIMED_COUPON = registerCoupon(
            "durability_timed_coupon",
            CouponEffectType.DURABILITY,
            CouponMode.TIMED);

    public static final DeferredItem<CouponItem> ENCHANTING_EXPERIENCE_ONCE_COUPON = registerCoupon(
            "enchanting_experience_once_coupon",
            CouponEffectType.ENCHANTING_EXPERIENCE,
            CouponMode.SINGLE_USE);

    public static final DeferredItem<CouponItem> ENCHANTING_EXPERIENCE_MULTI_COUPON = registerCoupon(
            "enchanting_experience_multi_coupon",
            CouponEffectType.ENCHANTING_EXPERIENCE,
            CouponMode.USES);

    public static final DeferredItem<CouponItem> ENCHANTING_EXPERIENCE_TIMED_COUPON = registerCoupon(
            "enchanting_experience_timed_coupon",
            CouponEffectType.ENCHANTING_EXPERIENCE,
            CouponMode.TIMED);

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static DeferredItem<CouponItem> registerCoupon(String name, CouponEffectType effect, CouponMode mode) {
        return ITEMS.registerItem(
                name,
                properties -> new CouponItem(effect, mode, properties),
                new Item.Properties().stacksTo(1)
        );
    }
}
