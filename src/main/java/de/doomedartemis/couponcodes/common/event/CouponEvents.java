package de.doomedartemis.couponcodes.common.event;

import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.loot.CouponLootDataManager;
import de.doomedartemis.couponcodes.common.advancement.CouponCriteria;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.inventory.SmithingMenu;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CouponEvents {
    private static final Item[] SMITHING_TEMPLATES = {
            Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
            Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE
    };

    private static final Map<UUID, Map<String, Integer>> TRACKED_COUNTS = new HashMap<>();
    private static final Map<UUID, Map<String, ItemStack>> TRACKED_DAMAGE_STACKS = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> DEATH_DROP_RETURNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ARROW_REFUND_TICK = new HashMap<>();
    private static final Set<UUID> PENDING_ANVIL_XP_DISCOUNTS = new HashSet<>();
    private static final Set<UUID> PENDING_ANVIL_MATERIAL_DISCOUNTS = new HashSet<>();

    private CouponEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (!CouponConfig.areCouponsEnabled()) {
            CouponBossBars.update(player);
            return;
        }

        CouponData.tickCouponsInInventory(player);
        reduceNewElytraGlideDamage(player);
        reduceNewDurabilityDamage(player);
        if (player.containerMenu instanceof SmithingMenu) {
            for (Item template : SMITHING_TEMPLATES) {
                refundTrackedInventoryCost(player, CouponEffectType.SMITHING_TEMPLATE, template);
            }
        }
        CouponBossBars.update(player);
    }

    public static void onPlayerEnchantItem(PlayerEnchantItemEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.ENCHANTING_EXPERIENCE);
        if (coupon == null) {
            return;
        }

        int refundLevels = Math.max(1, Math.round(discountPercent(player, coupon) / (float) CouponConfig.enchantingPercentPerRefundLevel()));
        player.giveExperienceLevels(refundLevels);
        coupon.consumeUse(player);
    }

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }

        PENDING_ANVIL_XP_DISCOUNTS.remove(player.getUUID());
        PENDING_ANVIL_MATERIAL_DISCOUNTS.remove(player.getUUID());

        CouponData.CarriedCoupon xpCoupon = findBestCoupon(player, CouponEffectType.ANVIL_EXPERIENCE);
        if (xpCoupon != null && event.getCost() > 0) {
            int cost = (int) event.getCost();
            event.setCost(discountedCountWithMinimum(cost, discountPercent(player, xpCoupon), CouponConfig.anvilMinimumExperienceCost()));
            PENDING_ANVIL_XP_DISCOUNTS.add(player.getUUID());
        }

        CouponData.CarriedCoupon materialCoupon = findBestCoupon(player, CouponEffectType.REPAIR_MATERIAL);
        if (materialCoupon != null && event.getMaterialCost() > 0) {
            int materialCost = event.getMaterialCost();
            event.setMaterialCost(discountedCountWithMinimum(materialCost, discountPercent(player, materialCoupon), CouponConfig.anvilMinimumMaterialCost()));
            PENDING_ANVIL_MATERIAL_DISCOUNTS.add(player.getUUID());
        }
    }

    public static void onAnvilRepair(AnvilRepairEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (PENDING_ANVIL_XP_DISCOUNTS.remove(player.getUUID())) {
            consumeBestCoupon(player, CouponEffectType.ANVIL_EXPERIENCE);
        }
        if (PENDING_ANVIL_MATERIAL_DISCOUNTS.remove(player.getUUID())) {
            consumeBestCoupon(player, CouponEffectType.REPAIR_MATERIAL);
        }
        applyToolRepairBonus(player, event.getLeft(), event.getOutput());
    }

    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.VILLAGER_TRADE);
        if (coupon != null) {
            int discount = discountPercent(player, coupon);
            refundDiscountedCost(player, event.getMerchantOffer().getCostA(), discount);
            refundDiscountedCost(player, event.getMerchantOffer().getCostB(), discount);
            coupon.consumeUse(player);
        }

        CouponData.CarriedCoupon restockCoupon = findBestCoupon(player, CouponEffectType.VILLAGER_RESTOCK);
        if (restockCoupon != null) {
            boolean restock = roll(player, discountPercent(player, restockCoupon));
            if (restock || CouponConfig.consumeChanceCouponsOnFailedRoll()) {
                restockCoupon.consumeUse(player);
            }
            if (restock) {
                event.getMerchantOffer().resetUses();
            }
        }
    }

    public static void onGrindstoneTakeItem(GrindstoneEvent.OnTakeItem event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }

        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();
        if (top.isEmpty() || bottom.isEmpty() || !top.isDamageableItem() || !bottom.isDamageableItem() || !top.is(bottom.getItem())) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.TOOL_REPAIR);
        if (coupon == null) {
            return;
        }

        boolean refund = roll(player, discountPercent(player, coupon));
        if (refund || CouponConfig.consumeChanceCouponsOnFailedRoll()) {
            coupon.consumeUse(player);
        }
        if (refund) {
            event.setNewBottomItem(bottom.copyWithCount(1));
        }
    }

    public static void onPlayerBrewedPotion(PlayerBrewedPotionEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.BREWING_INGREDIENT);
        if (coupon == null) {
            return;
        }

        int discount = discountPercent(player, coupon);
        boolean refund = roll(player, discount);
        if (refund || CouponConfig.consumeChanceCouponsOnFailedRoll()) {
            coupon.consumeUse(player);
        }
        if (refund) {
            giveOrDrop(player, event.getStack().copyWithCount(1));
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof AbstractArrow arrow && arrow.getOwner() instanceof Player player) {
            refundArrowCoupon(player, arrow);
        } else if (entity instanceof ThrownEnderpearl pearl && pearl.getOwner() instanceof Player player) {
            refundConsumableCoupon(player, CouponEffectType.ENDER_PEARL, new ItemStack(Items.ENDER_PEARL));
        } else if (entity instanceof FireworkRocketEntity rocket && rocket.getOwner() instanceof Player player) {
            refundConsumableCoupon(player, CouponEffectType.ROCKET, rocket.getItem().copyWithCount(1));
        }
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        ItemStack used = event.getItem();
        if (used.has(DataComponents.FOOD)) {
            refundConsumableCoupon(player, CouponEffectType.FOOD, used.copyWithCount(1));
        }

        PotionContents potionContents = used.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null && potionContents.hasEffects()) {
            extendPotionEffectsFromUse(player, potionContents);
        }
    }

    public static void onBonemeal(BonemealEvent event) {
        if (!event.getLevel().isClientSide() && event.isValidBonemealTarget()) {
            refundConsumableCoupon(event.getPlayer(), CouponEffectType.BONE_MEAL, event.getStack().copyWithCount(1));
        }
    }

    public static void onUseTotem(LivingUseTotemEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            refundConsumableCoupon(player, CouponEffectType.TOTEM, event.getTotem().copyWithCount(1));
        }
    }

    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.FISHING);
        if (coupon == null || event.getRodDamage() <= 0) {
            return;
        }

        int preventedDamage = preventedAmount(player, event.getRodDamage(), discountPercent(player, coupon));
        if (preventedDamage > 0) {
            event.damageRodBy(Math.max(0, event.getRodDamage() - preventedDamage));
        }
        if (preventedDamage > 0 || CouponConfig.consumeDurabilityCouponsOnFailedRoll()) {
            coupon.consumeUse(player);
        }
    }

    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.MENDING);
        if (coupon == null) {
            return;
        }

        ExperienceOrb orb = event.getOrb();
        Optional<EnchantedItemInUse> selected = EnchantmentHelper.getRandomItemWith(
                EnchantmentEffectComponents.REPAIR_WITH_XP,
                player,
                ItemStack::isDamaged
        );
        if (selected.isEmpty()) {
            return;
        }

        ItemStack stack = selected.get().itemStack();
        int vanillaRepair = mendingRepairAmount(serverPlayer.serverLevel(), stack, orb.getValue());
        if (vanillaRepair <= 0) {
            return;
        }

        int bonusRepair = Math.min(stack.getDamageValue(), Math.max(1, Math.round(vanillaRepair * discountPercent(player, coupon) / 100.0F)));
        if (bonusRepair > 0) {
            stack.setDamageValue(stack.getDamageValue() - bonusRepair);
            coupon.consumeUse(player);
        }
    }

    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.FALL_DAMAGE);
        if (coupon == null) {
            return;
        }

        event.setDamageMultiplier(event.getDamageMultiplier() * (1.0F - discountPercent(player, coupon) / 100.0F));
        coupon.consumeUse(player);
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        addEnemyCouponDrop(event);

        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.DEATH_DROP);
        if (coupon == null) {
            return;
        }

        List<ItemStack> returned = preventDroppedItems(event.getDrops(), discountPercent(player, coupon), player);
        if (!returned.isEmpty()) {
            DEATH_DROP_RETURNS.put(player.getUUID(), returned);
            coupon.consumeUse(player);
        }
    }

    private static void addEnemyCouponDrop(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !CouponConfig.areCouponsEnabled()) {
            return;
        }

        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityType == null) {
            return;
        }

        for (ItemStack stack : CouponLootDataManager.generate(CouponLootDataManager.entityProfile(entityType), entity.getRandom())) {
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
            if (event.getSource().getEntity() instanceof ServerPlayer serverPlayer && stack.getItem() instanceof CouponItem coupon) {
                CouponCriteria.triggerEntityCouponDropped(
                        serverPlayer,
                        entityType,
                        coupon.effect(),
                        coupon.mode()
                );
            }
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        List<ItemStack> returned = DEATH_DROP_RETURNS.remove(event.getEntity().getUUID());
        if (returned == null || event.getEntity().level().isClientSide()) {
            return;
        }

        for (ItemStack stack : returned) {
            giveOrDrop(event.getEntity(), stack);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        TRACKED_COUNTS.remove(playerId);
        TRACKED_DAMAGE_STACKS.remove(playerId);
        DEATH_DROP_RETURNS.remove(playerId);
        LAST_ARROW_REFUND_TICK.remove(playerId);
        PENDING_ANVIL_XP_DISCOUNTS.remove(playerId);
        PENDING_ANVIL_MATERIAL_DISCOUNTS.remove(playerId);
    }

    private static void reduceNewDurabilityDamage(Player player) {
        Inventory inventory = player.getInventory();
        Map<String, ItemStack> trackedStacks = TRACKED_DAMAGE_STACKS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());

        trackAndDiscountContainer(player, trackedStacks, "main", inventory.items);
        trackAndDiscountContainer(player, trackedStacks, "armor", inventory.armor);
        trackAndDiscountContainer(player, trackedStacks, "offhand", inventory.offhand);
    }

    private static void reduceNewElytraGlideDamage(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Map<String, ItemStack> trackedStacks = TRACKED_DAMAGE_STACKS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        String key = "elytra_glide:chest";

        if (chest.isEmpty() || !chest.is(Items.ELYTRA) || !chest.isDamageableItem()) {
            trackedStacks.remove(key);
            return;
        }

        int currentDamage = chest.getDamageValue();
        int lastDamage = lastTrackedDamage(trackedStacks, key, chest);
        int newDamage = currentDamage - lastDamage;
        if (newDamage > 0 && player.isFallFlying()) {
            CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.ELYTRA_GLIDE);
            if (coupon != null) {
                int preventedDamage = preventedAmount(player, newDamage, discountPercent(player, coupon));
                if (preventedDamage > 0) {
                    chest.setDamageValue(Math.max(0, currentDamage - preventedDamage));
                }
                if (preventedDamage > 0 || CouponConfig.consumeDurabilityCouponsOnFailedRoll()) {
                    coupon.consumeUse(player);
                }
                currentDamage = chest.getDamageValue();
            }
        }

        trackDamageStack(trackedStacks, key, chest);
    }

    private static void trackAndDiscountContainer(Player player, Map<String, ItemStack> trackedStacks, String prefix, Iterable<ItemStack> stacks) {
        int slot = 0;
        for (ItemStack stack : stacks) {
            String key = "damage:" + prefix + ":" + slot;
            slot++;

            if (stack.isEmpty() || !stack.isDamageableItem()) {
                trackedStacks.remove(key);
                continue;
            }

            int currentDamage = stack.getDamageValue();
            int lastDamage = lastTrackedDamage(trackedStacks, key, stack);
            int newDamage = currentDamage - lastDamage;

            if (newDamage > 0 && !(stack.is(Items.ELYTRA) && player.isFallFlying())) {
                CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.DURABILITY);
                if (coupon != null) {
                    int preventedDamage = preventedAmount(player, newDamage, discountPercent(player, coupon));
                    if (preventedDamage > 0) {
                        stack.setDamageValue(Math.max(0, currentDamage - preventedDamage));
                    }
                    if (preventedDamage > 0 || CouponConfig.consumeDurabilityCouponsOnFailedRoll()) {
                        coupon.consumeUse(player);
                    }
                    currentDamage = stack.getDamageValue();
                }
            }

            trackDamageStack(trackedStacks, key, stack);
        }
    }

    private static void extendPotionEffectsFromUse(Player player, PotionContents potionContents) {
        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.POTION_DURATION);
        if (coupon == null) {
            return;
        }

        boolean extended = false;
        int discount = discountPercent(player, coupon);
        for (MobEffectInstance appliedEffect : potionContents.getAllEffects()) {
            if (!appliedEffect.isInfiniteDuration()) {
                MobEffectInstance currentEffect = player.getEffect(appliedEffect.getEffect());
                if (currentEffect == null || currentEffect.isInfiniteDuration()) {
                    continue;
                }

                int extraDuration = Math.max(
                        CouponConfig.potionDurationExtensionTicks(),
                        Math.round(appliedEffect.getDuration() * discount / 100.0F)
                );
                Holder<MobEffect> effectType = currentEffect.getEffect();
                player.addEffect(new MobEffectInstance(
                        effectType,
                        currentEffect.getDuration() + extraDuration,
                        currentEffect.getAmplifier(),
                        currentEffect.isAmbient(),
                        currentEffect.isVisible(),
                        currentEffect.showIcon()
                ));
                extended = true;
            }
        }

        if (extended) {
            coupon.consumeUse(player);
        }
    }

    private static int lastTrackedDamage(Map<String, ItemStack> trackedStacks, String key, ItemStack stack) {
        ItemStack trackedStack = trackedStacks.get(key);
        if (trackedStack == null || !sameStackExceptDamage(trackedStack, stack)) {
            return stack.getDamageValue();
        }
        return trackedStack.getDamageValue();
    }

    private static void trackDamageStack(Map<String, ItemStack> trackedStacks, String key, ItemStack stack) {
        trackedStacks.put(key, stack.copyWithCount(1));
    }

    private static boolean sameStackExceptDamage(ItemStack previous, ItemStack current) {
        if (!previous.is(current.getItem())) {
            return false;
        }

        ItemStack previousComparable = previous.copyWithCount(1);
        ItemStack currentComparable = current.copyWithCount(1);
        if (previousComparable.isDamageableItem()) {
            previousComparable.setDamageValue(0);
        }
        if (currentComparable.isDamageableItem()) {
            currentComparable.setDamageValue(0);
        }
        return ItemStack.isSameItemSameComponents(previousComparable, currentComparable);
    }

    private static CouponData.CarriedCoupon findBestCoupon(Player player, CouponEffectType effect) {
        return CouponData.findBestCarriedCoupon(player, effect);
    }

    private static CouponData.CarriedCoupon consumeBestCoupon(Player player, CouponEffectType effect) {
        CouponData.CarriedCoupon coupon = findBestCoupon(player, effect);
        if (coupon != null) {
            coupon.consumeUse(player);
        }
        return coupon;
    }

    private static int discountPercent(Player player, CouponData.CarriedCoupon coupon) {
        return CouponData.discountPercent(coupon.stack(), coupon.coupon(), player.level());
    }

    private static void refundConsumableCoupon(Player player, CouponEffectType effect, ItemStack refund) {
        if (refund.isEmpty()) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, effect);
        if (coupon == null) {
            return;
        }

        int discount = discountPercent(player, coupon);
        boolean refundRoll = roll(player, discount);
        if (refundRoll || CouponConfig.consumeChanceCouponsOnFailedRoll()) {
            coupon.consumeUse(player);
        }
        if (refundRoll) {
            giveOrDrop(player, refund.copyWithCount(1));
        }
    }

    private static void refundArrowCoupon(Player player, AbstractArrow arrow) {
        if (arrow.pickup != AbstractArrow.Pickup.ALLOWED) {
            return;
        }

        long gameTime = player.level().getGameTime();
        Long lastRefundTick = LAST_ARROW_REFUND_TICK.get(player.getUUID());
        if (lastRefundTick != null && lastRefundTick == gameTime) {
            return;
        }

        ItemStack refund = arrow.getPickupItemStackOrigin();
        if (refund.isEmpty()) {
            refund = new ItemStack(Items.ARROW);
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.ARROW);
        if (coupon == null) {
            return;
        }

        LAST_ARROW_REFUND_TICK.put(player.getUUID(), gameTime);
        int discount = discountPercent(player, coupon);
        boolean refundRoll = roll(player, discount);
        if (refundRoll || CouponConfig.consumeChanceCouponsOnFailedRoll()) {
            coupon.consumeUse(player);
        }
        if (refundRoll) {
            giveOrDrop(player, refund.copyWithCount(1));
        }
    }

    private static void refundDiscountedCost(Player player, ItemStack cost, int discountPercent) {
        if (cost.isEmpty()) {
            return;
        }

        int refundCount = cost.getCount() - discountedCount(cost.getCount(), discountPercent);
        if (refundCount > 0) {
            giveOrDrop(player, cost.copyWithCount(refundCount));
        }
    }

    private static int discountedCount(int count, int discountPercent) {
        return Math.max(0, count - Math.round(count * discountPercent / 100.0F));
    }

    private static int discountedCountWithMinimum(int count, int discountPercent, int minimum) {
        return Math.min(count, Math.max(minimum, discountedCount(count, discountPercent)));
    }

    private static void applyToolRepairBonus(Player player, ItemStack input, ItemStack output) {
        if (input.isEmpty() || output.isEmpty() || !input.isDamageableItem() || !output.isDamageableItem()) {
            return;
        }

        int repairedDamage = input.getDamageValue() - output.getDamageValue();
        if (repairedDamage <= 0) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.TOOL_REPAIR);
        if (coupon == null) {
            return;
        }

        int bonusRepair = Math.min(output.getDamageValue(), Math.max(1, Math.round(repairedDamage * discountPercent(player, coupon) / 100.0F)));
        if (bonusRepair > 0) {
            output.setDamageValue(output.getDamageValue() - bonusRepair);
            coupon.consumeUse(player);
        }
    }

    private static int mendingRepairAmount(ServerLevel level, ItemStack stack, int xpValue) {
        return Math.min(
                EnchantmentHelper.modifyDurabilityToRepairFromXp(level, stack, (int) (xpValue * stack.getXpRepairRatio())),
                stack.getDamageValue()
        );
    }

    private static int preventedAmount(Player player, int amount, int discountPercent) {
        int prevented = 0;
        for (int i = 0; i < amount; i++) {
            if (roll(player, discountPercent)) {
                prevented++;
            }
        }
        return prevented;
    }

    private static boolean roll(Player player, int discountPercent) {
        return player.getRandom().nextInt(100) < discountPercent;
    }

    private static void refundTrackedInventoryCost(Player player, CouponEffectType effect, Item item) {
        Map<String, Integer> trackedCounts = TRACKED_COUNTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        String key = "count:" + effect.name() + ":" + Item.getId(item);
        int currentCount = countItems(player, item);
        int previousCount = trackedCounts.getOrDefault(key, currentCount);
        int consumed = previousCount - currentCount;

        if (consumed > 0) {
            CouponData.CarriedCoupon coupon = findBestCoupon(player, effect);
            if (coupon != null) {
                int refundCount = preventedAmount(player, consumed, discountPercent(player, coupon));
                if (refundCount > 0) {
                    giveOrDrop(player, new ItemStack(item, refundCount));
                }
                if (refundCount > 0 || CouponConfig.consumeChanceCouponsOnFailedRoll()) {
                    coupon.consumeUse(player);
                }
                currentCount = countItems(player, item);
            }
        }

        trackedCounts.put(key, currentCount);
    }

    private static int countItems(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static List<ItemStack> preventDroppedItems(Collection<ItemEntity> drops, int discountPercent, Player player) {
        List<ItemStack> returned = new ArrayList<>();
        Iterator<ItemEntity> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            ItemStack stack = itemEntity.getItem();
            int kept = preventedAmount(player, stack.getCount(), discountPercent);
            if (kept <= 0) {
                continue;
            }

            returned.add(stack.copyWithCount(kept));
            if (kept >= stack.getCount()) {
                iterator.remove();
                itemEntity.discard();
            } else {
                stack.shrink(kept);
            }
        }
        return returned;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
