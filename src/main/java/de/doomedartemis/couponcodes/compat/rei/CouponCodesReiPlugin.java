package de.doomedartemis.couponcodes.compat.rei;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@REIPluginClient
public final class CouponCodesReiPlugin implements REIClientPlugin {
    @Override
    public String getPluginProviderName() {
        return CouponCodes.MOD_ID + ":rei";
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        addInformation(
                registry,
                ModItems.EMPTY_COUPON.get().getDefaultInstance(),
                Component.translatable("item.coupon_codes.empty_coupon"),
                Component.translatable("rei.coupon_codes.empty_coupon")
        );
        addInformation(
                registry,
                ModItems.COUPON_POUCH.get().getDefaultInstance(),
                Component.translatable("item.coupon_codes.coupon_pouch"),
                Component.translatable("rei.coupon_codes.coupon_pouch")
        );

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
}
