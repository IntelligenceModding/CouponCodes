package de.doomedartemis.couponcodes.common.config;

import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CouponConfig {
    public static final ModConfigSpec SPEC;
    private static final String DEFAULT_DAILY_BOOST_ANNOUNCEMENT = "&6[Coupons]&r &eDaily boost: &b{boost} &7(strength x{strength}, uses x{uses}, duration x{duration})";

    private static final ModConfigSpec.BooleanValue ENABLE_COUPONS;
    private static final ModConfigSpec.BooleanValue ENABLE_EMPTY_COUPON_ROLLS;
    private static final ModConfigSpec.BooleanValue ENABLE_COUPON_POUCHES;
    private static final ModConfigSpec.BooleanValue ENABLE_COMMANDS;
    private static final ModConfigSpec.BooleanValue SHOW_TIMED_COUPON_BOSS_BAR;
    private static final ModConfigSpec.BooleanValue ENABLE_DAILY_BOOSTS;
    private static final ModConfigSpec.BooleanValue ANNOUNCE_DAILY_BOOSTS;
    private static final ModConfigSpec.ConfigValue<String> DAILY_BOOST_ANNOUNCEMENT_MESSAGE;
    private static final ModConfigSpec.IntValue DAILY_BOOST_CATEGORY_CHANCE;
    private static final ModConfigSpec.IntValue DAILY_BOOST_STRENGTH_MULTIPLIER;
    private static final ModConfigSpec.IntValue DAILY_BOOST_USE_MULTIPLIER;
    private static final ModConfigSpec.IntValue DAILY_BOOST_DURATION_MULTIPLIER;
    private static final ModConfigSpec.BooleanValue ALLOW_COUPONS_IN_POUCHES;
    private static final ModConfigSpec.BooleanValue ALLOW_COUPONS_IN_SHULKER_BOXES;
    private static final ModConfigSpec.IntValue CONTAINER_SEARCH_DEPTH;
    private static final ModConfigSpec.BooleanValue ALLOW_TIMED_COUPON_INVENTORY_ACTIVATION;
    private static final ModConfigSpec.BooleanValue ALLOW_TIMED_COUPON_POUCH_ACTIVATION;
    private static final ModConfigSpec.BooleanValue ALLOW_TIMED_COUPON_DURATION_STACKING;
    private static final ModConfigSpec.IntValue MAX_ACTIVE_TIMED_COUPONS;
    private static final ModConfigSpec.BooleanValue PLAY_ACTIVATION_FEEDBACK;
    private static final ModConfigSpec.BooleanValue PLAY_USE_FEEDBACK;
    private static final ModConfigSpec.IntValue USE_FEEDBACK_COOLDOWN_TICKS;
    private static final ModConfigSpec.IntValue ACTIVATION_PARTICLE_COUNT;
    private static final ModConfigSpec.IntValue USE_PARTICLE_COUNT;
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISCOUNT_VALUES;
    private static final ModConfigSpec.IntValue MULTI_USE_MIN_USES;
    private static final ModConfigSpec.IntValue MULTI_USE_DEFAULT_USES;
    private static final ModConfigSpec.IntValue MULTI_USE_MAX_USES;
    private static final ModConfigSpec.IntValue TIMED_MIN_SECONDS;
    private static final ModConfigSpec.IntValue TIMED_DEFAULT_SECONDS;
    private static final ModConfigSpec.IntValue TIMED_MAX_SECONDS;
    private static final ModConfigSpec.IntValue ANVIL_MINIMUM_EXPERIENCE_COST;
    private static final ModConfigSpec.IntValue ANVIL_MINIMUM_MATERIAL_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_PERCENT_PER_REFUND_LEVEL;
    private static final ModConfigSpec.IntValue POTION_DURATION_EXTENSION_TICKS;
    private static final ModConfigSpec.BooleanValue CONSUME_CHANCE_COUPONS_ON_FAILED_ROLL;
    private static final ModConfigSpec.BooleanValue CONSUME_DURABILITY_COUPONS_ON_FAILED_ROLL;
    private static final Map<Rarity, ModConfigSpec.IntValue> ROLL_WEIGHTS = new EnumMap<>(Rarity.class);
    private static final Map<CouponEffectType, ModConfigSpec.BooleanValue> ENABLED_EFFECTS = new EnumMap<>(CouponEffectType.class);
    private static final Map<CouponMode, ModConfigSpec.BooleanValue> ENABLED_MODES = new EnumMap<>(CouponMode.class);
    private static final Map<CouponEffectType, Map<CouponMode, ModConfigSpec.BooleanValue>> ENABLED_COUPONS = new EnumMap<>(CouponEffectType.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLE_COUPONS = builder
                .comment("Master switch for all coupon effects. Existing coupon items remain, but do nothing when this is false.")
                .translation(serverConfigKey("general", "enable_coupons"))
                .define("enableCoupons", true);
        ENABLE_EMPTY_COUPON_ROLLS = builder
                .comment("Allows empty coupons to turn into configured random coupons.")
                .translation(serverConfigKey("general", "enable_empty_coupon_rolls"))
                .define("enableEmptyCouponRolls", true);
        ENABLE_COUPON_POUCHES = builder
                .comment("Allows coupon pouches to open and apply contained coupons.")
                .translation(serverConfigKey("general", "enable_coupon_pouches"))
                .define("enableCouponPouches", true);
        ENABLE_COMMANDS = builder
                .comment("Allows coupon_codes admin commands to run.")
                .translation(serverConfigKey("general", "enable_commands"))
                .define("enableCommands", true);
        SHOW_TIMED_COUPON_BOSS_BAR = builder
                .comment("Shows a boss bar while a timed coupon is active.")
                .translation(serverConfigKey("general", "show_timed_coupon_boss_bar"))
                .define("showTimedCouponBossBar", true);
        DISCOUNT_VALUES = builder
                .comment("Possible discount percentages assigned when a coupon is initialized.")
                .translation(serverConfigKey("general", "discount_values"))
                .defineList("discountValues", List.of(10, 20, 25, 50), CouponConfig::isValidDiscount);
        builder.pop();

        builder.push("dailyBoost");
        ENABLE_DAILY_BOOSTS = builder.comment("Enables the daily boosted coupon type system.")
                .translation(serverConfigKey("daily_boost", "enable"))
                .define("enableDailyBoosts", true);
        ANNOUNCE_DAILY_BOOSTS = builder.comment("Announces each new daily boosted coupon type in chat.")
                .translation(serverConfigKey("daily_boost", "announce"))
                .define("announceDailyBoosts", true);
        DAILY_BOOST_ANNOUNCEMENT_MESSAGE = builder.comment(
                        "Chat message sent when a new daily boost starts.",
                        "Placeholders: {effect}, {boost}, {boost_type}, {category}, {strength}, {uses}, {duration}.",
                        "Supports Minecraft-style formatting codes with &: &0-&9, &a-&f, &k-&o, &r."
                )
                .translation(serverConfigKey("daily_boost", "announcement_message"))
                .define("announcementMessage", DEFAULT_DAILY_BOOST_ANNOUNCEMENT);
        DAILY_BOOST_CATEGORY_CHANCE = builder.comment("Percent chance for the daily boost to affect a whole coupon category instead of one coupon type.")
                .translation(serverConfigKey("daily_boost", "category_chance"))
                .defineInRange("categoryBoostChance", 10, 0, 100);
        DAILY_BOOST_STRENGTH_MULTIPLIER = builder.comment("Multiplier applied to today's boosted coupon strength.")
                .translation(serverConfigKey("daily_boost", "strength_multiplier"))
                .defineInRange("strengthMultiplier", 2, 1, 16);
        DAILY_BOOST_USE_MULTIPLIER = builder.comment("Multiplier applied to today's boosted reusable and single-use coupon uses.")
                .translation(serverConfigKey("daily_boost", "use_multiplier"))
                .defineInRange("useMultiplier", 2, 1, 16);
        DAILY_BOOST_DURATION_MULTIPLIER = builder.comment("Multiplier applied to today's boosted timed coupon duration.")
                .translation(serverConfigKey("daily_boost", "duration_multiplier"))
                .defineInRange("durationMultiplier", 2, 1, 16);
        builder.pop();

        builder.push("containers");
        ALLOW_COUPONS_IN_POUCHES = builder.comment("Allows coupons stored in coupon pouches to be found and consumed.")
                .translation(serverConfigKey("containers", "allow_coupons_in_pouches"))
                .define("allowCouponsInPouches", true);
        ALLOW_COUPONS_IN_SHULKER_BOXES = builder.comment("Allows coupons stored in shulker boxes to be found and consumed.")
                .translation(serverConfigKey("containers", "allow_coupons_in_shulker_boxes"))
                .define("allowCouponsInShulkerBoxes", false);
        CONTAINER_SEARCH_DEPTH = builder.comment("Maximum nested container depth checked for carried coupons. 0 means only inventory and offhand.")
                .translation(serverConfigKey("containers", "container_search_depth"))
                .defineInRange("containerSearchDepth", 1, 0, 3);
        builder.pop();

        builder.push("timed");
        ALLOW_TIMED_COUPON_INVENTORY_ACTIVATION = builder.comment("Allows right-clicking timed coupons in hand to activate them.")
                .translation(serverConfigKey("timed", "allow_inventory_activation"))
                .define("allowInventoryActivation", true);
        ALLOW_TIMED_COUPON_POUCH_ACTIVATION = builder.comment("Allows right-clicking timed coupons inside a coupon pouch to activate them.")
                .translation(serverConfigKey("timed", "allow_pouch_activation"))
                .define("allowPouchActivation", true);
        ALLOW_TIMED_COUPON_DURATION_STACKING = builder.comment("Allows activating a matching timed coupon to add its duration to an existing active timed coupon.")
                .translation(serverConfigKey("timed", "allow_duration_stacking"))
                .define("allowDurationStacking", true);
        MAX_ACTIVE_TIMED_COUPONS = builder.comment("Maximum active timed coupons per player. Matching coupons may still stack when duration stacking is enabled.")
                .translation(serverConfigKey("timed", "max_active_coupons"))
                .defineInRange("maxActiveCoupons", 1, 1, 16);
        builder.pop();

        builder.push("feedback");
        PLAY_ACTIVATION_FEEDBACK = builder.comment("Plays sounds and particles when a coupon is activated or rolled.")
                .translation(serverConfigKey("feedback", "play_activation_feedback"))
                .define("playActivationFeedback", true);
        PLAY_USE_FEEDBACK = builder.comment("Plays sounds and particles when a coupon effect is consumed.")
                .translation(serverConfigKey("feedback", "play_use_feedback"))
                .define("playUseFeedback", true);
        USE_FEEDBACK_COOLDOWN_TICKS = builder.comment("Minimum ticks between repeated use feedback for a player.")
                .translation(serverConfigKey("feedback", "use_feedback_cooldown_ticks"))
                .defineInRange("useFeedbackCooldownTicks", 20, 0, 200);
        ACTIVATION_PARTICLE_COUNT = builder.comment("Particle count for coupon activation feedback.")
                .translation(serverConfigKey("feedback", "activation_particle_count"))
                .defineInRange("activationParticleCount", 30, 0, 200);
        USE_PARTICLE_COUNT = builder.comment("Particle count for coupon use feedback.")
                .translation(serverConfigKey("feedback", "use_particle_count"))
                .defineInRange("useParticleCount", 12, 0, 200);
        builder.pop();

        builder.push("values");
        MULTI_USE_MIN_USES = builder.comment("Minimum random use count for reusable coupons.")
                .translation(serverConfigKey("values", "multi_use_min_uses"))
                .defineInRange("multiUseMinUses", 2, 1, 64);
        MULTI_USE_DEFAULT_USES = builder.comment("Default use count when commands omit a reusable coupon use count.")
                .translation(serverConfigKey("values", "multi_use_default_uses"))
                .defineInRange("multiUseDefaultUses", 3, 1, 64);
        MULTI_USE_MAX_USES = builder.comment("Maximum random use count for reusable coupons.")
                .translation(serverConfigKey("values", "multi_use_max_uses"))
                .defineInRange("multiUseMaxUses", 5, 1, 64);
        TIMED_MIN_SECONDS = builder.comment("Minimum random duration for timed coupons.")
                .translation(serverConfigKey("values", "timed_min_seconds"))
                .defineInRange("timedMinSeconds", 15, 1, 3600);
        TIMED_DEFAULT_SECONDS = builder.comment("Default duration when commands omit a timed coupon duration.")
                .translation(serverConfigKey("values", "timed_default_seconds"))
                .defineInRange("timedDefaultSeconds", 30, 1, 3600);
        TIMED_MAX_SECONDS = builder.comment("Maximum random duration for timed coupons.")
                .translation(serverConfigKey("values", "timed_max_seconds"))
                .defineInRange("timedMaxSeconds", 60, 1, 3600);
        ANVIL_MINIMUM_EXPERIENCE_COST = builder.comment("Minimum experience cost left after an anvil experience coupon discount.")
                .translation(serverConfigKey("values", "anvil_minimum_experience_cost"))
                .defineInRange("anvilMinimumExperienceCost", 1, 0, 1000);
        ANVIL_MINIMUM_MATERIAL_COST = builder.comment("Minimum material cost left after a repair material coupon discount.")
                .translation(serverConfigKey("values", "anvil_minimum_material_cost"))
                .defineInRange("anvilMinimumMaterialCost", 1, 0, 64);
        ENCHANTING_PERCENT_PER_REFUND_LEVEL = builder.comment("Discount percent required for each refunded enchanting level.")
                .translation(serverConfigKey("values", "enchanting_percent_per_refund_level"))
                .defineInRange("enchantingPercentPerRefundLevel", 25, 1, 95);
        POTION_DURATION_EXTENSION_TICKS = builder.comment("Ticks added per successful potion duration coupon roll.")
                .translation(serverConfigKey("values", "potion_duration_extension_ticks"))
                .defineInRange("potionDurationExtensionTicks", 1, 1, 200);
        CONSUME_CHANCE_COUPONS_ON_FAILED_ROLL = builder.comment("Consumes random-chance refund coupons even when their refund roll fails.")
                .translation(serverConfigKey("values", "consume_chance_coupons_on_failed_roll"))
                .define("consumeChanceCouponsOnFailedRoll", true);
        CONSUME_DURABILITY_COUPONS_ON_FAILED_ROLL = builder.comment("Consumes durability coupons even when no damage is prevented.")
                .translation(serverConfigKey("values", "consume_durability_coupons_on_failed_roll"))
                .define("consumeDurabilityCouponsOnFailedRoll", true);
        builder.pop();

        builder.push("rollWeights");
        ROLL_WEIGHTS.put(Rarity.COMMON, builder.comment("Random roll weight for common coupons.")
                .translation(serverConfigKey("roll_weights", "common"))
                .defineInRange("common", 100, 0, 100000));
        ROLL_WEIGHTS.put(Rarity.UNCOMMON, builder.comment("Random roll weight for uncommon coupons.")
                .translation(serverConfigKey("roll_weights", "uncommon"))
                .defineInRange("uncommon", 40, 0, 100000));
        ROLL_WEIGHTS.put(Rarity.RARE, builder.comment("Random roll weight for rare coupons.")
                .translation(serverConfigKey("roll_weights", "rare"))
                .defineInRange("rare", 12, 0, 100000));
        ROLL_WEIGHTS.put(Rarity.EPIC, builder.comment("Random roll weight for epic coupons.")
                .translation(serverConfigKey("roll_weights", "epic"))
                .defineInRange("epic", 3, 0, 100000));
        builder.pop();

        builder.push("enabledEffects");
        for (CouponEffectType effect : CouponEffectType.values()) {
            ENABLED_EFFECTS.put(effect, builder.comment("Enables all " + configName(effect) + " coupons.")
                    .translation(serverConfigKey("enabled_effects", configName(effect)))
                    .define(configName(effect), true));
        }
        builder.pop();

        builder.push("enabledModes");
        for (CouponMode mode : CouponMode.values()) {
            ENABLED_MODES.put(mode, builder.comment("Enables all " + configName(mode) + " coupons.")
                    .translation(serverConfigKey("enabled_modes", configName(mode)))
                    .define(configName(mode), true));
        }
        builder.pop();

        builder.push("enabledCoupons");
        for (CouponEffectType effect : CouponEffectType.values()) {
            builder.push(configName(effect));
            EnumMap<CouponMode, ModConfigSpec.BooleanValue> byMode = new EnumMap<>(CouponMode.class);
            for (CouponMode mode : CouponMode.values()) {
                byMode.put(mode, builder.comment("Enables this exact coupon. Set false to fully deactivate it.")
                        .translation(serverConfigKey("enabled_coupons", configName(effect), configName(mode)))
                        .define(configName(mode), true));
            }
            ENABLED_COUPONS.put(effect, byMode);
            builder.pop();
        }
        builder.pop();

        SPEC = builder.build();
    }

    private CouponConfig() {
    }

    public static boolean areCouponsEnabled() {
        return ENABLE_COUPONS.get();
    }

    public static boolean canRollEmptyCoupons() {
        return areCouponsEnabled() && ENABLE_EMPTY_COUPON_ROLLS.get();
    }

    public static boolean areCouponPouchesEnabled() {
        return areCouponsEnabled() && ENABLE_COUPON_POUCHES.get();
    }

    public static boolean areCommandsEnabled() {
        return ENABLE_COMMANDS.get();
    }

    public static boolean showTimedCouponBossBar() {
        return SHOW_TIMED_COUPON_BOSS_BAR.get();
    }

    public static boolean areDailyBoostsEnabled() {
        return areCouponsEnabled()
                && ENABLE_DAILY_BOOSTS.get()
                && (dailyBoostStrengthMultiplier() > 1
                || dailyBoostUseMultiplier() > 1
                || dailyBoostDurationMultiplier() > 1);
    }

    public static boolean announceDailyBoosts() {
        return areDailyBoostsEnabled() && ANNOUNCE_DAILY_BOOSTS.get();
    }

    public static String dailyBoostAnnouncementMessage() {
        String message = DAILY_BOOST_ANNOUNCEMENT_MESSAGE.get();
        return message == null || message.isBlank() ? DEFAULT_DAILY_BOOST_ANNOUNCEMENT : message;
    }

    public static int dailyBoostCategoryChance() {
        return DAILY_BOOST_CATEGORY_CHANCE.get();
    }

    public static int dailyBoostStrengthMultiplier() {
        return DAILY_BOOST_STRENGTH_MULTIPLIER.get();
    }

    public static int dailyBoostUseMultiplier() {
        return DAILY_BOOST_USE_MULTIPLIER.get();
    }

    public static int dailyBoostDurationMultiplier() {
        return DAILY_BOOST_DURATION_MULTIPLIER.get();
    }

    public static boolean allowCouponsInPouches() {
        return areCouponPouchesEnabled() && ALLOW_COUPONS_IN_POUCHES.get();
    }

    public static boolean allowCouponsInShulkerBoxes() {
        return areCouponsEnabled() && ALLOW_COUPONS_IN_SHULKER_BOXES.get();
    }

    public static int containerSearchDepth() {
        return CONTAINER_SEARCH_DEPTH.get();
    }

    public static boolean allowTimedCouponInventoryActivation() {
        return areCouponsEnabled() && ALLOW_TIMED_COUPON_INVENTORY_ACTIVATION.get();
    }

    public static boolean allowTimedCouponPouchActivation() {
        return areCouponPouchesEnabled() && ALLOW_TIMED_COUPON_POUCH_ACTIVATION.get();
    }

    public static boolean allowTimedCouponDurationStacking() {
        return ALLOW_TIMED_COUPON_DURATION_STACKING.get();
    }

    public static int maxActiveTimedCoupons() {
        return MAX_ACTIVE_TIMED_COUPONS.get();
    }

    public static boolean playActivationFeedback() {
        return PLAY_ACTIVATION_FEEDBACK.get();
    }

    public static boolean playUseFeedback() {
        return PLAY_USE_FEEDBACK.get();
    }

    public static int useFeedbackCooldownTicks() {
        return USE_FEEDBACK_COOLDOWN_TICKS.get();
    }

    public static int activationParticleCount() {
        return ACTIVATION_PARTICLE_COUNT.get();
    }

    public static int useParticleCount() {
        return USE_PARTICLE_COUNT.get();
    }

    public static boolean isCouponEnabled(CouponEffectType effect, CouponMode mode) {
        return areCouponsEnabled()
                && ENABLED_EFFECTS.get(effect).get()
                && ENABLED_MODES.get(mode).get()
                && ENABLED_COUPONS.get(effect).get(mode).get();
    }

    public static int randomDiscount(RandomSource random) {
        List<Integer> values = DISCOUNT_VALUES.get().stream()
                .filter(CouponConfig::isValidDiscount)
                .map(value -> Mth.clamp(value, 1, 95))
                .toList();
        if (values.isEmpty()) {
            return 10;
        }
        return values.get(random.nextInt(values.size()));
    }

    public static int randomMultiUses(RandomSource random) {
        int min = multiUseMinUses();
        int max = multiUseMaxUses();
        return min + random.nextInt(max - min + 1);
    }

    public static int multiUseDefaultUses() {
        return Mth.clamp(MULTI_USE_DEFAULT_USES.get(), multiUseMinUses(), multiUseMaxUses());
    }

    public static int multiUseMinUses() {
        return Math.min(MULTI_USE_MIN_USES.get(), MULTI_USE_MAX_USES.get());
    }

    public static int multiUseMaxUses() {
        return Math.max(MULTI_USE_MIN_USES.get(), MULTI_USE_MAX_USES.get());
    }

    public static int randomTimedSeconds(RandomSource random) {
        int min = timedMinSeconds();
        int max = timedMaxSeconds();
        return min + random.nextInt(max - min + 1);
    }

    public static int timedDefaultSeconds() {
        return Mth.clamp(TIMED_DEFAULT_SECONDS.get(), timedMinSeconds(), timedMaxSeconds());
    }

    public static int timedMinSeconds() {
        return Math.min(TIMED_MIN_SECONDS.get(), TIMED_MAX_SECONDS.get());
    }

    public static int timedMaxSeconds() {
        return Math.max(TIMED_MIN_SECONDS.get(), TIMED_MAX_SECONDS.get());
    }

    public static int anvilMinimumExperienceCost() {
        return ANVIL_MINIMUM_EXPERIENCE_COST.get();
    }

    public static int anvilMinimumMaterialCost() {
        return ANVIL_MINIMUM_MATERIAL_COST.get();
    }

    public static int enchantingPercentPerRefundLevel() {
        return ENCHANTING_PERCENT_PER_REFUND_LEVEL.get();
    }

    public static int potionDurationExtensionTicks() {
        return POTION_DURATION_EXTENSION_TICKS.get();
    }

    public static boolean consumeChanceCouponsOnFailedRoll() {
        return CONSUME_CHANCE_COUPONS_ON_FAILED_ROLL.get();
    }

    public static boolean consumeDurabilityCouponsOnFailedRoll() {
        return CONSUME_DURABILITY_COUPONS_ON_FAILED_ROLL.get();
    }

    public static int rollWeight(Rarity rarity) {
        return Math.max(0, ROLL_WEIGHTS.get(rarity).get());
    }

    private static boolean isValidDiscount(Object value) {
        return value instanceof Integer discount && discount >= 1 && discount <= 95;
    }

    private static String configName(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String serverConfigKey(String... path) {
        return configKey("server", path);
    }

    private static String configKey(String type, String... path) {
        return "config.coupon_codes." + type + "." + String.join(".", path);
    }
}
