package de.doomedartemis.couponcodes.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import de.doomedartemis.couponcodes.common.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class DyeCouponPouchRecipe extends CustomRecipe {
    private static final String GROUP = "coupon_pouch_dyeing";
    private final DyeColor targetColor;

    public DyeCouponPouchRecipe(CraftingBookCategory category, DyeColor targetColor) {
        super(category);
        this.targetColor = targetColor;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        RecipeInputs inputs = findInputs(input, targetColor);
        return inputs != null && targetItem(targetColor) != null && inputs.pouch().getItem() != targetItem(targetColor);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        RecipeInputs inputs = findInputs(input, targetColor);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }

        Item target = targetItem(targetColor);
        return target == null || inputs.pouch().getItem() == target
                ? ItemStack.EMPTY
                : inputs.pouch().transmuteCopy(target, 1);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.COUPON_POUCH_DYEING.get();
    }

    @Override
    public String getGroup() {
        return GROUP;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        Item targetItem = targetItem(targetColor);
        return NonNullList.of(
                Ingredient.EMPTY,
                Ingredient.of(ModItems.allCouponPouches().stream()
                        .map(item -> item.get().asItem())
                        .filter(item -> item != targetItem)
                        .map(Item::getDefaultInstance)),
                Ingredient.of(DyeItem.byColor(targetColor))
        );
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        Item target = targetItem(targetColor);
        return target == null ? ItemStack.EMPTY : target.getDefaultInstance();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    private static RecipeInputs findInputs(CraftingInput input, DyeColor targetColor) {
        ItemStack pouch = ItemStack.EMPTY;
        DyeColor dye = null;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof CouponPouchItem) {
                if (!pouch.isEmpty()) {
                    return null;
                }
                pouch = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                if (dye != null) {
                    return null;
                }
                dye = dyeItem.getDyeColor();
            } else {
                return null;
            }
        }

        return !pouch.isEmpty() && dye == targetColor ? new RecipeInputs(pouch) : null;
    }

    private static Item targetItem(DyeColor color) {
        return ModItems.coloredCouponPouch(color)
                .map(item -> item.get().asItem())
                .orElse(null);
    }

    private record RecipeInputs(ItemStack pouch) {
    }

    public static final class Serializer implements RecipeSerializer<DyeCouponPouchRecipe> {
        private static final MapCodec<DyeCouponPouchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(DyeCouponPouchRecipe::category),
                        DyeColor.CODEC.fieldOf("color").forGetter(recipe -> recipe.targetColor)
                ).apply(instance, DyeCouponPouchRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, DyeCouponPouchRecipe> STREAM_CODEC = StreamCodec.of(
                DyeCouponPouchRecipe.Serializer::toNetwork,
                DyeCouponPouchRecipe.Serializer::fromNetwork
        );

        @Override
        public MapCodec<DyeCouponPouchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DyeCouponPouchRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static DyeCouponPouchRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            DyeColor color = buffer.readEnum(DyeColor.class);
            return new DyeCouponPouchRecipe(category, color);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, DyeCouponPouchRecipe recipe) {
            buffer.writeEnum(recipe.category());
            buffer.writeEnum(recipe.targetColor);
        }
    }
}
