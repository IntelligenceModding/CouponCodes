package de.artemis.coupon_codes.common.coupon;

import de.artemis.coupon_codes.common.item.CouponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;

public final class CouponData {
    private static final String INITIALIZED_KEY = "Initialized";
    private static final String ACTIVE_KEY = "Active";
    private static final String DISCOUNT_KEY = "DiscountPercent";
    private static final String USES_KEY = "UsesRemaining";
    private static final String TICKS_KEY = "TicksRemaining";
    private static final String INITIAL_TICKS_KEY = "InitialTicks";

    private static final int[] DISCOUNTS = {10, 20, 25, 50};

    private CouponData() {
    }

    public static void initializeIfNeeded(ItemStack stack, CouponItem coupon, RandomSource random) {
        CompoundTag tag = tag(stack);
        if (tag.getBoolean(INITIALIZED_KEY)) {
            return;
        }

        int discount = DISCOUNTS[random.nextInt(DISCOUNTS.length)];
        int value = switch (coupon.mode()) {
            case SINGLE_USE -> 1;
            case USES -> 2 + random.nextInt(4);
            case TIMED -> (5 + random.nextInt(3) * 5) * 60;
        };

        set(stack, coupon.mode(), discount, value, false);
    }

    public static void set(ItemStack stack, CouponMode mode, int discountPercent, int value, boolean active) {
        CompoundTag tag = tag(stack);
        tag.putBoolean(INITIALIZED_KEY, true);
        tag.putInt(DISCOUNT_KEY, Mth.clamp(discountPercent, 1, 95));

        if (mode == CouponMode.TIMED) {
            int ticks = Math.max(1, value) * 20;
            tag.putInt(TICKS_KEY, ticks);
            tag.putInt(INITIAL_TICKS_KEY, ticks);
            tag.putBoolean(ACTIVE_KEY, active);
            tag.remove(USES_KEY);
        } else {
            tag.putInt(USES_KEY, mode == CouponMode.SINGLE_USE ? 1 : Math.max(1, value));
            tag.remove(TICKS_KEY);
            tag.remove(INITIAL_TICKS_KEY);
            tag.remove(ACTIVE_KEY);
        }

        save(stack, tag);
    }

    public static void activateTimed(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.getBoolean(INITIALIZED_KEY)) {
            return;
        }

        tag.putBoolean(ACTIVE_KEY, true);
        save(stack, tag);
    }

    public static boolean isTimedActive(ItemStack stack) {
        return tag(stack).getBoolean(ACTIVE_KEY);
    }

    public static boolean isActiveTimedCoupon(ItemStack stack, CouponItem coupon) {
        return coupon.mode() == CouponMode.TIMED && isTimedActive(stack) && !isExpired(stack, coupon);
    }

    public static float timedProgress(ItemStack stack) {
        CompoundTag tag = tag(stack);
        int initialTicks = tag.getInt(INITIAL_TICKS_KEY);
        int ticksRemaining = tag.getInt(TICKS_KEY);

        if (initialTicks <= 0) {
            initialTicks = Math.max(1, ticksRemaining);
            tag.putInt(INITIAL_TICKS_KEY, initialTicks);
            save(stack, tag);
        }

        return Mth.clamp(ticksRemaining / (float) initialTicks, 0.0F, 1.0F);
    }

    public static Component bossBarName(ItemStack stack, CouponItem coupon) {
        return Component.translatable(
                "bossbar.coupon_codes.timed_coupon",
                Component.translatable("item.coupon_codes.coupon.effect." + coupon.effect().name().toLowerCase(Locale.ROOT), discountPercent(stack)),
                secondsRemaining(stack)
        );
    }

    public static void tick(ItemStack stack, CouponItem coupon) {
        if (coupon.mode() != CouponMode.TIMED || !isTimedActive(stack)) {
            return;
        }

        CompoundTag tag = tag(stack);
        int ticksRemaining = tag.getInt(TICKS_KEY);
        if (ticksRemaining > 0) {
            int updatedTicks = ticksRemaining - 1;
            tag.putInt(TICKS_KEY, updatedTicks);
            save(stack, tag);

            if (updatedTicks <= 0) {
                stack.shrink(1);
            }
        }
    }

    public static void tickCouponsInInventory(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CouponItem coupon) {
                initializeIfNeeded(stack, coupon, player.getRandom());
                tick(stack, coupon);
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof CouponItem coupon) {
                initializeIfNeeded(stack, coupon, player.getRandom());
                tick(stack, coupon);
            }
        }
    }

    public static boolean matches(ItemStack stack, CouponItem coupon, CouponEffectType effect) {
        if (coupon.effect() != effect || isExpired(stack, coupon)) {
            return false;
        }

        return coupon.mode() != CouponMode.TIMED || isTimedActive(stack);
    }

    public static int discountPercent(ItemStack stack) {
        return Mth.clamp(tag(stack).getInt(DISCOUNT_KEY), 0, 95);
    }

    public static void consumeUse(ItemStack stack, CouponItem coupon) {
        if (coupon.mode() == CouponMode.TIMED) {
            return;
        }

        CompoundTag tag = tag(stack);
        int usesRemaining = Math.max(0, tag.getInt(USES_KEY) - 1);
        tag.putInt(USES_KEY, usesRemaining);
        save(stack, tag);

        if (usesRemaining <= 0) {
            stack.shrink(1);
        }
    }

    public static boolean isExpired(ItemStack stack, CouponItem coupon) {
        CompoundTag tag = tag(stack);
        return switch (coupon.mode()) {
            case TIMED -> tag.getInt(TICKS_KEY) <= 0;
            case SINGLE_USE, USES -> tag.getInt(USES_KEY) <= 0;
        };
    }

    public static void appendHoverText(ItemStack stack, CouponItem coupon, List<Component> tooltip) {
        if (!tag(stack).getBoolean(INITIALIZED_KEY)) {
            tooltip.add(Component.translatable("item.coupon_codes.coupon.unrolled").withStyle(ChatFormatting.GRAY));
            return;
        }

        int discount = discountPercent(stack);
        tooltip.add(Component.translatable("item.coupon_codes.coupon.effect." + coupon.effect().name().toLowerCase(Locale.ROOT), discount)
                .withStyle(ChatFormatting.GOLD));

        if (coupon.mode() == CouponMode.TIMED) {
            String stateKey = isTimedActive(stack) ? "active" : "inactive";
            tooltip.add(Component.translatable("item.coupon_codes.coupon.mode.timed." + stateKey, secondsRemaining(stack))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.coupon_codes.coupon.mode.uses", tag(stack).getInt(USES_KEY))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static int secondsRemaining(ItemStack stack) {
        return Math.max(0, tag(stack).getInt(TICKS_KEY) / 20);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}
