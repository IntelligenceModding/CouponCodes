package de.doomedartemis.couponcodes.common.coupon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ActiveTimedCouponSavedData extends SavedData {
    private static final String DATA_NAME = CouponCodes.MOD_ID + "_active_timed_coupons";
    private static final String PLAYERS_KEY = "Players";
    private static final String PLAYER_KEY = "Player";
    private static final String COUPONS_KEY = "Coupons";
    private static final Codec<SavedPlayerCoupons> SAVED_PLAYER_COUPONS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf(PLAYER_KEY).forGetter(SavedPlayerCoupons::player),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf(COUPONS_KEY).forGetter(SavedPlayerCoupons::coupons)
    ).apply(instance, SavedPlayerCoupons::new));
    private static final Codec<ActiveTimedCouponSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SAVED_PLAYER_COUPONS_CODEC.listOf().optionalFieldOf(PLAYERS_KEY, List.of()).forGetter(ActiveTimedCouponSavedData::savedPlayers)
    ).apply(instance, ActiveTimedCouponSavedData::new));
    private static final SavedDataType<ActiveTimedCouponSavedData> TYPE = new SavedDataType<>(
            DATA_NAME,
            ActiveTimedCouponSavedData::new,
            CODEC
    );

    private final Map<UUID, List<ItemStack>> activeCoupons = new HashMap<>();

    public static ActiveTimedCouponSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ActiveTimedCouponSavedData() {
    }

    private ActiveTimedCouponSavedData(List<SavedPlayerCoupons> savedPlayers) {
        for (SavedPlayerCoupons savedPlayer : savedPlayers) {
            List<ItemStack> coupons = savedPlayer.coupons().stream()
                    .filter(stack -> !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
            if (!coupons.isEmpty()) {
                this.activeCoupons.put(savedPlayer.player(), new ArrayList<>(coupons));
            }
        }
    }

    private List<SavedPlayerCoupons> savedPlayers() {
        List<SavedPlayerCoupons> players = new ArrayList<>();
        for (Map.Entry<UUID, List<ItemStack>> entry : activeCoupons.entrySet()) {
            List<ItemStack> coupons = new ArrayList<>();
            for (ItemStack stack : entry.getValue()) {
                if (!stack.isEmpty()) {
                    coupons.add(stack.copy());
                }
            }

            if (!coupons.isEmpty()) {
                players.add(new SavedPlayerCoupons(entry.getKey(), coupons));
            }
        }
        return players;
    }

    public List<ItemStack> activeCoupons(UUID playerId) {
        return activeCoupons.computeIfAbsent(playerId, ignored -> new ArrayList<>());
    }

    public List<ItemStack> activeCouponsOrEmpty(UUID playerId) {
        return activeCoupons.getOrDefault(playerId, List.of());
    }

    public int clear(UUID playerId) {
        List<ItemStack> removed = activeCoupons.remove(playerId);
        if (removed == null) {
            return 0;
        }
        setDirty();
        return removed.size();
    }

    public int clear(UUID playerId, CouponEffectType effect) {
        return removeMatching(playerId, stack -> stack.getItem() instanceof CouponItem coupon && coupon.effect() == effect);
    }

    public int clear(UUID playerId, CouponCategory category) {
        return removeMatching(playerId, stack -> stack.getItem() instanceof CouponItem coupon && coupon.effect().category() == category);
    }

    private int removeMatching(UUID playerId, java.util.function.Predicate<ItemStack> predicate) {
        List<ItemStack> coupons = activeCoupons.get(playerId);
        if (coupons == null) {
            return 0;
        }

        int cleared = 0;
        Iterator<ItemStack> iterator = coupons.iterator();
        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                iterator.remove();
                cleared++;
            }
        }

        if (coupons.isEmpty()) {
            activeCoupons.remove(playerId);
        }
        if (cleared > 0) {
            setDirty();
        }
        return cleared;
    }

    private record SavedPlayerCoupons(UUID player, List<ItemStack> coupons) {
    }
}
