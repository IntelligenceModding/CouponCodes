package de.artemis.coupon_codes.common.event;

import de.artemis.coupon_codes.common.coupon.CouponData;
import de.artemis.coupon_codes.common.item.CouponItem;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CouponBossBars {
    private static final Map<UUID, ServerBossEvent> BOSS_BARS = new HashMap<>();

    private CouponBossBars() {
    }

    public static void update(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack timedCoupon = findBestActiveTimedCoupon(serverPlayer);
        if (timedCoupon.isEmpty() || !(timedCoupon.getItem() instanceof CouponItem coupon)) {
            remove(serverPlayer);
            return;
        }

        ServerBossEvent bossBar = BOSS_BARS.computeIfAbsent(
                serverPlayer.getUUID(),
                ignored -> create(serverPlayer)
        );

        if (!bossBar.getPlayers().contains(serverPlayer)) {
            bossBar.addPlayer(serverPlayer);
        }

        bossBar.setName(CouponData.bossBarName(timedCoupon, coupon));
        bossBar.setProgress(CouponData.timedProgress(timedCoupon));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            remove(serverPlayer);
        }
    }

    private static ServerBossEvent create(ServerPlayer player) {
        ServerBossEvent bossBar = new ServerBossEvent(
                Component.translatable("bossbar.coupon_codes.timed_coupon.empty"),
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.addPlayer(player);
        return bossBar;
    }

    private static void remove(ServerPlayer player) {
        ServerBossEvent bossBar = BOSS_BARS.remove(player.getUUID());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    private static ItemStack findBestActiveTimedCoupon(Player player) {
        ItemStack bestCoupon = ItemStack.EMPTY;
        int bestDiscount = -1;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CouponItem coupon && CouponData.isActiveTimedCoupon(stack, coupon)) {
                int discount = CouponData.discountPercent(stack);
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestCoupon = stack;
                }
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof CouponItem coupon && CouponData.isActiveTimedCoupon(stack, coupon)) {
                int discount = CouponData.discountPercent(stack);
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestCoupon = stack;
                }
            }
        }

        return bestCoupon;
    }
}
