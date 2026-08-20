package de.doomedartemis.couponcodes.common.coupon;

import java.util.Locale;

public enum CouponCategory {
    EQUIPMENT,
    MAGIC,
    TRADE,
    CONSUMABLES,
    MOBILITY,
    COMBAT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName(id());
    }

    static String displayName(String id) {
        String[] words = id.split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }
}
