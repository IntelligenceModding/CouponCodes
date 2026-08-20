package de.doomedartemis.couponcodes.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.doomedartemis.couponcodes.common.network.OpenCouponPouchPayload;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import de.doomedartemis.couponcodes.common.registry.ModMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientModEvents {
    private static final KeyMapping OPEN_COUPON_POUCH = new KeyMapping(
            "key.coupon_codes.open_coupon_pouch",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            "key.categories.coupon_codes"
    );

    private ClientModEvents() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_COUPON_POUCH);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.screen != null) {
            return;
        }

        while (OPEN_COUPON_POUCH.consumeClick()) {
            PacketDistributor.sendToServer(OpenCouponPouchPayload.INSTANCE);
        }
    }

    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        CouponIconDecorator decorator = new CouponIconDecorator();
        ModItems.allCoupons().forEach(item -> event.register(item.get(), decorator));
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COUPON_POUCH.get(), CouponPouchScreen::new);
    }

    public static void onBossEventProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!ClientConfig.showTimedCouponBossBar() && isCouponBossBar(event)) {
            event.setCanceled(true);
        }
    }

    private static boolean isCouponBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        ComponentContents contents = event.getBossEvent().getName().getContents();
        return contents instanceof TranslatableContents translatable
                && ("bossbar.coupon_codes.timed_coupon".equals(translatable.getKey())
                || "bossbar.coupon_codes.timed_coupon.empty".equals(translatable.getKey()));
    }
}
