package de.doomedartemis.couponcodes.compat.jei;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IModInfoRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class CouponCodesJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(CouponCodes.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerModInfo(IModInfoRegistration registration) {
        registration.addModAliases(CouponCodes.MOD_ID, List.of("coupons", "coupon codes"));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, couponPouchDyeingRecipes());
        registration.addItemStackInfo(
                ModItems.EMPTY_COUPON.get().getDefaultInstance(),
                Component.translatable("jei.coupon_codes.empty_coupon")
        );
        ModItems.allCouponPouches().forEach(item -> registration.addItemStackInfo(
                item.get().getDefaultInstance(),
                Component.translatable("jei.coupon_codes.coupon_pouch")
        ));

        for (CouponEffectType effect : CouponEffectType.values()) {
            for (CouponMode mode : CouponMode.values()) {
                registration.addItemStackInfo(
                        ModItems.couponItem(effect, mode).get().getDefaultInstance(),
                        Component.translatable("jei.coupon_codes.coupon.common"),
                        Component.translatable("item.coupon_codes.coupon.mode." + mode.id() + ".pending"),
                        Component.translatable("item.coupon_codes.coupon.scope." + effect.id()),
                        Component.translatable("item.coupon_codes.coupon.note." + effect.id())
                );
            }
        }
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        addAliases(registration, ModItems.EMPTY_COUPON.get().getDefaultInstance(), "random coupon");
        ModItems.allCouponPouches().forEach(item -> addAliases(registration, item.get().getDefaultInstance(), "coupon storage"));

        for (CouponEffectType effect : CouponEffectType.values()) {
            for (CouponMode mode : CouponMode.values()) {
                ItemStack stack = ModItems.couponItem(effect, mode).get().getDefaultInstance();
                List<String> aliases = effectAliases(effect);
                if (!aliases.isEmpty()) {
                    addAliases(registration, stack, aliases);
                }
            }
        }
    }

    private static List<String> effectAliases(CouponEffectType effect) {
        return switch (effect) {
            case DURABILITY -> List.of("tool damage", "armor damage");
            case ENCHANTING_EXPERIENCE -> List.of("levels");
            case ANVIL_EXPERIENCE -> List.of("repair cost");
            case TOOL_REPAIR -> List.of("grindstone", "tool repair");
            case VILLAGER_TRADE -> List.of("emerald");
            case VILLAGER_RESTOCK -> List.of("villager restock", "trade reset");
            case ARROW -> List.of("bow", "crossbow");
            case FOOD -> List.of("hunger");
            case POTION_DURATION -> List.of("status effect");
            case MENDING -> List.of("xp repair", "mending");
            case TOTEM -> List.of("undying");
            case SMITHING_TEMPLATE -> List.of("armor trim", "netherite upgrade");
            case REPAIR_MATERIAL -> List.of("anvil repair");
            case BONE_MEAL -> List.of("bonemeal");
            case FISHING -> List.of("fishing rod", "fishing");
            case ROCKET -> List.of("firework", "elytra");
            case ENDER_PEARL -> List.of("teleport");
            case ELYTRA_GLIDE -> List.of("elytra durability", "gliding");
            case FALL_DAMAGE -> List.of("landing");
            case DEATH_DROP -> List.of("keep inventory");
        };
    }

    private static void addAliases(IIngredientAliasRegistration registration, ItemStack stack, String... aliases) {
        registration.addAliases(VanillaTypes.ITEM_STACK, stack, List.of(aliases));
    }

    private static void addAliases(IIngredientAliasRegistration registration, ItemStack stack, List<String> aliases) {
        registration.addAliases(VanillaTypes.ITEM_STACK, stack, aliases);
    }

    private static List<RecipeHolder<CraftingRecipe>> couponPouchDyeingRecipes() {
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            ModItems.coloredCouponPouch(color).ifPresent(target -> {
                Item targetItem = target.get().asItem();
                List<Ingredient> ingredients = List.of(
                        Ingredient.of(ModItems.allCouponPouches().stream()
                                .map(item -> item.get().asItem())
                                .filter(item -> item != targetItem)),
                        Ingredient.of(DyeItem.byColor(color))
                );
                ShapelessRecipe recipe = new ShapelessRecipe(
                        "coupon_pouch_dyeing",
                        CraftingBookCategory.MISC,
                        target.get().getDefaultInstance(),
                        ingredients
                );
                recipes.add(new RecipeHolder<>(
                        ResourceKey.create(Registries.RECIPE, dyeingRecipeId(color)),
                        recipe
                ));
            });
        }
        return recipes;
    }

    private static Identifier dyeingRecipeId(DyeColor color) {
        return Identifier.fromNamespaceAndPath(CouponCodes.MOD_ID, "coupon_pouch_dyeing/" + color.getName());
    }
}
