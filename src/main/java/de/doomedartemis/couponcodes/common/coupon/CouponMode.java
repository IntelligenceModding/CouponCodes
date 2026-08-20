package de.doomedartemis.couponcodes.common.coupon;

import java.util.Locale;

public enum CouponMode {
    SINGLE_USE,
    USES,
    TIMED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String commandName() {
        return switch (this) {
            case SINGLE_USE -> "once";
            case USES -> "multi";
            case TIMED -> "timed";
        };
    }
}
