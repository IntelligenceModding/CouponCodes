package de.doomedartemis.couponcodes.common.coupon;

import de.doomedartemis.couponcodes.CouponCodes;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

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

    private final Map<UUID, List<ItemStack>> activeCoupons = new HashMap<>();

    public static ActiveTimedCouponSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ActiveTimedCouponSavedData::new, ActiveTimedCouponSavedData::load),
                DATA_NAME
        );
    }

    private static ActiveTimedCouponSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ActiveTimedCouponSavedData data = new ActiveTimedCouponSavedData();
        ListTag players = tag.getList(PLAYERS_KEY, Tag.TAG_COMPOUND);
        for (Tag playerTag : players) {
            if (!(playerTag instanceof CompoundTag playerData) || !playerData.hasUUID(PLAYER_KEY)) {
                continue;
            }

            List<ItemStack> coupons = new ArrayList<>();
            ListTag couponTags = playerData.getList(COUPONS_KEY, Tag.TAG_COMPOUND);
            for (Tag couponTag : couponTags) {
                ItemStack.parse(registries, couponTag).ifPresent(stack -> {
                    if (!stack.isEmpty()) {
                        coupons.add(stack);
                    }
                });
            }

            if (!coupons.isEmpty()) {
                data.activeCoupons.put(playerData.getUUID(PLAYER_KEY), coupons);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, List<ItemStack>> entry : activeCoupons.entrySet()) {
            ListTag coupons = new ListTag();
            for (ItemStack stack : entry.getValue()) {
                if (!stack.isEmpty()) {
                    coupons.add(stack.saveOptional(registries));
                }
            }

            if (!coupons.isEmpty()) {
                CompoundTag playerData = new CompoundTag();
                playerData.putUUID(PLAYER_KEY, entry.getKey());
                playerData.put(COUPONS_KEY, coupons);
                players.add(playerData);
            }
        }

        tag.put(PLAYERS_KEY, players);
        return tag;
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

}
