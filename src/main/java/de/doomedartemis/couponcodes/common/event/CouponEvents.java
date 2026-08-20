package de.doomedartemis.couponcodes.common.event;

import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.loot.CouponLootDataManager;
import de.doomedartemis.couponcodes.common.advancement.CouponCriteria;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final Map<UUID, Float> LAST_EXHAUSTION = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> DEATH_DROP_RETURNS = new HashMap<>();

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
        reduceNewFoodExhaustion(player);
        extendPotionEffects(player);
        for (Item template : SMITHING_TEMPLATES) {
            refundTrackedInventoryCost(player, CouponEffectType.SMITHING_TEMPLATE, template);
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

        CouponData.CarriedCoupon xpCoupon = findBestCoupon(player, CouponEffectType.ANVIL_EXPERIENCE);
        if (xpCoupon != null && event.getCost() > 0) {
            int cost = (int) event.getCost();
            event.setCost(discountedCountWithMinimum(cost, discountPercent(player, xpCoupon), CouponConfig.anvilMinimumExperienceCost()));
        }

        CouponData.CarriedCoupon materialCoupon = findBestCoupon(player, CouponEffectType.REPAIR_MATERIAL);
        if (materialCoupon != null && event.getMaterialCost() > 0) {
            int materialCost = event.getMaterialCost();
            event.setMaterialCost(discountedCountWithMinimum(materialCost, discountPercent(player, materialCoupon), CouponConfig.anvilMinimumMaterialCost()));
        }
    }

    public static void onAnvilRepair(AnvilRepairEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        consumeBestCoupon(player, CouponEffectType.ANVIL_EXPERIENCE);
        if (!event.getRight().isEmpty()) {
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

    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !event.hasAmmo()) {
            return;
        }

        refundConsumableCoupon(player, CouponEffectType.ARROW, firstMatching(player, Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW));
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.is(Items.ENDER_PEARL)) {
            refundConsumableCoupon(event.getEntity(), CouponEffectType.ENDER_PEARL, stack.copyWithCount(1));
        } else if (stack.is(Items.FIREWORK_ROCKET)) {
            refundConsumableCoupon(event.getEntity(), CouponEffectType.ROCKET, stack.copyWithCount(1));
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

    private static void reduceNewDurabilityDamage(Player player) {
        Inventory inventory = player.getInventory();
        Map<String, Integer> trackedCounts = TRACKED_COUNTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());

        trackAndDiscountContainer(player, trackedCounts, "main", inventory.items);
        trackAndDiscountContainer(player, trackedCounts, "armor", inventory.armor);
        trackAndDiscountContainer(player, trackedCounts, "offhand", inventory.offhand);
    }

    private static void reduceNewElytraGlideDamage(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Map<String, Integer> trackedCounts = TRACKED_COUNTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        String key = "elytra_glide:chest";

        if (chest.isEmpty() || !chest.is(Items.ELYTRA) || !chest.isDamageableItem()) {
            trackedCounts.remove(key);
            return;
        }

        int currentDamage = chest.getDamageValue();
        int lastDamage = trackedCounts.getOrDefault(key, currentDamage);
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

        trackedCounts.put(key, currentDamage);
    }

    private static void trackAndDiscountContainer(Player player, Map<String, Integer> trackedCounts, String prefix, Iterable<ItemStack> stacks) {
        int slot = 0;
        for (ItemStack stack : stacks) {
            String key = "damage:" + prefix + ":" + slot;
            slot++;

            if (stack.isEmpty() || !stack.isDamageableItem()) {
                trackedCounts.remove(key);
                continue;
            }

            int currentDamage = stack.getDamageValue();
            int lastDamage = trackedCounts.getOrDefault(key, currentDamage);
            int newDamage = currentDamage - lastDamage;

            if (newDamage > 0) {
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

            trackedCounts.put(key, currentDamage);
        }
    }

    private static void reduceNewFoodExhaustion(Player player) {
        float currentExhaustion = player.getFoodData().getExhaustionLevel();
        float lastExhaustion = LAST_EXHAUSTION.getOrDefault(player.getUUID(), currentExhaustion);
        float addedExhaustion = currentExhaustion - lastExhaustion;

        if (addedExhaustion > 0.0F) {
            CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.FOOD);
            if (coupon != null) {
                float prevented = addedExhaustion * discountPercent(player, coupon) / 100.0F;
                if (prevented > 0.0F) {
                    currentExhaustion = Math.max(0.0F, currentExhaustion - prevented);
                    player.getFoodData().setExhaustion(currentExhaustion);
                    coupon.consumeUse(player);
                }
            }
        }

        LAST_EXHAUSTION.put(player.getUUID(), currentExhaustion);
    }

    private static void extendPotionEffects(Player player) {
        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.POTION_DURATION);
        if (coupon == null) {
            return;
        }

        List<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
        boolean extended = false;
        for (MobEffectInstance effect : effects) {
            if (!effect.isInfiniteDuration() && roll(player, discountPercent(player, coupon))) {
                Holder<MobEffect> effectType = effect.getEffect();
                player.addEffect(new MobEffectInstance(
                        effectType,
                        effect.getDuration() + CouponConfig.potionDurationExtensionTicks(),
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                ));
                extended = true;
            }
        }

        if (extended) {
            coupon.consumeUse(player);
        }
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

    private static ItemStack firstMatching(Player player, Item... items) {
        for (ItemStack stack : player.getInventory().items) {
            for (Item item : items) {
                if (stack.is(item)) {
                    return stack.copyWithCount(1);
                }
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            for (Item item : items) {
                if (stack.is(item)) {
                    return stack.copyWithCount(1);
                }
            }
        }
        return ItemStack.EMPTY;
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
