package de.doomedartemis.couponcodes.compat.rei;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

@REIPluginClient
public final class CouponCodesReiPlugin implements REIClientPlugin {
    @Override
    public String getPluginProviderName() {
        return CouponCodes.MOD_ID + ":rei";
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        addCouponPouchDyeingDisplays(registry);
        addInformation(
                registry,
                ModItems.EMPTY_COUPON.get().getDefaultInstance(),
                Component.translatable("item.coupon_codes.empty_coupon"),
                Component.translatable("rei.coupon_codes.empty_coupon")
        );
        ModItems.allCouponPouches().forEach(item -> addInformation(
                registry,
                item.get().getDefaultInstance(),
                item.get().getDefaultInstance().getHoverName(),
                Component.translatable("rei.coupon_codes.coupon_pouch")
        ));

        for (CouponEffectType effect : CouponEffectType.values()) {
            for (CouponMode mode : CouponMode.values()) {
                ItemStack stack = ModItems.couponItem(effect, mode).get().getDefaultInstance();
                addInformation(
                        registry,
                        stack,
                        stack.getHoverName(),
                        Component.translatable("rei.coupon_codes.coupon.common"),
                        Component.translatable("item.coupon_codes.coupon.mode." + mode.id() + ".pending"),
                        Component.translatable("item.coupon_codes.coupon.scope." + effect.id()),
                        Component.translatable("item.coupon_codes.coupon.note." + effect.id())
                );
            }
        }
    }

    private static void addInformation(DisplayRegistry registry, ItemStack stack, Component name, Component... lines) {
        registry.add(DefaultInformationDisplay.createFromEntries(
                EntryIngredient.of(EntryStacks.of(stack)),
                name
        ).lines(lines));
    }

    private static void addCouponPouchDyeingDisplays(DisplayRegistry registry) {
        for (DyeColor color : DyeColor.values()) {
            ModItems.coloredCouponPouch(color).ifPresent(target -> {
                Item targetItem = target.get().asItem();
                registry.add(new DefaultCustomShapelessDisplay(
                        List.of(
                                EntryIngredients.ofItemStacks(ModItems.allCouponPouches().stream()
                                        .map(item -> item.get().asItem())
                                        .filter(item -> item != targetItem)
                                        .map(Item::getDefaultInstance)
                                        .toList()),
                                EntryIngredients.of(DyeItem.byColor(color))
                        ),
                        List.of(EntryIngredients.of(targetItem)),
                        Optional.of(dyeingRecipeId(color))
                ));
            });
        }
    }

    private static Identifier dyeingRecipeId(DyeColor color) {
        return Identifier.fromNamespaceAndPath(CouponCodes.MOD_ID, "coupon_pouch_dyeing/" + color.getName());
    }
}
