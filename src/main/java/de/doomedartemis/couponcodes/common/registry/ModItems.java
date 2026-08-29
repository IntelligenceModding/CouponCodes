package de.doomedartemis.couponcodes.common.registry;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponCategory;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.item.EmptyCouponItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CouponCodes.MOD_ID);

    public static final DeferredItem<EmptyCouponItem> EMPTY_COUPON = ITEMS.registerItem(
            "empty_coupon",
            EmptyCouponItem::new,
            () -> new Item.Properties().stacksTo(64).rarity(Rarity.RARE)
    );

    public static final DeferredItem<CouponPouchItem> COUPON_POUCH = ITEMS.registerItem(
            "coupon_pouch",
            CouponPouchItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    );

    private static final List<String> COUPON_POUCH_COLORS = List.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
    );
    private static final Map<String, DeferredItem<CouponPouchItem>> COLORED_COUPON_POUCHES = new LinkedHashMap<>();
    private static final Map<CouponEffectType, Map<CouponMode, DeferredItem<CouponItem>>> COUPONS = new EnumMap<>(CouponEffectType.class);

    private ModItems() {
    }

    static {
        for (String color : COUPON_POUCH_COLORS) {
            COLORED_COUPON_POUCHES.put(color, ITEMS.registerItem(
                    color + "_coupon_pouch",
                    CouponPouchItem::new,
                    () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
            ));
        }
        for (CouponEffectType effect : CouponEffectType.values()) {
            EnumMap<CouponMode, DeferredItem<CouponItem>> byMode = new EnumMap<>(CouponMode.class);
            for (CouponMode mode : CouponMode.values()) {
                byMode.put(mode, registerCoupon(itemName(effect, mode), effect, mode));
            }
            COUPONS.put(effect, byMode);
        }
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static DeferredItem<CouponItem> couponItem(CouponEffectType effect, CouponMode mode) {
        return COUPONS.get(effect).get(mode);
    }

    public static List<DeferredItem<CouponItem>> allCoupons() {
        return COUPONS.values().stream()
                .flatMap(byMode -> byMode.values().stream())
                .toList();
    }

    public static List<DeferredItem<CouponPouchItem>> allCouponPouches() {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(COUPON_POUCH),
                        COLORED_COUPON_POUCHES.values().stream()
                )
                .toList();
    }

    public static Optional<DeferredItem<CouponPouchItem>> coloredCouponPouch(DyeColor color) {
        return Optional.ofNullable(COLORED_COUPON_POUCHES.get(color.getName()));
    }

    public static Optional<DeferredItem<CouponItem>> randomCoupon(RandomSource random) {
        return randomCoupon(random, null);
    }

    public static Optional<DeferredItem<CouponItem>> randomCoupon(RandomSource random, CouponCategory category) {
        int totalWeight = 0;
        for (CouponEffectType effect : CouponEffectType.values()) {
            if (category != null && effect.category() != category) {
                continue;
            }
            for (CouponMode mode : CouponMode.values()) {
                totalWeight += rollWeight(effect, mode);
            }
        }
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        int selectedWeight = random.nextInt(totalWeight);
        for (CouponEffectType effect : CouponEffectType.values()) {
            if (category != null && effect.category() != category) {
                continue;
            }
            for (CouponMode mode : CouponMode.values()) {
                selectedWeight -= rollWeight(effect, mode);
                if (selectedWeight < 0) {
                    return Optional.of(couponItem(effect, mode));
                }
            }
        }

        return Optional.empty();
    }

    private static DeferredItem<CouponItem> registerCoupon(String name, CouponEffectType effect, CouponMode mode) {
        return ITEMS.registerItem(
                name,
                properties -> new CouponItem(effect, mode, properties),
                () -> new Item.Properties().stacksTo(1).rarity(couponRarity(effect, mode))
        );
    }

    public static Rarity couponRarity(CouponEffectType effect, CouponMode mode) {
        int score = baseRarityScore(effect) + modeRarityBonus(mode);
        return switch (Math.min(score, 3)) {
            case 0 -> Rarity.COMMON;
            case 1 -> Rarity.UNCOMMON;
            case 2 -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }

    private static int rollWeight(CouponEffectType effect, CouponMode mode) {
        if (!CouponConfig.isCouponEnabled(effect, mode)) {
            return 0;
        }
        return CouponConfig.rollWeight(couponRarity(effect, mode));
    }

    private static int baseRarityScore(CouponEffectType effect) {
        return switch (effect) {
            case DURABILITY, FOOD, BONE_MEAL, FISHING -> 0;
            case ENCHANTING_EXPERIENCE, ARROW, POTION_DURATION, MENDING, ROCKET, ENDER_PEARL, ELYTRA_GLIDE, FALL_DAMAGE -> 1;
            case ANVIL_EXPERIENCE, TOOL_REPAIR, VILLAGER_TRADE, VILLAGER_RESTOCK, SMITHING_TEMPLATE, REPAIR_MATERIAL -> 2;
            case TOTEM, DEATH_DROP -> 3;
        };
    }

    private static int modeRarityBonus(CouponMode mode) {
        return switch (mode) {
            case SINGLE_USE -> 0;
            case USES, TIMED -> 1;
        };
    }

    private static String itemName(CouponEffectType effect, CouponMode mode) {
        return effect.id() + "_" + mode.commandName() + "_coupon";
    }
}
