package de.doomedartemis.couponcodes.common.event;

import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CouponBossBars {
    private static final Map<UUID, ServerBossEvent> BOSS_BARS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SECONDS = new HashMap<>();
    private static final int TICK_SOUND_START_SECONDS = 10;

    private CouponBossBars() {
    }

    public static void update(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!CouponConfig.showTimedCouponBossBar()) {
            remove(serverPlayer);
            return;
        }

        ItemStack timedCoupon = CouponData.findBestActiveTimedCoupon(serverPlayer);
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

        bossBar.setName(CouponData.bossBarName(timedCoupon, coupon, serverPlayer.level()));
        bossBar.setColor(color(ModItems.couponRarity(coupon.effect(), coupon.mode())));
        bossBar.setProgress(CouponData.timedProgress(timedCoupon));
        tickSecondSound(serverPlayer, timedCoupon);
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
        playAppearSound(player);
        return bossBar;
    }

    private static void remove(ServerPlayer player) {
        ServerBossEvent bossBar = BOSS_BARS.remove(player.getUUID());
        if (bossBar != null) {
            bossBar.removePlayer(player);
            LAST_SECONDS.remove(player.getUUID());
            playDisappearSound(player);
        }
    }

    private static void tickSecondSound(ServerPlayer player, ItemStack timedCoupon) {
        if (!(timedCoupon.getItem() instanceof CouponItem coupon)) {
            return;
        }

        int seconds = CouponData.secondsRemaining(timedCoupon, coupon, player.level());
        Integer previousSeconds = LAST_SECONDS.put(player.getUUID(), seconds);
        if (previousSeconds != null && seconds < previousSeconds && seconds <= TICK_SOUND_START_SECONDS) {
            playSecondSound(player);
        }
    }

    private static void playAppearSound(ServerPlayer player) {
        playSound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.28F, 1.15F);
    }

    private static void playSecondSound(ServerPlayer player) {
        playSound(player, SoundEvents.ITEM_PICKUP, 0.04F, 1.65F);
    }

    private static void playDisappearSound(ServerPlayer player) {
        playSound(player, SoundEvents.BEACON_DEACTIVATE, 0.25F, 1.4F);
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static BossEvent.BossBarColor color(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> BossEvent.BossBarColor.WHITE;
            case UNCOMMON -> BossEvent.BossBarColor.YELLOW;
            case RARE -> BossEvent.BossBarColor.BLUE;
            case EPIC -> BossEvent.BossBarColor.PURPLE;
        };
    }
}
