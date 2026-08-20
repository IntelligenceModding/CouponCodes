package de.doomedartemis.couponcodes.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.doomedartemis.couponcodes.common.registry.ModLootModifiers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class VanillaChestCouponLootModifier extends LootModifier {
    public static final MapCodec<VanillaChestCouponLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, VanillaChestCouponLootModifier::new));

    public VanillaChestCouponLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.addAll(CouponLootDataManager.generate(
                CouponLootDataManager.lootTableProfile(context.getQueriedLootTableId()),
                context.getRandom()
        ));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.VANILLA_CHEST_COUPONS.get();
    }
}
