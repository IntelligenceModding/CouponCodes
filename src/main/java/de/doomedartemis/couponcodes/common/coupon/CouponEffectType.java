package de.doomedartemis.couponcodes.common.coupon;

import java.util.Locale;

public enum CouponEffectType {
    DURABILITY(CouponCategory.EQUIPMENT),
    ENCHANTING_EXPERIENCE(CouponCategory.MAGIC),
    ANVIL_EXPERIENCE(CouponCategory.EQUIPMENT),
    TOOL_REPAIR(CouponCategory.EQUIPMENT),
    VILLAGER_TRADE(CouponCategory.TRADE),
    VILLAGER_RESTOCK(CouponCategory.TRADE),
    ARROW(CouponCategory.COMBAT),
    FOOD(CouponCategory.CONSUMABLES),
    POTION_DURATION(CouponCategory.MAGIC),
    MENDING(CouponCategory.MAGIC),
    TOTEM(CouponCategory.COMBAT),
    SMITHING_TEMPLATE(CouponCategory.EQUIPMENT),
    REPAIR_MATERIAL(CouponCategory.EQUIPMENT),
    BONE_MEAL(CouponCategory.CONSUMABLES),
    FISHING(CouponCategory.CONSUMABLES),
    ROCKET(CouponCategory.MOBILITY),
    ENDER_PEARL(CouponCategory.MOBILITY),
    ELYTRA_GLIDE(CouponCategory.MOBILITY),
    FALL_DAMAGE(CouponCategory.MOBILITY),
    DEATH_DROP(CouponCategory.COMBAT);

    private final CouponCategory category;

    CouponEffectType(CouponCategory category) {
        this.category = category;
    }

    public CouponCategory category() {
        return category;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String commandName() {
        return id().replace("_", "");
    }

    public String displayName() {
        return CouponCategory.displayName(id());
    }
}
