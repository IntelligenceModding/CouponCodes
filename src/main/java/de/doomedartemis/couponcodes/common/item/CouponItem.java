package de.doomedartemis.couponcodes.common.item;

import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.coupon.CouponFeedback;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CouponItem extends Item {
    private final CouponEffectType effect;
    private final CouponMode mode;

    public CouponItem(CouponEffectType effect, CouponMode mode, Properties properties) {
        super(properties);
        this.effect = effect;
        this.mode = mode;
    }

    public CouponEffectType effect() {
        return effect;
    }

    public CouponMode mode() {
        return mode;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide() || mode != CouponMode.TIMED) {
            return InteractionResultHolder.pass(stack);
        }
        if (!CouponConfig.isCouponEnabled(effect, mode)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!CouponConfig.allowTimedCouponInventoryActivation()) {
            return InteractionResultHolder.fail(stack);
        }

        CouponData.initializeIfNeeded(stack, this, player.getRandom());
        if (!CouponData.activateTimed(player, stack, this)) {
            return InteractionResultHolder.fail(stack);
        }

        CouponFeedback.playActivation(player, this);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CouponData.appendHoverText(stack, this, context.level(), tooltip, flag);
    }
}
