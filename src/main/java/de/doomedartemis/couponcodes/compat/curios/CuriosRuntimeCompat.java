package de.doomedartemis.couponcodes.compat.curios;

import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.List;

final class CuriosRuntimeCompat {
    private static final ICurioItem COUPON_POUCH_CURIO = new ICurioItem() {
    };

    private CuriosRuntimeCompat() {
    }

    static void register(IEventBus modEventBus) {
        modEventBus.addListener(CuriosRuntimeCompat::onCommonSetup);
    }

    static void tickCouponsInEquippedCurios(Player player, RandomSource random) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getCurios().values().forEach(stacksHandler -> {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                CouponData.tickCouponsInCarriedStack(player, stacks.getStackInSlot(slot), random);
            }
        }));
    }

    static void collectCarriedCoupons(Player player, List<CouponData.CarriedCoupon> coupons, CouponPouchMenu openPouch) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getCurios().values().forEach(stacksHandler -> {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                int equippedSlot = slot;
                ItemStack stack = stacks.getStackInSlot(equippedSlot);
                CouponData.collectCarriedCouponsFromStack(
                        player,
                        stack,
                        () -> stacks.setStackInSlot(equippedSlot, stack),
                        coupons,
                        openPouch
                );
            }
        }));
    }

    static boolean openFirstPouch(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof CouponPouchItem))
                .map(slotResult -> CouponPouchItem.open(player, slotResult.stack()))
                .orElse(false);
    }

    static boolean isEquipped(Player player, ItemStack stack) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(equippedStack -> equippedStack == stack || ItemStack.matches(equippedStack, stack)))
                .isPresent();
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ModItems.allCouponPouches().forEach(item -> CuriosApi.registerCurio(item.get(), COUPON_POUCH_CURIO)));
    }
}
