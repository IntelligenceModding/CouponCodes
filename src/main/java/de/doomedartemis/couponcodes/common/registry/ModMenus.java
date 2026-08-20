package de.doomedartemis.couponcodes.common.registry;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CouponCodes.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CouponPouchMenu>> COUPON_POUCH =
            MENUS.register("coupon_pouch", () -> IMenuTypeExtension.create(CouponPouchMenu::createClient));

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
