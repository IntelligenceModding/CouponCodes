package de.doomedartemis.couponcodes.compat.curios;

import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.List;

public final class CuriosCompat {
    private static final String CURIOS_MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static void register(IEventBus modEventBus) {
        if (isLoaded()) {
            CuriosRuntimeCompat.register(modEventBus);
        }
    }

    public static void registerClient(IEventBus modEventBus) {
        if (isLoaded()) {
            CuriosClientRuntimeCompat.register(modEventBus);
        }
    }

    public static void tickCouponsInEquippedCurios(Player player, RandomSource random) {
        if (isLoaded()) {
            CuriosRuntimeCompat.tickCouponsInEquippedCurios(player, random);
        }
    }

    public static void collectCarriedCoupons(Player player, List<CouponData.CarriedCoupon> coupons, CouponPouchMenu openPouch) {
        if (isLoaded()) {
            CuriosRuntimeCompat.collectCarriedCoupons(player, coupons, openPouch);
        }
    }

    public static boolean openFirstPouch(ServerPlayer player) {
        return isLoaded() && CuriosRuntimeCompat.openFirstPouch(player);
    }

    public static boolean isEquipped(Player player, ItemStack stack) {
        return isLoaded() && CuriosRuntimeCompat.isEquipped(player, stack);
    }

    private static boolean isLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }
}
