package de.doomedartemis.couponcodes.common.advancement;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.Optional;

public final class AdvancementRewards {
    private AdvancementRewards() {
    }

    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!CouponConfig.areAdvancementRewardsEnabled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Identifier advancementId = event.getAdvancement().id();
        if (!CouponCodes.MOD_ID.equals(advancementId.getNamespace())) {
            return;
        }

        Optional<Item> rewardItem = rewardItem();
        if (rewardItem.isEmpty()) {
            return;
        }

        giveStackCount(player, rewardItem.get(), CouponConfig.advancementRewardCount());
    }

    private static Optional<Item> rewardItem() {
        Identifier itemId;
        try {
            itemId = Identifier.parse(CouponConfig.advancementRewardItem());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }

        return BuiltInRegistries.ITEM.getOptional(itemId)
                .filter(item -> item != Items.AIR);
    }

    private static void giveStackCount(ServerPlayer player, Item item, int count) {
        int remaining = count;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, item.getDefaultMaxStackSize());
            giveOrDrop(player, rewardStack(player, item, stackSize));
            remaining -= stackSize;
        }
    }

    private static ItemStack rewardStack(ServerPlayer player, Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (item instanceof CouponItem coupon) {
            CouponData.initializeIfNeeded(stack, coupon, player.getRandom());
        }
        return stack;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.getInventory().add(stack)) {
            ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
            itemEntity.setDeltaMovement(0.0, 0.0, 0.0);
            player.level().addFreshEntity(itemEntity);
        }
    }
}
