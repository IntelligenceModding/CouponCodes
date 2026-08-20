package de.doomedartemis.couponcodes.common.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.coupon.CouponCategory;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Locale;
import java.util.Optional;

public final class CouponCriteria {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, CouponCodes.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, CouponTrigger> COUPON_ROLLED =
            TRIGGERS.register("coupon_rolled", CouponTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, CouponTrigger> COUPON_ACTIVATED =
            TRIGGERS.register("coupon_activated", CouponTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, CouponTrigger> COUPON_USED =
            TRIGGERS.register("coupon_used", CouponTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, CouponTrigger> COUPON_OBTAINED =
            TRIGGERS.register("coupon_obtained", CouponTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, EntityCouponTrigger> ENTITY_COUPON_DROPPED =
            TRIGGERS.register("entity_coupon_dropped", EntityCouponTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PouchOpenedTrigger> POUCH_OPENED =
            TRIGGERS.register("pouch_opened", PouchOpenedTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PouchStockedTrigger> POUCH_STOCKED =
            TRIGGERS.register("pouch_stocked", PouchStockedTrigger::new);

    private CouponCriteria() {
    }

    public static void register(IEventBus eventBus) {
        TRIGGERS.register(eventBus);
    }

    public static void triggerRolled(ServerPlayer player, CouponEffectType effect, CouponMode mode) {
        COUPON_ROLLED.get().trigger(player, effect, mode);
    }

    public static void triggerActivated(ServerPlayer player, CouponEffectType effect, CouponMode mode) {
        COUPON_ACTIVATED.get().trigger(player, effect, mode);
    }

    public static void triggerUsed(ServerPlayer player, CouponEffectType effect, CouponMode mode) {
        COUPON_USED.get().trigger(player, effect, mode);
    }

    public static void triggerObtained(ServerPlayer player, CouponEffectType effect, CouponMode mode) {
        COUPON_OBTAINED.get().trigger(player, effect, mode);
    }

    public static void triggerEntityCouponDropped(ServerPlayer player, ResourceLocation entityType, CouponEffectType effect, CouponMode mode) {
        ENTITY_COUPON_DROPPED.get().trigger(player, entityType, effect, mode);
    }

    public static void triggerPouchOpened(ServerPlayer player) {
        POUCH_OPENED.get().trigger(player);
    }

    public static void triggerPouchStocked(ServerPlayer player, int couponCount) {
        POUCH_STOCKED.get().trigger(player, couponCount);
    }

    public static final class CouponTrigger extends SimpleCriterionTrigger<CouponTrigger.TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, CouponEffectType effect, CouponMode mode) {
            trigger(player, instance -> instance.matches(effect, mode));
        }

        public record TriggerInstance(
                Optional<ContextAwarePredicate> player,
                Optional<CouponEffectType> effect,
                Optional<CouponCategory> category,
                Optional<CouponMode> mode,
                Optional<Rarity> minRarity
        ) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<CouponEffectType> EFFECT_CODEC = Codec.STRING.comapFlatMap(
                    value -> enumValue(CouponEffectType.class, value),
                    value -> value.name().toLowerCase(Locale.ROOT)
            );
            public static final Codec<CouponMode> MODE_CODEC = Codec.STRING.comapFlatMap(
                    value -> enumValue(CouponMode.class, value),
                    value -> value.name().toLowerCase(Locale.ROOT)
            );
            public static final Codec<CouponCategory> CATEGORY_CODEC = Codec.STRING.comapFlatMap(
                    value -> enumValue(CouponCategory.class, value),
                    value -> value.name().toLowerCase(Locale.ROOT)
            );
            public static final Codec<Rarity> RARITY_CODEC = Codec.STRING.comapFlatMap(
                    value -> enumValue(Rarity.class, value),
                    value -> value.name().toLowerCase(Locale.ROOT)
            );
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    EFFECT_CODEC.optionalFieldOf("effect").forGetter(TriggerInstance::effect),
                    CATEGORY_CODEC.optionalFieldOf("category").forGetter(TriggerInstance::category),
                    MODE_CODEC.optionalFieldOf("mode").forGetter(TriggerInstance::mode),
                    RARITY_CODEC.optionalFieldOf("min_rarity").forGetter(TriggerInstance::minRarity)
                    )
                    .apply(instance, TriggerInstance::new));

            public boolean matches(CouponEffectType effect, CouponMode mode) {
                return this.effect.map(value -> value == effect).orElse(true)
                        && this.category.map(value -> value == effect.category()).orElse(true)
                        && this.mode.map(value -> value == mode).orElse(true)
                        && this.minRarity.map(value -> rarityScore(ModItems.couponRarity(effect, mode)) >= rarityScore(value)).orElse(true);
            }

            private static <T extends Enum<T>> DataResult<T> enumValue(Class<T> enumClass, String value) {
                try {
                    return DataResult.success(Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown " + enumClass.getSimpleName() + " value: " + value);
                }
            }

            private static int rarityScore(Rarity rarity) {
                return switch (rarity) {
                    case COMMON -> 0;
                    case UNCOMMON -> 1;
                    case RARE -> 2;
                    case EPIC -> 3;
                };
            }
        }
    }

    public static final class EntityCouponTrigger extends SimpleCriterionTrigger<EntityCouponTrigger.TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, ResourceLocation entityType, CouponEffectType effect, CouponMode mode) {
            trigger(player, instance -> instance.matches(entityType, effect, mode));
        }

        public record TriggerInstance(
                Optional<ContextAwarePredicate> player,
                Optional<ResourceLocation> entity,
                Optional<CouponEffectType> effect,
                Optional<CouponCategory> category,
                Optional<CouponMode> mode
        ) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity),
                    CouponTrigger.TriggerInstance.EFFECT_CODEC.optionalFieldOf("effect").forGetter(TriggerInstance::effect),
                    CouponTrigger.TriggerInstance.CATEGORY_CODEC.optionalFieldOf("category").forGetter(TriggerInstance::category),
                    CouponTrigger.TriggerInstance.MODE_CODEC.optionalFieldOf("mode").forGetter(TriggerInstance::mode)
                    )
                    .apply(instance, TriggerInstance::new));

            public boolean matches(ResourceLocation entityType, CouponEffectType effect, CouponMode mode) {
                return this.entity.map(value -> value.equals(entityType)).orElse(true)
                        && this.effect.map(value -> value == effect).orElse(true)
                        && this.category.map(value -> value == effect.category()).orElse(true)
                        && this.mode.map(value -> value == mode).orElse(true);
            }
        }
    }

    public static final class PouchOpenedTrigger extends SimpleCriterionTrigger<PouchOpenedTrigger.TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            trigger(player, ignored -> true);
        }

        public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
                    )
                    .apply(instance, TriggerInstance::new));
        }
    }

    public static final class PouchStockedTrigger extends SimpleCriterionTrigger<PouchStockedTrigger.TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, int couponCount) {
            trigger(player, instance -> instance.matches(couponCount));
        }

        public record TriggerInstance(
                Optional<ContextAwarePredicate> player,
                int minCoupons
        ) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                            Codec.INT.optionalFieldOf("min_coupons", 1).forGetter(TriggerInstance::minCoupons)
                    )
                    .apply(instance, TriggerInstance::new));

            public boolean matches(int couponCount) {
                return couponCount >= minCoupons;
            }
        }
    }
}
