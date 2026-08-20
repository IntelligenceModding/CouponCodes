package de.doomedartemis.couponcodes.common.coupon;

import de.doomedartemis.couponcodes.common.config.CouponConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CouponDailyBoost {
    private static final long TICKS_PER_DAY = 24000L;
    private static final Map<Level, Long> ANNOUNCED_DAYS = new HashMap<>();

    private CouponDailyBoost() {
    }

    public static CouponEffectType boostedEffect(Level level) {
        Boost boost = boost(level);
        return boost == null || boost.isCategory() ? null : boost.effect();
    }

    public static Boost boost(Level level) {
        if (level == null || !CouponConfig.areDailyBoostsEnabled()) {
            return null;
        }

        List<CouponEffectType> availableEffects = availableEffects();
        if (availableEffects.isEmpty()) {
            return null;
        }

        List<CouponCategory> availableCategories = availableCategories(availableEffects);
        boolean boostCategory = !availableCategories.isEmpty()
                && Math.floorMod(daySeed(level, 0), 100) < CouponConfig.dailyBoostCategoryChance();
        if (boostCategory) {
            int categoryIndex = Math.floorMod(daySeed(level, 1), availableCategories.size());
            return Boost.category(availableCategories.get(categoryIndex));
        }

        int effectIndex = Math.floorMod(daySeed(level, 2), availableEffects.size());
        return Boost.effect(availableEffects.get(effectIndex));
    }

    public static boolean isBoosted(Level level, CouponEffectType effect, CouponMode mode) {
        if (!CouponConfig.isCouponEnabled(effect, mode)) {
            return false;
        }

        Boost boost = boost(level);
        return boost != null && boost.matches(effect);
    }

    public static int effectiveDiscountPercent(int discountPercent, Level level, CouponEffectType effect, CouponMode mode) {
        int multiplier = isBoosted(level, effect, mode) ? CouponConfig.dailyBoostStrengthMultiplier() : 1;
        return Mth.clamp(discountPercent * multiplier, 0, 100);
    }

    public static long minecraftDay(Level level) {
        return Math.floorDiv(level.getDayTime(), TICKS_PER_DAY);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!CouponConfig.announceDailyBoosts()) {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level.players().isEmpty() || !level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            return;
        }

        long day = minecraftDay(level);
        if (ANNOUNCED_DAYS.getOrDefault(level, Long.MIN_VALUE) == day) {
            return;
        }

        long dayTime = Math.floorMod(level.getDayTime(), TICKS_PER_DAY);
        if (dayTime > 200L) {
            return;
        }

        Boost boost = boost(level);
        if (boost == null) {
            return;
        }

        ANNOUNCED_DAYS.put(level, day);
        server.getPlayerList().broadcastSystemMessage(announcement(boost), false);
    }

    private static List<CouponEffectType> availableEffects() {
        List<CouponEffectType> effects = new ArrayList<>();
        for (CouponEffectType effect : CouponEffectType.values()) {
            if (isEffectAvailable(effect)) {
                effects.add(effect);
            }
        }
        return effects;
    }

    private static boolean isEffectAvailable(CouponEffectType effect) {
        for (CouponMode mode : CouponMode.values()) {
            if (CouponConfig.isCouponEnabled(effect, mode)) {
                return true;
            }
        }
        return false;
    }

    private static List<CouponCategory> availableCategories(List<CouponEffectType> availableEffects) {
        List<CouponCategory> categories = new ArrayList<>();
        for (CouponEffectType effect : availableEffects) {
            if (!categories.contains(effect.category())) {
                categories.add(effect.category());
            }
        }
        return categories;
    }

    private static int daySeed(Level level, int salt) {
        long value = minecraftDay(level) * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (int) value;
    }

    private static Component announcement(Boost boost) {
        String message = CouponConfig.dailyBoostAnnouncementMessage()
                .replace("{effect}", boost.displayName())
                .replace("{boost}", boost.displayName())
                .replace("{boost_type}", boost.typeName())
                .replace("{category}", boost.categoryName())
                .replace("{strength}", Integer.toString(CouponConfig.dailyBoostStrengthMultiplier()))
                .replace("{uses}", Integer.toString(CouponConfig.dailyBoostUseMultiplier()))
                .replace("{duration}", Integer.toString(CouponConfig.dailyBoostDurationMultiplier()));
        return formattedLiteral(message);
    }

    public record Boost(CouponEffectType effect, CouponCategory category) {
        public static Boost effect(CouponEffectType effect) {
            return new Boost(effect, null);
        }

        public static Boost category(CouponCategory category) {
            return new Boost(null, category);
        }

        public boolean isCategory() {
            return category != null;
        }

        public boolean matches(CouponEffectType effect) {
            return isCategory() ? effect.category() == category : effect == this.effect;
        }

        public String displayName() {
            return isCategory() ? category.displayName() + " category" : effect.displayName();
        }

        public String typeName() {
            return isCategory() ? "category" : "coupon type";
        }

        public String categoryName() {
            return isCategory() ? category.displayName() : effect.category().displayName();
        }
    }

    private static Component formattedLiteral(String message) {
        MutableComponent result = Component.empty();
        List<ChatFormatting> activeFormats = new ArrayList<>();
        activeFormats.add(ChatFormatting.WHITE);
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            char current = message.charAt(i);
            if (current == '&' && i + 1 < message.length()) {
                ChatFormatting nextFormat = ChatFormatting.getByCode(message.charAt(i + 1));
                if (nextFormat != null) {
                    appendSegment(result, segment, activeFormats);
                    applyFormat(activeFormats, nextFormat);
                    i++;
                    continue;
                }
            }
            segment.append(current);
        }

        appendSegment(result, segment, activeFormats);
        return result;
    }

    private static void applyFormat(List<ChatFormatting> activeFormats, ChatFormatting format) {
        if (format == ChatFormatting.RESET) {
            activeFormats.clear();
            activeFormats.add(ChatFormatting.WHITE);
            return;
        }

        if (format.isColor()) {
            activeFormats.removeIf(ChatFormatting::isColor);
        }
        if (!activeFormats.contains(format)) {
            activeFormats.add(format);
        }
    }

    private static void appendSegment(MutableComponent result, StringBuilder segment, List<ChatFormatting> formats) {
        if (segment.isEmpty()) {
            return;
        }

        result.append(Component.literal(segment.toString()).withStyle(formats.toArray(ChatFormatting[]::new)));
        segment.setLength(0);
    }
}
