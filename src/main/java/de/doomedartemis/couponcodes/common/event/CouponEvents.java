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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
import java.util.function.Consumer;

public final class CouponEvents {
    private static final ResourceLocation ENDER_DRAGON_ENTITY = ResourceLocation.parse("minecraft:ender_dragon");
    private static final int DRAGON_COUPON_DROP_DEATH_TIME = 199;
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

    private static final Map<UUID, SmithingState> SMITHING_STATES = new HashMap<>();
    private static final Map<UUID, Map<String, ItemStack>> TRACKED_DAMAGE_STACKS = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> DEATH_DROP_RETURNS = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> PENDING_ITEM_REFUNDS = new HashMap<>();
    private static final Map<UUID, UUID> PENDING_DRAGON_COUPON_DROP_KILLERS = new HashMap<>();
    private static final Map<UUID, AnvilDiscountState> ANVIL_DISCOUNTS = new HashMap<>();
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
        if (player.containerMenu instanceof AnvilMenu anvilMenu) {
            applyAnvilDiscounts(player, anvilMenu);
        } else {
            ANVIL_DISCOUNTS.remove(player.getUUID());
            PENDING_ANVIL_XP_DISCOUNTS.remove(player.getUUID());
            PENDING_ANVIL_MATERIAL_DISCOUNTS.remove(player.getUUID());
        }
        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            refundSmithingTemplate(player, smithingMenu);
        } else {
            SMITHING_STATES.remove(player.getUUID());
        }
        CouponBossBars.update(player);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_ITEM_REFUNDS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, List<ItemStack>>> iterator = PENDING_ITEM_REFUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, List<ItemStack>> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                for (ItemStack stack : entry.getValue()) {
                    giveOrDrop(player, stack);
                }
                syncInventory(player);
            }
            iterator.remove();
        }
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
                syncMerchantOffers(player, event);
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

    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EnderDragon dragon) || entity.level().isClientSide() || dragon.dragonDeathTime != DRAGON_COUPON_DROP_DEATH_TIME) {
            return;
        }

        UUID dragonId = dragon.getUUID();
        ServerPlayer killer = dragon.level() instanceof ServerLevel level
                ? playerById(level, PENDING_DRAGON_COUPON_DROP_KILLERS.remove(dragonId))
                : null;
        if (killer == null && dragon.getKillCredit() instanceof ServerPlayer killCredit) {
            killer = killCredit;
        }

        addGeneratedCouponDrops(dragon, ENDER_DRAGON_ENTITY, killer, drop -> dragon.level().addFreshEntity(drop));
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        ItemStack used = event.getItem();
        if (used.has(DataComponents.FOOD)) {
            refundConsumableCoupon(player, CouponEffectType.FOOD, used.copyWithCount(1), true);
        }

        PotionContents potionContents = used.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null && potionContents.hasEffects()) {
            extendPotionEffectsFromUse(player, potionContents);
        }
    }

    public static void onBonemeal(BonemealEvent event) {
        if (!event.getLevel().isClientSide() && event.isValidBonemealTarget()) {
            refundConsumableCoupon(event.getPlayer(), CouponEffectType.BONE_MEAL, event.getStack().copyWithCount(1), true);
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

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !event.getSource().is(DamageTypeTags.IS_FALL) || event.getAmount() <= 0.0F) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.FALL_DAMAGE);
        if (coupon == null) {
            return;
        }

        event.setAmount(event.getAmount() * (1.0F - discountPercent(player, coupon) / 100.0F));
        coupon.consumeUse(player);
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        addEnemyCouponDrop(event);

        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        DropCouponCandidate coupon = findBestDeathDropCoupon(player, event.getDrops());
        if (coupon == null) {
            return;
        }

        List<ItemStack> returned = preventDroppedItems(event.getDrops(), discountPercent(player, coupon.stack(), coupon.coupon()), player, coupon.entity());
        if (!returned.isEmpty()) {
            DEATH_DROP_RETURNS.put(player.getUUID(), returned);
            consumeDeathDropCoupon(player, event.getDrops(), coupon);
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

        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (entity instanceof EnderDragon) {
            if (killer != null) {
                PENDING_DRAGON_COUPON_DROP_KILLERS.put(entity.getUUID(), killer.getUUID());
            }
            return;
        }

        addGeneratedCouponDrops(entity, entityType, killer, event.getDrops()::add);
    }

    private static void addGeneratedCouponDrops(LivingEntity entity, ResourceLocation entityType, ServerPlayer killer, Consumer<ItemEntity> drops) {
        for (ItemStack stack : CouponLootDataManager.generate(CouponLootDataManager.entityProfile(entityType), entity.getRandom())) {
            drops.accept(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
            if (killer != null && stack.getItem() instanceof CouponItem coupon) {
                CouponCriteria.triggerEntityCouponDropped(killer, entityType, coupon.effect(), coupon.mode());
            }
        }
    }

    private static ServerPlayer playerById(ServerLevel level, UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(playerId);
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
        SMITHING_STATES.remove(playerId);
        TRACKED_DAMAGE_STACKS.remove(playerId);
        DEATH_DROP_RETURNS.remove(playerId);
        LAST_ARROW_REFUND_TICK.remove(playerId);
        PENDING_ANVIL_XP_DISCOUNTS.remove(playerId);
        PENDING_ANVIL_MATERIAL_DISCOUNTS.remove(playerId);
        PENDING_ITEM_REFUNDS.remove(playerId);
        ANVIL_DISCOUNTS.remove(playerId);
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

    private static void applyAnvilDiscounts(Player player, AnvilMenu menu) {
        if (menu.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty() || menu.getCost() <= 0) {
            ANVIL_DISCOUNTS.remove(player.getUUID());
            PENDING_ANVIL_XP_DISCOUNTS.remove(player.getUUID());
            PENDING_ANVIL_MATERIAL_DISCOUNTS.remove(player.getUUID());
            return;
        }

        String key = anvilKey(menu);
        int currentXpCost = menu.getCost();
        int currentMaterialCost = menu.repairItemCountCost;
        AnvilDiscountState previous = ANVIL_DISCOUNTS.get(player.getUUID());
        if (previous != null
                && previous.key().equals(key)
                && currentXpCost == previous.discountedXpCost()
                && currentMaterialCost == previous.discountedMaterialCost()) {
            return;
        }

        int baseXpCost = previous != null && previous.key().equals(key) && currentXpCost == previous.discountedXpCost()
                ? previous.baseXpCost()
                : currentXpCost;
        int baseMaterialCost = previous != null && previous.key().equals(key) && currentMaterialCost == previous.discountedMaterialCost()
                ? previous.baseMaterialCost()
                : currentMaterialCost;

        int discountedXpCost = baseXpCost;
        CouponData.CarriedCoupon xpCoupon = findBestCoupon(player, CouponEffectType.ANVIL_EXPERIENCE);
        if (xpCoupon != null && baseXpCost > CouponConfig.anvilMinimumExperienceCost()) {
            discountedXpCost = discountedCountWithMinimum(baseXpCost, discountPercent(player, xpCoupon), CouponConfig.anvilMinimumExperienceCost());
        }

        int discountedMaterialCost = baseMaterialCost;
        CouponData.CarriedCoupon materialCoupon = findBestCoupon(player, CouponEffectType.REPAIR_MATERIAL);
        if (materialCoupon != null && baseMaterialCost > CouponConfig.anvilMinimumMaterialCost()) {
            discountedMaterialCost = discountedCountWithMinimum(baseMaterialCost, discountPercent(player, materialCoupon), CouponConfig.anvilMinimumMaterialCost());
        }

        boolean xpApplied = discountedXpCost < baseXpCost;
        boolean materialApplied = discountedMaterialCost < baseMaterialCost;
        if (xpApplied) {
            PENDING_ANVIL_XP_DISCOUNTS.add(player.getUUID());
        } else {
            PENDING_ANVIL_XP_DISCOUNTS.remove(player.getUUID());
        }
        if (materialApplied) {
            PENDING_ANVIL_MATERIAL_DISCOUNTS.add(player.getUUID());
        } else {
            PENDING_ANVIL_MATERIAL_DISCOUNTS.remove(player.getUUID());
        }

        if (discountedXpCost != currentXpCost) {
            menu.setMaximumCost(discountedXpCost);
        }
        if (discountedMaterialCost != currentMaterialCost) {
            menu.repairItemCountCost = discountedMaterialCost;
        }
        if (discountedXpCost != currentXpCost || discountedMaterialCost != currentMaterialCost) {
            menu.broadcastChanges();
        }

        ANVIL_DISCOUNTS.put(player.getUUID(), new AnvilDiscountState(
                key,
                baseXpCost,
                discountedXpCost,
                baseMaterialCost,
                discountedMaterialCost
        ));
    }

    private static String anvilKey(AnvilMenu menu) {
        return stackKey(menu.getSlot(AnvilMenu.INPUT_SLOT).getItem())
                + "|"
                + stackKey(menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem())
                + "|"
                + stackKey(menu.getSlot(AnvilMenu.RESULT_SLOT).getItem());
    }

    private static String stackKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        return Item.getId(stack.getItem()) + ":" + stack.getCount() + ":" + ItemStack.hashItemAndComponents(stack);
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

    private static void syncMerchantOffers(Player player, TradeWithVillagerEvent event) {
        if (player instanceof ServerPlayer serverPlayer && player.containerMenu instanceof MerchantMenu menu) {
            int traderLevel = event.getAbstractVillager() instanceof Villager villager
                    ? villager.getVillagerData().getLevel()
                    : menu.getTraderLevel();
            serverPlayer.sendMerchantOffers(
                    menu.containerId,
                    event.getAbstractVillager().getOffers(),
                    traderLevel,
                    event.getAbstractVillager().getVillagerXp(),
                    event.getAbstractVillager().showProgressBar(),
                    event.getAbstractVillager().canRestock()
            );
            menu.broadcastChanges();
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
        refundConsumableCoupon(player, effect, refund, false);
    }

    private static void refundConsumableCoupon(Player player, CouponEffectType effect, ItemStack refund, boolean deferRefund) {
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
            if (deferRefund) {
                scheduleItemRefund(player, refund.copyWithCount(1));
            } else {
                giveOrDrop(player, refund.copyWithCount(1));
            }
        }
    }

    private static void scheduleItemRefund(Player player, ItemStack refund) {
        if (player instanceof ServerPlayer) {
            PENDING_ITEM_REFUNDS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(refund);
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

    private static void refundSmithingTemplate(Player player, SmithingMenu menu) {
        SmithingState current = SmithingState.capture(player, menu);
        SmithingState previous = SMITHING_STATES.put(player.getUUID(), current);
        if (previous == null || !previous.hadResult() || !isSmithingTemplate(previous.template())) {
            return;
        }
        if (countMatchingCarriedItems(player, previous.result()) <= previous.carriedResultCount()) {
            return;
        }

        int consumed = previous.consumedInputCount(current);
        if (consumed <= 0) {
            return;
        }

        CouponData.CarriedCoupon coupon = findBestCoupon(player, CouponEffectType.SMITHING_TEMPLATE);
        if (coupon == null) {
            return;
        }

        int refundCount = preventedAmount(player, consumed, discountPercent(player, coupon));
        if (refundCount > 0) {
            refundIntoSmithingTemplateSlot(player, menu, previous.template().copyWithCount(refundCount));
        }
        if (refundCount > 0 || CouponConfig.consumeChanceCouponsOnFailedRoll()) {
            coupon.consumeUse(player);
        }
    }

    private static boolean isSmithingTemplate(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (Item template : SMITHING_TEMPLATES) {
            if (stack.is(template)) {
                return true;
            }
        }
        return false;
    }

    private static int countMatchingCarriedItems(Player player, ItemStack match) {
        if (match.isEmpty()) {
            return 0;
        }

        int count = 0;
        Inventory inventory = player.getInventory();
        count += countMatchingItems(inventory.items, match);
        count += countMatchingItems(inventory.armor, match);
        count += countMatchingItems(inventory.offhand, match);
        ItemStack carried = player.containerMenu.getCarried();
        if (ItemStack.isSameItemSameComponents(carried, match)) {
            count += carried.getCount();
        }
        return count;
    }

    private static int countMatchingItems(Iterable<ItemStack> stacks, ItemStack match) {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, match)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void refundIntoSmithingTemplateSlot(Player player, SmithingMenu menu, ItemStack refund) {
        if (refund.isEmpty()) {
            return;
        }

        ItemStack templateStack = menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        if (templateStack.isEmpty()) {
            menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(refund);
            menu.broadcastFullState();
            return;
        }

        if (ItemStack.isSameItemSameComponents(templateStack, refund)) {
            int accepted = Math.min(refund.getCount(), templateStack.getMaxStackSize() - templateStack.getCount());
            if (accepted > 0) {
                templateStack.grow(accepted);
                menu.getSlot(SmithingMenu.TEMPLATE_SLOT).setChanged();
                refund.shrink(accepted);
                menu.broadcastFullState();
            }
        }

        if (!refund.isEmpty()) {
            giveOrDrop(player, refund);
        }
    }

    private static DropCouponCandidate findBestDeathDropCoupon(Player player, Collection<ItemEntity> drops) {
        DropCouponCandidate best = null;
        CouponData.CarriedCoupon carried = findBestCoupon(player, CouponEffectType.DEATH_DROP);
        if (carried != null) {
            best = new DropCouponCandidate(carried, null, carried.stack(), carried.coupon());
        }

        for (ItemEntity itemEntity : drops) {
            ItemStack stack = itemEntity.getItem();
            if (stack.getItem() instanceof CouponItem coupon
                    && coupon.effect() == CouponEffectType.DEATH_DROP
                    && CouponConfig.isCouponEnabled(coupon.effect(), coupon.mode())
                    && isBetterDeathDropCoupon(player, stack, coupon, best)) {
                best = new DropCouponCandidate(null, itemEntity, stack, coupon);
            }
        }

        return best;
    }

    private static boolean isBetterDeathDropCoupon(Player player, ItemStack stack, CouponItem coupon, DropCouponCandidate currentBest) {
        if (currentBest == null) {
            return true;
        }

        int candidateRank = modeRank(coupon);
        int currentRank = modeRank(currentBest.coupon());
        if (candidateRank != currentRank) {
            return candidateRank > currentRank;
        }

        int candidateDiscount = discountPercent(player, stack, coupon);
        int currentDiscount = discountPercent(player, currentBest.stack(), currentBest.coupon());
        if (candidateDiscount != currentDiscount) {
            return candidateDiscount > currentDiscount;
        }

        return remainingValue(player, stack, coupon) > remainingValue(player, currentBest.stack(), currentBest.coupon());
    }

    private static int modeRank(CouponItem coupon) {
        return switch (coupon.mode()) {
            case TIMED -> 3;
            case USES -> 2;
            case SINGLE_USE -> 1;
        };
    }

    private static int remainingValue(Player player, ItemStack stack, CouponItem coupon) {
        return switch (coupon.mode()) {
            case TIMED -> CouponData.secondsRemaining(stack, coupon, player.level());
            case SINGLE_USE, USES -> CouponData.usesRemaining(stack, coupon, player.level());
        };
    }

    private static int discountPercent(Player player, ItemStack stack, CouponItem coupon) {
        return CouponData.discountPercent(stack, coupon, player.level());
    }

    private static void consumeDeathDropCoupon(Player player, Collection<ItemEntity> drops, DropCouponCandidate candidate) {
        if (candidate.carried() != null) {
            candidate.carried().consumeUse(player);
            return;
        }

        new CouponData.CarriedCoupon(candidate.stack(), candidate.coupon(), () -> {
        }).consumeUse(player);
        if (candidate.stack().isEmpty() && candidate.entity() != null) {
            drops.remove(candidate.entity());
            candidate.entity().discard();
        }
    }

    private static List<ItemStack> preventDroppedItems(Collection<ItemEntity> drops, int discountPercent, Player player, ItemEntity ignoredDrop) {
        List<ItemStack> returned = new ArrayList<>();
        Iterator<ItemEntity> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            if (itemEntity == ignoredDrop) {
                continue;
            }
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

    private static void syncInventory(ServerPlayer player) {
        player.containerMenu.broadcastFullState();
        if (player.containerMenu != player.inventoryMenu) {
            player.inventoryMenu.broadcastFullState();
        }
    }

    private record AnvilDiscountState(String key, int baseXpCost, int discountedXpCost, int baseMaterialCost, int discountedMaterialCost) {
    }

    private record SmithingState(ItemStack template, ItemStack base, ItemStack addition, ItemStack result, int carriedResultCount) {
        private static SmithingState capture(Player player, SmithingMenu menu) {
            ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem().copy();
            return new SmithingState(
                    menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().copy(),
                    menu.getSlot(SmithingMenu.BASE_SLOT).getItem().copy(),
                    menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().copy(),
                    result,
                    countMatchingCarriedItems(player, result)
            );
        }

        private boolean hadResult() {
            return !this.result.isEmpty();
        }

        private int consumedInputCount(SmithingState current) {
            if (!sameConsumedStack(this.template, current.template)
                    || !sameConsumedStack(this.base, current.base)
                    || !sameConsumedStack(this.addition, current.addition)) {
                return 0;
            }

            int consumedTemplates = consumedCount(this.template, current.template);
            int consumedBase = consumedCount(this.base, current.base);
            int consumedAddition = consumedCount(this.addition, current.addition);
            return Math.min(consumedTemplates, Math.min(consumedBase, consumedAddition));
        }

        private static boolean sameConsumedStack(ItemStack previous, ItemStack current) {
            return current.isEmpty() || ItemStack.isSameItemSameComponents(previous, current);
        }

        private static int consumedCount(ItemStack previous, ItemStack current) {
            return previous.getCount() - current.getCount();
        }
    }

    private record DropCouponCandidate(CouponData.CarriedCoupon carried, ItemEntity entity, ItemStack stack, CouponItem coupon) {
    }
}
