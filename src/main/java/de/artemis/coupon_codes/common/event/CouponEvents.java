package de.artemis.coupon_codes.common.event;

import de.artemis.coupon_codes.common.coupon.CouponData;
import de.artemis.coupon_codes.common.coupon.CouponEffectType;
import de.artemis.coupon_codes.common.item.CouponItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CouponEvents {
    private static final Map<UUID, Map<String, Integer>> LAST_DAMAGE_BY_SLOT = new HashMap<>();

    private CouponEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        CouponData.tickCouponsInInventory(player);
        reduceNewDurabilityDamage(player);
        CouponBossBars.update(player);
    }

    public static void onPlayerEnchantItem(PlayerEnchantItemEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack coupon = findBestCoupon(player, CouponEffectType.ENCHANTING_EXPERIENCE);
        if (coupon.isEmpty()) {
            return;
        }

        int refundLevels = Math.max(1, Math.round(CouponData.discountPercent(coupon) / 25.0F));
        serverPlayer.giveExperienceLevels(refundLevels);
        CouponData.consumeUse(coupon, (CouponItem) coupon.getItem());
    }

    private static void reduceNewDurabilityDamage(Player player) {
        Inventory inventory = player.getInventory();
        Map<String, Integer> previousDamage = LAST_DAMAGE_BY_SLOT.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());

        trackAndDiscountContainer(player, previousDamage, "main", inventory.items);
        trackAndDiscountContainer(player, previousDamage, "armor", inventory.armor);
        trackAndDiscountContainer(player, previousDamage, "offhand", inventory.offhand);
    }

    private static void trackAndDiscountContainer(Player player, Map<String, Integer> previousDamage, String prefix, Iterable<ItemStack> stacks) {
        int slot = 0;
        for (ItemStack stack : stacks) {
            String key = prefix + ":" + slot;
            slot++;

            if (stack.isEmpty() || !stack.isDamageableItem()) {
                previousDamage.remove(key);
                continue;
            }

            int currentDamage = stack.getDamageValue();
            int lastDamage = previousDamage.getOrDefault(key, currentDamage);
            int newDamage = currentDamage - lastDamage;

            if (newDamage > 0) {
                ItemStack coupon = findBestCoupon(player, CouponEffectType.DURABILITY);
                if (!coupon.isEmpty()) {
                    int preventedDamage = preventedDamage(player, newDamage, CouponData.discountPercent(coupon));
                    if (preventedDamage > 0) {
                        stack.setDamageValue(Math.max(0, currentDamage - preventedDamage));
                    }
                    CouponData.consumeUse(coupon, (CouponItem) coupon.getItem());
                    currentDamage = stack.getDamageValue();
                }
            }

            previousDamage.put(key, currentDamage);
        }
    }

    private static ItemStack findBestCoupon(Player player, CouponEffectType effect) {
        ItemStack bestCoupon = ItemStack.EMPTY;
        int bestDiscount = -1;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CouponItem coupon) {
                int discount = matchingDiscount(stack, coupon, effect);
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestCoupon = stack;
                }
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof CouponItem coupon) {
                int discount = matchingDiscount(stack, coupon, effect);
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestCoupon = stack;
                }
            }
        }

        return bestCoupon;
    }

    private static int matchingDiscount(ItemStack stack, CouponItem coupon, CouponEffectType effect) {
        return CouponData.matches(stack, coupon, effect) ? CouponData.discountPercent(stack) : -1;
    }

    private static int preventedDamage(Player player, int damage, int discountPercent) {
        int preventedDamage = 0;
        for (int i = 0; i < damage; i++) {
            if (player.getRandom().nextInt(100) < discountPercent) {
                preventedDamage++;
            }
        }
        return preventedDamage;
    }
}
