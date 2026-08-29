package de.doomedartemis.couponcodes.common.item;

import de.doomedartemis.couponcodes.common.advancement.CouponCriteria;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.coupon.CouponFeedback;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Optional;
import java.util.function.Consumer;

public class EmptyCouponItem extends Item {
    public EmptyCouponItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        if (CouponConfig.canRollEmptyCoupons()) {
            tooltip.accept(Component.translatable("item.coupon_codes.empty_coupon.tooltip").withStyle(ChatFormatting.GRAY));
            if (flag.isAdvanced()) {
                tooltip.accept(Component.translatable("item.coupon_codes.empty_coupon.note").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.accept(Component.translatable("item.coupon_codes.empty_coupon.disabled").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!CouponConfig.canRollEmptyCoupons()) {
            return InteractionResult.FAIL;
        }

        Optional<DeferredItem<CouponItem>> rolledCoupon = ModItems.randomCoupon(player.getRandom());
        if (rolledCoupon.isEmpty()) {
            return InteractionResult.FAIL;
        }

        ItemStack coupon = new ItemStack(rolledCoupon.get().get());
        if (coupon.getItem() instanceof CouponItem couponItem) {
            CouponData.initializeIfNeeded(coupon, couponItem, player.getRandom());
            CouponFeedback.playActivation(player, couponItem);
            if (player instanceof ServerPlayer serverPlayer) {
                CouponCriteria.triggerRolled(serverPlayer, couponItem.effect(), couponItem.mode());
            }
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (!player.getInventory().add(coupon)) {
            player.drop(coupon, false);
        }

        return InteractionResult.SUCCESS;
    }
}
