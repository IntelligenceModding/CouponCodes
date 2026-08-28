package de.doomedartemis.couponcodes.common.coupon;

import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CouponFeedback {
    private static final Map<UUID, Long> LAST_USE_FEEDBACK = new HashMap<>();

    private CouponFeedback() {
    }

    public static void playActivation(Player player, CouponItem coupon) {
        if (!CouponConfig.playActivationFeedback() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Rarity rarity = ModItems.couponRarity(coupon.effect(), coupon.mode());
        spawnGlintBurst(serverPlayer, rarity, CouponConfig.activationParticleCount(), 0.75F, 0.12D, 0.32D);
        serverPlayer.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.26F, pitch(rarity, 1.0F));
    }

    public static void playUse(Player player, CouponItem coupon) {
        if (!CouponConfig.playUseFeedback() || !(player instanceof ServerPlayer serverPlayer) || isUseFeedbackCoolingDown(serverPlayer)) {
            return;
        }

        Rarity rarity = ModItems.couponRarity(coupon.effect(), coupon.mode());
        spawnGlintBurst(serverPlayer, rarity, CouponConfig.useParticleCount(), 0.55F, 0.08D, 0.22D);
        serverPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.10F, pitch(rarity, 1.15F));
    }

    private static boolean isUseFeedbackCoolingDown(ServerPlayer player) {
        int cooldownTicks = CouponConfig.useFeedbackCooldownTicks();
        if (cooldownTicks <= 0) {
            return false;
        }

        long gameTime = player.serverLevel().getGameTime();
        Long lastFeedbackTime = LAST_USE_FEEDBACK.get(player.getUUID());
        if (lastFeedbackTime != null && gameTime - lastFeedbackTime < cooldownTicks) {
            return true;
        }

        LAST_USE_FEEDBACK.put(player.getUUID(), gameTime);
        return false;
    }

    private static void spawnGlintBurst(ServerPlayer player, Rarity rarity, int count, float scale, double baseOutwardSpeed, double baseUpwardSpeed) {
        if (count <= 0) {
            return;
        }

        int rarityColor = color(rarity);
        DustParticleOptions dust = new DustParticleOptions(rarityColor, scale);
        DustColorTransitionOptions glintDust = new DustColorTransitionOptions(rarityColor, 0xFFFFFF, scale * 0.85F);
        double originY = player.getY() + 0.1D;

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i / count) + player.getRandom().nextDouble() * 0.35D;
            double ring = i / (double) count;
            double radius = 0.16D + ring * 0.36D + player.getRandom().nextDouble() * 0.08D;
            double outwardSpeed = baseOutwardSpeed + player.getRandom().nextDouble() * 0.08D;
            double upwardSpeed = baseUpwardSpeed + ring * 0.22D + player.getRandom().nextDouble() * 0.14D;
            double xDirection = Math.cos(angle);
            double zDirection = Math.sin(angle);
            double x = player.getX() + xDirection * radius;
            double y = originY + ring * 0.75D + player.getRandom().nextDouble() * 0.18D;
            double z = player.getZ() + zDirection * radius;
            double xSpeed = xDirection * outwardSpeed;
            double zSpeed = zDirection * outwardSpeed;

            player.serverLevel().sendParticles(
                    player,
                    dust,
                    false,
                    false,
                    x,
                    y,
                    z,
                    0,
                    xSpeed,
                    upwardSpeed,
                    zSpeed,
                    1.0D
            );

            if (i % 2 == 0) {
                player.serverLevel().sendParticles(
                        player,
                        ParticleTypes.ENCHANT,
                        false,
                        false,
                        x,
                        y + 0.08D,
                        z,
                        0,
                        xSpeed * 0.65D,
                        upwardSpeed * 0.75D,
                        zSpeed * 0.65D,
                        1.0D
                );
            } else {
                player.serverLevel().sendParticles(
                        player,
                        glintDust,
                        false,
                        false,
                        x,
                        y + 0.04D,
                        z,
                        0,
                        xSpeed * 0.85D,
                        upwardSpeed * 0.9D,
                        zSpeed * 0.85D,
                        1.0D
                );
            }
        }
    }

    private static int color(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFFFFFF;
            case UNCOMMON -> 0xFFFF55;
            case RARE -> 0x55FFFF;
            case EPIC -> 0xFF55FF;
        };
    }

    private static float pitch(Rarity rarity, float basePitch) {
        return switch (rarity) {
            case COMMON -> basePitch;
            case UNCOMMON -> basePitch + 0.08F;
            case RARE -> basePitch + 0.16F;
            case EPIC -> basePitch + 0.24F;
        };
    }
}
