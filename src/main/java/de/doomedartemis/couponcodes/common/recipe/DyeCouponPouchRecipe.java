package de.doomedartemis.couponcodes.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import de.doomedartemis.couponcodes.common.registry.ModRecipeSerializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

public class DyeCouponPouchRecipe implements CraftingRecipe {
    public static final MapCodec<DyeCouponPouchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(DyeCouponPouchRecipe::category),
                    DyeColor.CODEC.fieldOf("color").forGetter(recipe -> recipe.targetColor)
            ).apply(instance, DyeCouponPouchRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DyeCouponPouchRecipe> STREAM_CODEC = StreamCodec.of(
            DyeCouponPouchRecipe::toNetwork,
            DyeCouponPouchRecipe::fromNetwork
    );

    private static final String GROUP = "coupon_pouch_dyeing";
    private final CraftingBookCategory category;
    private final DyeColor targetColor;

    public DyeCouponPouchRecipe(CraftingBookCategory category, DyeColor targetColor) {
        this.category = category;
        this.targetColor = targetColor;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        RecipeInputs inputs = findInputs(input, targetColor);
        return inputs != null && targetItem(targetColor) != null && inputs.pouch().getItem() != targetItem(targetColor);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
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
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModRecipeSerializers.COUPON_POUCH_DYEING.get();
    }

    @Override
    public String group() {
        return GROUP;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredients());
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    private List<Ingredient> ingredients() {
        Item targetItem = targetItem(targetColor);
        return List.of(
                Ingredient.of(ModItems.allCouponPouches().stream()
                        .map(item -> item.get().asItem())
                        .filter(item -> item != targetItem)),
                Ingredient.of(dyeItem(targetColor))
        );
    }

    public ItemStack getResultItem() {
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
            } else if (stack.getItem() instanceof DyeItem) {
                if (dye != null) {
                    return null;
                }
                dye = stack.get(DataComponents.DYE);
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

    public static Item dyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }

    private record RecipeInputs(ItemStack pouch) {
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
