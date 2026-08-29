package de.doomedartemis.couponcodes.common.network;

import de.doomedartemis.couponcodes.compat.curios.CuriosCompat;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(OpenCouponPouchPayload.TYPE, OpenCouponPouchPayload.STREAM_CODEC, ModNetwork::openCouponPouch);
        registrar.playToServer(SortCouponPouchPayload.TYPE, SortCouponPouchPayload.STREAM_CODEC, ModNetwork::sortCouponPouch);
        registrar.playToServer(ToggleCouponPouchAutoActivationPayload.TYPE, ToggleCouponPouchAutoActivationPayload.STREAM_CODEC, ModNetwork::toggleCouponPouchAutoActivation);
    }

    private static void openCouponPouch(OpenCouponPouchPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            openFirstCarriedPouch(player);
        }
    }

    private static void sortCouponPouch(SortCouponPouchPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof CouponPouchMenu pouchMenu) {
            pouchMenu.sortPouchItems();
        }
    }

    private static void toggleCouponPouchAutoActivation(ToggleCouponPouchAutoActivationPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof CouponPouchMenu pouchMenu) {
            pouchMenu.toggleAutoActivation();
        }
    }

    private static void openFirstCarriedPouch(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (CouponPouchItem.open(player, stack)) {
                return;
            }
        }

        ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (CouponPouchItem.open(player, offhand)) {
            return;
        }

        CuriosCompat.openFirstPouch(player);
    }
}
