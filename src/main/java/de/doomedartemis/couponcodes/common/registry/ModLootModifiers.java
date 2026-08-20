package de.doomedartemis.couponcodes.common.registry;

import com.mojang.serialization.MapCodec;
import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.loot.VanillaChestCouponLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, CouponCodes.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<VanillaChestCouponLootModifier>> VANILLA_CHEST_COUPONS =
            LOOT_MODIFIER_SERIALIZERS.register("vanilla_chest_coupons", () -> VanillaChestCouponLootModifier.CODEC);

    private ModLootModifiers() {
    }

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
