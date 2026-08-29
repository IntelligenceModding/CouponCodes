package de.doomedartemis.couponcodes.common.registry;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.recipe.DyeCouponPouchRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CouponCodes.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DyeCouponPouchRecipe>> COUPON_POUCH_DYEING =
            RECIPE_SERIALIZERS.register("coupon_pouch_dyeing", () -> new RecipeSerializer<>(DyeCouponPouchRecipe.CODEC, DyeCouponPouchRecipe.STREAM_CODEC));

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
