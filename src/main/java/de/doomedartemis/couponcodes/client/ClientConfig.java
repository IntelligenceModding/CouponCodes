package de.doomedartemis.couponcodes.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue SHOW_TIMED_COUPON_BOSS_BAR;
    private static final ModConfigSpec.BooleanValue SHOW_COUPON_ICON_OVERLAYS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        SHOW_TIMED_COUPON_BOSS_BAR = builder
                .comment("Shows coupon timed-effect boss bars on this client. This only affects local rendering.")
                .translation("config.coupon_codes.client.show_timed_coupon_boss_bar")
                .define("showTimedCouponBossBar", true);
        SHOW_COUPON_ICON_OVERLAYS = builder
                .comment("Shows small effect icons over coupon item stacks on this client.")
                .translation("config.coupon_codes.client.show_coupon_icon_overlays")
                .define("showCouponIconOverlays", true);

        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static boolean showTimedCouponBossBar() {
        return SHOW_TIMED_COUPON_BOSS_BAR.get();
    }

    public static boolean showCouponIconOverlays() {
        return SHOW_COUPON_ICON_OVERLAYS.get();
    }
}
