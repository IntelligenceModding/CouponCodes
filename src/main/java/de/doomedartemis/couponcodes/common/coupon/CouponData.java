package de.doomedartemis.couponcodes.common.coupon;

import de.doomedartemis.couponcodes.common.advancement.CouponCriteria;
import de.doomedartemis.couponcodes.compat.curios.CuriosCompat;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CouponData {
    private static final Map<UUID, List<ItemStack>> ACTIVE_TIMED_COUPONS = new HashMap<>();

    private static final String INITIALIZED_KEY = "Initialized";
    private static final String ACTIVE_KEY = "Active";
    private static final String DISCOUNT_KEY = "DiscountPercent";
    private static final String USES_KEY = "UsesRemaining";
    private static final String TICKS_KEY = "TicksRemaining";
    private static final String INITIAL_TICKS_KEY = "InitialTicks";
    private static final String BOOST_SPARE_DAY_KEY = "DailyBoostSpareDay";
    private static final String BOOST_SPARE_READY_KEY = "DailyBoostSpareReady";
    private static final String BOOST_FREE_USES_KEY = "DailyBoostFreeUses";
    private static final String BOOST_USE_MULTIPLIER_KEY = "DailyBoostUseMultiplier";
    private static final String BEST_CARRIED_KEY = "BestCarriedCoupon";

    private CouponData() {
    }

    public record CarriedCoupon(ItemStack stack, CouponItem coupon, Runnable save) {
        public void consumeUse() {
            CouponData.consumeUse(stack, coupon);
            save.run();
        }

        public void consumeUse(Player player) {
            CouponFeedback.playUse(player, coupon);
            if (player instanceof ServerPlayer serverPlayer) {
                CouponCriteria.triggerUsed(serverPlayer, coupon.effect(), coupon.mode());
            }
            CouponData.consumeUse(stack, coupon, player.level());
            save.run();
        }
    }

    public record CouponInventoryStats(
            int carriedCoupons,
            int uninitializedCoupons,
            int activeTimedCoupons,
            Map<CouponCategory, Integer> categoryCounts,
            Map<CouponMode, Integer> modeCounts
    ) {
    }

    public static void initializeIfNeeded(ItemStack stack, CouponItem coupon, RandomSource random) {
        if (!CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())) {
            return;
        }

        CompoundTag tag = tag(stack);
        if (tag.getBoolean(INITIALIZED_KEY)) {
            return;
        }

        int discount = CouponConfig.randomDiscount(random);
        int value = switch (coupon.mode()) {
            case SINGLE_USE -> 1;
            case USES -> CouponConfig.randomMultiUses(coupon.effect(), random);
            case TIMED -> CouponConfig.randomTimedSeconds(coupon.effect(), random);
        };

        set(stack, coupon.mode(), discount, value, false);
    }

    public static void set(ItemStack stack, CouponMode mode, int discountPercent, int value, boolean active) {
        CompoundTag tag = tag(stack);
        tag.putBoolean(INITIALIZED_KEY, true);
        tag.putInt(DISCOUNT_KEY, Mth.clamp(discountPercent, 1, 95));

        CouponEffectType effect = stack.getItem() instanceof CouponItem coupon && coupon.mode() == mode ? coupon.effect() : null;
        if (mode == CouponMode.TIMED) {
            CouponConfig.IntRange range = CouponConfig.timedSecondsRange(effect);
            int ticks = Mth.clamp(value, range.min(), range.max()) * 20;
            tag.putInt(TICKS_KEY, ticks);
            tag.putInt(INITIAL_TICKS_KEY, ticks);
            tag.putBoolean(ACTIVE_KEY, active);
            tag.remove(USES_KEY);
        } else {
            int uses = 1;
            if (mode == CouponMode.USES) {
                CouponConfig.IntRange range = CouponConfig.multiUseRange(effect);
                uses = Mth.clamp(value, range.min(), range.max());
            }
            tag.putInt(USES_KEY, uses);
            tag.remove(TICKS_KEY);
            tag.remove(INITIAL_TICKS_KEY);
            tag.remove(ACTIVE_KEY);
        }

        save(stack, tag);
    }

    public static boolean activateTimed(Player player, ItemStack stack, CouponItem coupon) {
        if (coupon.mode() != CouponMode.TIMED || !CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())) {
            return false;
        }

        removeExpiredActiveTimedCoupons(player);
        List<ItemStack> activeCoupons = activeTimedCoupons(player);
        if (CouponConfig.allowTimedCouponDurationStacking()) {
            for (ItemStack activeCoupon : activeCoupons) {
                if (!(activeCoupon.getItem() instanceof CouponItem activeCouponItem)) {
                    continue;
                }

                if (sameTimedCoupon(stack, coupon, activeCoupon, activeCouponItem)) {
                    addTimedCouponDuration(activeCoupon, stack);
                    markActiveTimedCouponsDirty(player);
                    if (player instanceof ServerPlayer serverPlayer) {
                        CouponCriteria.triggerActivated(serverPlayer, coupon.effect(), coupon.mode());
                    }
                    stack.shrink(1);
                    return true;
                }
            }
        }

        if (activeCoupons.size() >= CouponConfig.maxActiveTimedCoupons()) {
            return false;
        }

        ItemStack activeCoupon = stack.copy();
        activeCoupon.setCount(1);
        activateTimed(activeCoupon);
        activeCoupons.add(activeCoupon);
        markActiveTimedCouponsDirty(player);
        if (player instanceof ServerPlayer serverPlayer) {
            CouponCriteria.triggerActivated(serverPlayer, coupon.effect(), coupon.mode());
        }
        stack.shrink(1);
        return true;
    }

    private static void activateTimed(ItemStack stack) {
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

    public static boolean isInitialized(ItemStack stack) {
        return tag(stack).getBoolean(INITIALIZED_KEY);
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

    public static Component bossBarName(ItemStack stack, CouponItem coupon, Level level) {
        return Component.translatable(
                "bossbar.coupon_codes.timed_coupon",
                Component.translatable("item.coupon_codes.coupon.effect." + coupon.effect().id(), discountPercent(stack, coupon, level)),
                secondsRemaining(stack, coupon, level)
        );
    }

    public static void tick(ItemStack stack, CouponItem coupon) {
        tick(stack, coupon, null);
    }

    public static void tick(ItemStack stack, CouponItem coupon, Level level) {
        if (coupon.mode() != CouponMode.TIMED || !isTimedActive(stack)) {
            return;
        }

        int durationMultiplier = CouponDailyBoost.isBoosted(level, coupon.effect(), coupon.mode()) ? CouponConfig.dailyBoostDurationMultiplier() : 1;
        if (durationMultiplier > 1 && Math.floorMod(level.getGameTime(), durationMultiplier) != 0) {
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
        tickCouponsInStacks(player, player.getInventory().items, player.getRandom(), 0);
        tickCouponsInStacks(player, player.getInventory().offhand, player.getRandom(), 0);
        CuriosCompat.tickCouponsInEquippedCurios(player, player.getRandom());
        updateBestCarriedMarkers(player);
        tickActiveTimedCoupons(player);
    }

    public static void tickCouponsInCarriedStack(Player player, ItemStack stack, RandomSource random) {
        tickCouponsInStack(player, stack, random, 0);
    }

    public static CouponInventoryStats inventoryStats(Player player) {
        EnumMap<CouponCategory, Integer> categoryCounts = new EnumMap<>(CouponCategory.class);
        EnumMap<CouponMode, Integer> modeCounts = new EnumMap<>(CouponMode.class);
        int carried = 0;
        int uninitialized = 0;

        for (CarriedCoupon carriedCoupon : carriedCoupons(player)) {
            ItemStack stack = carriedCoupon.stack();
            CouponItem coupon = carriedCoupon.coupon();
            int count = stack.getCount();
            carried += count;
            categoryCounts.merge(coupon.effect().category(), count, Integer::sum);
            modeCounts.merge(coupon.mode(), count, Integer::sum);
            if (!isInitialized(stack)) {
                uninitialized += count;
            }
        }

        int activeTimed = 0;
        for (ItemStack stack : activeTimedCouponsOrEmpty(player)) {
            if (stack.getItem() instanceof CouponItem coupon
                    && CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())
                    && isActiveTimedCoupon(stack, coupon)) {
                activeTimed += stack.getCount();
            }
        }

        return new CouponInventoryStats(
                carried,
                uninitialized,
                activeTimed,
                Map.copyOf(categoryCounts),
                Map.copyOf(modeCounts)
        );
    }

    public static CarriedCoupon findBestCarriedCoupon(Player player, CouponEffectType effect) {
        CarriedCoupon bestCoupon = activeTimedCoupon(player, effect);
        int bestDiscount = bestCoupon == null ? -1 : discountPercent(bestCoupon.stack(), bestCoupon.coupon(), player.level());

        for (CarriedCoupon carriedCoupon : carriedCoupons(player)) {
            int discount = matches(carriedCoupon.stack(), carriedCoupon.coupon(), effect) ? discountPercent(carriedCoupon.stack(), carriedCoupon.coupon(), player.level()) : -1;
            if (discount > bestDiscount) {
                bestDiscount = discount;
                bestCoupon = carriedCoupon;
            }
        }

        return bestCoupon;
    }

    public static ItemStack findBestActiveTimedCoupon(Player player) {
        return findBestActiveTimedCoupon(player, (CouponEffectType) null);
    }

    public static ItemStack findBestActiveTimedCoupon(Player player, CouponEffectType effect) {
        ItemStack bestCoupon = ItemStack.EMPTY;
        int bestDiscount = -1;

        for (ItemStack stack : activeTimedCouponsOrEmpty(player)) {
            if (stack.getItem() instanceof CouponItem coupon
                    && CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())
                    && CouponData.isActiveTimedCoupon(stack, coupon)) {
                if (effect != null && coupon.effect() != effect) {
                    continue;
                }

                int discount = CouponData.discountPercent(stack, coupon, player.level());
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestCoupon = stack;
                }
            }
        }

        return bestCoupon;
    }

    public static ItemStack findBestActiveTimedCoupon(Player player, CouponCategory category) {
        ItemStack bestCoupon = ItemStack.EMPTY;
        int bestDiscount = -1;

        for (ItemStack stack : activeTimedCouponsOrEmpty(player)) {
            if (stack.getItem() instanceof CouponItem coupon
                    && CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())
                    && CouponData.isActiveTimedCoupon(stack, coupon)
                    && coupon.effect().category() == category) {
                int discount = CouponData.discountPercent(stack, coupon, player.level());
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestCoupon = stack;
                }
            }
        }

        return bestCoupon;
    }

    public static int clearActiveTimedCoupons(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return ActiveTimedCouponSavedData.get(serverLevel).clear(player.getUUID());
        }

        List<ItemStack> activeCoupons = ACTIVE_TIMED_COUPONS.remove(player.getUUID());
        return activeCoupons == null ? 0 : activeCoupons.size();
    }

    public static int clearActiveTimedCoupons(Player player, CouponEffectType effect) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return ActiveTimedCouponSavedData.get(serverLevel).clear(player.getUUID(), effect);
        }

        List<ItemStack> activeCoupons = ACTIVE_TIMED_COUPONS.get(player.getUUID());
        if (activeCoupons == null) {
            return 0;
        }

        int cleared = 0;
        Iterator<ItemStack> iterator = activeCoupons.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (stack.getItem() instanceof CouponItem coupon && coupon.effect() == effect) {
                iterator.remove();
                cleared++;
            }
        }

        if (activeCoupons.isEmpty()) {
            ACTIVE_TIMED_COUPONS.remove(player.getUUID());
        }
        return cleared;
    }

    public static int clearActiveTimedCoupons(Player player, CouponCategory category) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return ActiveTimedCouponSavedData.get(serverLevel).clear(player.getUUID(), category);
        }

        List<ItemStack> activeCoupons = ACTIVE_TIMED_COUPONS.get(player.getUUID());
        if (activeCoupons == null) {
            return 0;
        }

        int cleared = 0;
        Iterator<ItemStack> iterator = activeCoupons.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (stack.getItem() instanceof CouponItem coupon && coupon.effect().category() == category) {
                iterator.remove();
                cleared++;
            }
        }

        if (activeCoupons.isEmpty()) {
            ACTIVE_TIMED_COUPONS.remove(player.getUUID());
        }
        return cleared;
    }

    private static void tickActiveTimedCoupons(Player player) {
        List<ItemStack> activeCoupons = activeTimedCouponsOrEmpty(player);
        if (activeCoupons.isEmpty()) {
            return;
        }

        boolean ticked = false;
        for (ItemStack stack : activeCoupons) {
            if (stack.getItem() instanceof CouponItem coupon && CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())) {
                tick(stack, coupon, player.level());
                ticked = true;
            }
        }
        if (ticked) {
            markActiveTimedCouponsDirty(player);
        }

        removeExpiredActiveTimedCoupons(player);
    }

    private static void removeExpiredActiveTimedCoupons(Player player) {
        List<ItemStack> activeCoupons = activeTimedCouponsOrEmpty(player);
        if (activeCoupons.isEmpty()) {
            return;
        }

        boolean changed = false;
        Iterator<ItemStack> iterator = activeCoupons.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (stack.isEmpty()
                    || !(stack.getItem() instanceof CouponItem coupon)
                    || !CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())
                    || isExpired(stack, coupon)) {
                iterator.remove();
                changed = true;
            }
        }

        if (activeCoupons.isEmpty()) {
            removeActiveTimedCoupons(player);
            return;
        }

        while (activeCoupons.size() > CouponConfig.maxActiveTimedCoupons()) {
            activeCoupons.remove(activeCoupons.size() - 1);
            changed = true;
        }

        if (changed) {
            markActiveTimedCouponsDirty(player);
        }
    }

    private static boolean sameTimedCoupon(ItemStack stack, CouponItem coupon, ItemStack activeCoupon, CouponItem activeCouponItem) {
        return coupon.mode() == CouponMode.TIMED
                && activeCouponItem.mode() == CouponMode.TIMED
                && coupon.effect() == activeCouponItem.effect()
                && discountPercent(stack) == discountPercent(activeCoupon);
    }

    private static void addTimedCouponDuration(ItemStack activeCoupon, ItemStack addedCoupon) {
        CompoundTag activeTag = tag(activeCoupon);
        CompoundTag addedTag = tag(addedCoupon);
        int addedTicks = Math.max(0, addedTag.getInt(TICKS_KEY));

        activeTag.putInt(TICKS_KEY, activeTag.getInt(TICKS_KEY) + addedTicks);
        activeTag.putInt(INITIAL_TICKS_KEY, activeTag.getInt(INITIAL_TICKS_KEY) + addedTicks);
        save(activeCoupon, activeTag);
    }

    private static List<ItemStack> activeTimedCoupons(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return ActiveTimedCouponSavedData.get(serverLevel).activeCoupons(player.getUUID());
        }
        return ACTIVE_TIMED_COUPONS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
    }

    private static List<ItemStack> activeTimedCouponsOrEmpty(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return ActiveTimedCouponSavedData.get(serverLevel).activeCouponsOrEmpty(player.getUUID());
        }
        return ACTIVE_TIMED_COUPONS.getOrDefault(player.getUUID(), List.of());
    }

    private static void removeActiveTimedCoupons(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            ActiveTimedCouponSavedData.get(serverLevel).clear(player.getUUID());
        } else {
            ACTIVE_TIMED_COUPONS.remove(player.getUUID());
        }
    }

    private static void markActiveTimedCouponsDirty(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            ActiveTimedCouponSavedData.get(serverLevel).setDirty();
        }
    }

    public static boolean matches(ItemStack stack, CouponItem coupon, CouponEffectType effect) {
        if (!CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode()) || coupon.effect() != effect || isExpired(stack, coupon)) {
            return false;
        }

        return coupon.mode() != CouponMode.TIMED || isTimedActive(stack);
    }

    public static boolean isBestCarriedCoupon(ItemStack stack) {
        return tag(stack).getBoolean(BEST_CARRIED_KEY);
    }

    public static int discountPercent(ItemStack stack) {
        return Mth.clamp(tag(stack).getInt(DISCOUNT_KEY), 0, 95);
    }

    public static int discountPercent(ItemStack stack, CouponItem coupon, Level level) {
        return CouponDailyBoost.effectiveDiscountPercent(discountPercent(stack), level, coupon.effect(), coupon.mode());
    }

    public static void consumeUse(ItemStack stack, CouponItem coupon) {
        consumeUse(stack, coupon, null);
    }

    public static void consumeUse(ItemStack stack, CouponItem coupon, Level level) {
        if (coupon.mode() == CouponMode.TIMED) {
            return;
        }

        CompoundTag tag = tag(stack);
        if (CouponDailyBoost.isBoosted(level, coupon.effect(), coupon.mode()) && spendDailyBoostFreeUse(tag, level)) {
            save(stack, tag);
            return;
        }

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

    public static void appendHoverText(ItemStack stack, CouponItem coupon, List<Component> tooltip, TooltipFlag flag) {
        appendHoverText(stack, coupon, null, tooltip, flag);
    }

    public static void appendHoverText(ItemStack stack, CouponItem coupon, Level level, List<Component> tooltip, TooltipFlag flag) {
        Rarity rarity = ModItems.couponRarity(coupon.effect(), coupon.mode());
        tooltip.add(Component.translatable("item.coupon_codes.coupon.rarity." + rarity.name().toLowerCase(Locale.ROOT))
                .withStyle(rarity.getStyleModifier()));
        tooltip.add(Component.translatable(
                        "item.coupon_codes.coupon.category",
                        Component.translatable("item.coupon_codes.coupon.category." + coupon.effect().category().id())
                )
                .withStyle(ChatFormatting.DARK_GRAY));

        if (!CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())) {
            tooltip.add(Component.translatable("item.coupon_codes.coupon.disabled").withStyle(ChatFormatting.RED));
            return;
        }

        if (!tag(stack).getBoolean(INITIALIZED_KEY)) {
            tooltip.add(Component.translatable("item.coupon_codes.coupon.unrolled").withStyle(ChatFormatting.GRAY));
            if (flag.isAdvanced()) {
                tooltip.add(Component.translatable("item.coupon_codes.coupon.mode." + coupon.mode().id() + ".pending")
                        .withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("item.coupon_codes.coupon.scope." + coupon.effect().id())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }

        int discount = discountPercent(stack, coupon, level);
        tooltip.add(Component.translatable("item.coupon_codes.coupon.effect." + coupon.effect().id(), discount)
                .withStyle(ChatFormatting.GOLD));
        if (CouponDailyBoost.isBoosted(level, coupon.effect(), coupon.mode())) {
            tooltip.add(Component.translatable("item.coupon_codes.coupon.daily_boost")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (coupon.mode() == CouponMode.TIMED) {
            String stateKey = isTimedActive(stack) ? "active" : "inactive";
            tooltip.add(Component.translatable("item.coupon_codes.coupon.mode.timed." + stateKey, secondsRemaining(stack, coupon, level))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            int usesRemaining = usesRemaining(stack, coupon, level);
            String key = coupon.mode() == CouponMode.SINGLE_USE && usesRemaining == 1
                    ? "item.coupon_codes.coupon.mode.single_use.uses"
                    : "item.coupon_codes.coupon.mode.uses.uses";
            tooltip.add(Component.translatable(key, usesRemaining)
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!isExpired(stack, coupon) && (coupon.mode() != CouponMode.TIMED || isTimedActive(stack))) {
            String bestKey = isBestCarriedCoupon(stack) ? "best" : "outclassed";
            tooltip.add(Component.translatable("item.coupon_codes.coupon.best." + bestKey)
                    .withStyle(isBestCarriedCoupon(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable("item.coupon_codes.coupon.scope." + coupon.effect().id())
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.coupon_codes.coupon.note." + coupon.effect().id())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static int secondsRemaining(ItemStack stack) {
        return Math.max(0, tag(stack).getInt(TICKS_KEY) / 20);
    }

    public static int secondsRemaining(ItemStack stack, CouponItem coupon, Level level) {
        int ticksRemaining = Math.max(0, tag(stack).getInt(TICKS_KEY));
        if (CouponDailyBoost.isBoosted(level, coupon.effect(), coupon.mode())) {
            int durationMultiplier = CouponConfig.dailyBoostDurationMultiplier();
            return Math.max(0, ticksRemaining * durationMultiplier / 20);
        }
        return ticksRemaining / 20;
    }

    public static int usesRemaining(ItemStack stack, CouponItem coupon, Level level) {
        int usesRemaining = Math.max(0, tag(stack).getInt(USES_KEY));
        if (!CouponDailyBoost.isBoosted(level, coupon.effect(), coupon.mode())) {
            return usesRemaining;
        }

        int useMultiplier = CouponConfig.dailyBoostUseMultiplier();
        return usesRemaining * useMultiplier - spentBoostedUsesForCurrentCharge(stack, level, useMultiplier);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    private static boolean spendDailyBoostFreeUse(CompoundTag tag, Level level) {
        int useMultiplier = CouponConfig.dailyBoostUseMultiplier();
        if (useMultiplier <= 1) {
            return false;
        }

        long day = CouponDailyBoost.minecraftDay(level);
        if (!tag.contains(BOOST_SPARE_DAY_KEY)
                || tag.getLong(BOOST_SPARE_DAY_KEY) != day
                || tag.getInt(BOOST_USE_MULTIPLIER_KEY) != useMultiplier) {
            tag.putLong(BOOST_SPARE_DAY_KEY, day);
            tag.putInt(BOOST_USE_MULTIPLIER_KEY, useMultiplier);
            tag.putInt(BOOST_FREE_USES_KEY, useMultiplier - 1);
            tag.remove(BOOST_SPARE_READY_KEY);
        }

        if (!tag.contains(BOOST_FREE_USES_KEY) && tag.contains(BOOST_SPARE_READY_KEY)) {
            tag.putInt(BOOST_FREE_USES_KEY, tag.getBoolean(BOOST_SPARE_READY_KEY) ? 1 : 0);
            tag.remove(BOOST_SPARE_READY_KEY);
        }

        int freeUses = Mth.clamp(tag.getInt(BOOST_FREE_USES_KEY), 0, useMultiplier - 1);
        if (freeUses <= 0) {
            tag.putInt(BOOST_FREE_USES_KEY, useMultiplier - 1);
            return false;
        }

        tag.putInt(BOOST_FREE_USES_KEY, freeUses - 1);
        return true;
    }

    private static int spentBoostedUsesForCurrentCharge(ItemStack stack, Level level, int useMultiplier) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(BOOST_SPARE_DAY_KEY)
                || tag.getLong(BOOST_SPARE_DAY_KEY) != CouponDailyBoost.minecraftDay(level)
                || tag.getInt(BOOST_USE_MULTIPLIER_KEY) != useMultiplier) {
            return 0;
        }

        int freeUses = tag.contains(BOOST_FREE_USES_KEY)
                ? tag.getInt(BOOST_FREE_USES_KEY)
                : tag.getBoolean(BOOST_SPARE_READY_KEY) ? 1 : 0;
        return useMultiplier - 1 - Mth.clamp(freeUses, 0, useMultiplier - 1);
    }

    private static CarriedCoupon activeTimedCoupon(Player player, CouponEffectType effect) {
        ItemStack stack = findBestActiveTimedCoupon(player, effect);
        if (stack.isEmpty() || !(stack.getItem() instanceof CouponItem coupon)) {
            return null;
        }
        return new CarriedCoupon(stack, coupon, () -> {
        });
    }

    private static List<CarriedCoupon> carriedCoupons(Player player) {
        List<CarriedCoupon> coupons = new ArrayList<>();
        CouponPouchMenu openPouch = player.containerMenu instanceof CouponPouchMenu pouchMenu ? pouchMenu : null;
        collectCarriedCoupons(player.getInventory().items, () -> {
        }, coupons, 0, openPouch);
        collectCarriedCoupons(player.getInventory().offhand, () -> {
        }, coupons, 0, openPouch);
        CuriosCompat.collectCarriedCoupons(player, coupons, openPouch);
        return coupons;
    }

    private static void tickCouponsInStacks(Player player, Iterable<ItemStack> stacks, RandomSource random, int depth) {
        for (ItemStack stack : stacks) {
            tickCouponsInStack(player, stack, random, depth);
        }
    }

    private static void tickCouponsInStack(Player player, ItemStack stack, RandomSource random, int depth) {
        if (stack.getItem() instanceof CouponItem coupon && CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())) {
            boolean wasInitialized = isInitialized(stack);
            initializeIfNeeded(stack, coupon, random);
            if (!wasInitialized && isInitialized(stack) && player instanceof ServerPlayer serverPlayer) {
                CouponCriteria.triggerObtained(serverPlayer, coupon.effect(), coupon.mode());
            }
        } else {
            tickCouponsInContainer(player, stack, random, depth);
        }
    }

    public static void collectCarriedCouponsFromStack(Player player, ItemStack stack, Runnable save, List<CarriedCoupon> coupons, CouponPouchMenu openPouch) {
        collectCarriedCoupon(stack, save, coupons, 0, openPouch);
    }

    private static void tickCouponsInContainer(Player player, ItemStack containerStack, RandomSource random, int depth) {
        if (!isCouponContainer(containerStack) || depth >= CouponConfig.containerSearchDepth()) {
            return;
        }

        ItemContainerContents contents = containerStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents == ItemContainerContents.EMPTY) {
            return;
        }

        NonNullList<ItemStack> stacks = NonNullList.withSize(contents.getSlots(), ItemStack.EMPTY);
        contents.copyInto(stacks);
        tickCouponsInStacks(player, stacks, random, depth + 1);
        containerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
    }

    private static void collectCarriedCoupons(Iterable<ItemStack> stacks, Runnable save, List<CarriedCoupon> coupons, int depth, CouponPouchMenu openPouch) {
        for (ItemStack stack : stacks) {
            collectCarriedCoupon(stack, save, coupons, depth, openPouch);
        }
    }

    private static void collectCarriedCoupon(ItemStack stack, Runnable save, List<CarriedCoupon> coupons, int depth, CouponPouchMenu openPouch) {
        if (openPouch != null
                && openPouch.isOpenFor(stack)
                && CouponConfig.allowCouponsInPouches()
                && openPouch.isAutoActivationEnabled()
                && depth < CouponConfig.containerSearchDepth()) {
            collectCarriedCoupons(openPouch.pouchItems(), openPouch::saveContents, coupons, depth + 1, null);
        } else if (stack.getItem() instanceof CouponItem coupon && CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())) {
            coupons.add(new CarriedCoupon(stack, coupon, save));
        } else {
            collectContainerCoupons(stack, save, coupons, depth, openPouch);
        }
    }

    private static void updateBestCarriedMarkers(Player player) {
        List<CarriedCoupon> coupons = carriedCoupons(player);
        EnumMap<CouponEffectType, CarriedCoupon> bestByEffect = new EnumMap<>(CouponEffectType.class);
        EnumMap<CouponEffectType, Integer> bestDiscountByEffect = new EnumMap<>(CouponEffectType.class);

        for (CarriedCoupon carriedCoupon : coupons) {
            CouponItem coupon = carriedCoupon.coupon();
            if (!matches(carriedCoupon.stack(), coupon, coupon.effect())) {
                continue;
            }

            int discount = discountPercent(carriedCoupon.stack(), coupon, player.level());
            int bestDiscount = bestDiscountByEffect.getOrDefault(coupon.effect(), -1);
            if (discount > bestDiscount) {
                bestDiscountByEffect.put(coupon.effect(), discount);
                bestByEffect.put(coupon.effect(), carriedCoupon);
            }
        }

        for (CarriedCoupon carriedCoupon : coupons) {
            boolean isBest = bestByEffect.get(carriedCoupon.coupon().effect()) == carriedCoupon;
            setBestCarriedMarker(carriedCoupon.stack(), isBest);
            carriedCoupon.save().run();
        }
    }

    private static void setBestCarriedMarker(ItemStack stack, boolean best) {
        CompoundTag tag = tag(stack);
        if (best) {
            if (tag.getBoolean(BEST_CARRIED_KEY)) {
                return;
            }
            tag.putBoolean(BEST_CARRIED_KEY, true);
        } else {
            if (!tag.contains(BEST_CARRIED_KEY)) {
                return;
            }
            tag.remove(BEST_CARRIED_KEY);
        }
        save(stack, tag);
    }

    private static void collectContainerCoupons(ItemStack containerStack, Runnable parentSave, List<CarriedCoupon> coupons, int depth, CouponPouchMenu openPouch) {
        if (!isCouponContainer(containerStack) || depth >= CouponConfig.containerSearchDepth()) {
            return;
        }

        ItemContainerContents contents = containerStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents == ItemContainerContents.EMPTY) {
            return;
        }

        NonNullList<ItemStack> stacks = NonNullList.withSize(contents.getSlots(), ItemStack.EMPTY);
        contents.copyInto(stacks);
        Runnable save = () -> {
            containerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
            parentSave.run();
        };
        collectCarriedCoupons(stacks, save, coupons, depth + 1, openPouch);
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static boolean isCouponContainer(ItemStack stack) {
        if (stack.getItem() instanceof CouponPouchItem) {
            return CouponConfig.allowCouponsInPouches() && CouponPouchItem.isAutoActivationEnabled(stack);
        }
        return CouponConfig.allowCouponsInShulkerBoxes() && isShulkerBox(stack);
    }

}
