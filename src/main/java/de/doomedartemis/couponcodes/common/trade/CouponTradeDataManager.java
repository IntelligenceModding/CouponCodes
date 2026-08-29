package de.doomedartemis.couponcodes.common.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import org.slf4j.Logger;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CouponTradeDataManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "coupon_trades";
    private static final FileToIdConverter LISTER = FileToIdConverter.json(DIRECTORY);
    private static final float DEFAULT_PRICE_MULTIPLIER = 0.05F;
    private static final int MIN_PROFESSION_COUPON_COST = 48;
    private static final int MAX_PROFESSION_COUPON_COST = 64;

    private static TradePool genericPool = new TradePool(1, List.of());
    private static TradePool rarePool = new TradePool(1, List.of());
    private static Map<ResourceKey<VillagerProfession>, Int2ObjectMap<TradePool>> professionPools = Map.of();

    public CouponTradeDataManager() {
    }

    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath("coupon_codes", "coupon_trades"), new CouponTradeDataManager());
    }

    public static void onWandererTrades(WandererTradesEvent event) {
        addListings(event.getGenericTrades(), genericPool);
        addListings(event.getRareTrades(), rarePool);
    }

    public static void onVillagerTrades(VillagerTradesEvent event) {
        Int2ObjectMap<TradePool> pools = professionPools.get(event.getType());
        if (pools == null || pools.isEmpty()) {
            return;
        }

        pools.forEach((level, pool) -> {
            List<VillagerTrades.ItemListing> trades = event.getTrades().get(level);
            if (trades != null) {
                addListings(trades, pool);
            }
        });
    }

    private static void addListings(List<VillagerTrades.ItemListing> trades, TradePool pool) {
        if (pool.entries().isEmpty()) {
            return;
        }

        for (int i = 0; i < pool.listings(); i++) {
            trades.add(new PoolTradeListing(pool));
        }
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> resources = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(resourceManager).entrySet()) {
            Identifier id = LISTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                resources.put(id, JsonParser.parseReader(reader));
            } catch (IOException | JsonParseException exception) {
                LOGGER.error("Couldn't read coupon trade data {}", id, exception);
            }
        }
        return resources;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        MutableTradePool generic = new MutableTradePool(1);
        MutableTradePool rare = new MutableTradePool(1);
        Map<ResourceKey<VillagerProfession>, Int2ObjectMap<MutableTradePool>> professions = new HashMap<>();

        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(entry -> parseFile(entry.getKey(), entry.getValue(), generic, rare, professions));

        genericPool = generic.freeze();
        rarePool = rare.freeze();
        professionPools = freezeProfessions(professions);
        int professionOfferCount = professionPools.values().stream()
                .flatMap(pools -> pools.values().stream())
                .mapToInt(pool -> pool.entries().size())
                .sum();
        LOGGER.info("Loaded {} generic and {} rare coupon wandering trader offers, plus {} villager profession coupon offers", genericPool.entries().size(), rarePool.entries().size(), professionOfferCount);
    }

    private static void parseFile(Identifier fileId, JsonElement json, MutableTradePool generic, MutableTradePool rare, Map<ResourceKey<VillagerProfession>, Int2ObjectMap<MutableTradePool>> professions) {
        try {
            JsonObject root = GsonHelper.convertToJsonObject(json, fileId.toString());
            if (GsonHelper.getAsBoolean(root, "replace", false)) {
                generic.clear();
                rare.clear();
                professions.clear();
            }
            if (GsonHelper.getAsBoolean(root, "replace_generic", false)) {
                generic.clear();
            }
            if (GsonHelper.getAsBoolean(root, "replace_rare", false)) {
                rare.clear();
            }
            if (GsonHelper.getAsBoolean(root, "replace_professions", false)) {
                professions.clear();
            }

            if (root.has("generic_listings")) {
                generic.setListings(GsonHelper.getAsInt(root, "generic_listings"));
            }
            if (root.has("rare_listings")) {
                rare.setListings(GsonHelper.getAsInt(root, "rare_listings"));
            }

            parseTrades(root, "generic", generic);
            parseTrades(root, "rare", rare);
            parseProfessionTrades(root, professions);
        } catch (JsonParseException | IllegalArgumentException exception) {
            LOGGER.error("Couldn't parse coupon trade data {}", fileId, exception);
        }
    }

    private static void parseTrades(JsonObject root, String fieldName, MutableTradePool pool) {
        JsonArray trades = GsonHelper.getAsJsonArray(root, fieldName, null);
        if (trades == null) {
            return;
        }

        for (JsonElement element : trades) {
            pool.add(parseTrade(GsonHelper.convertToJsonObject(element, fieldName + " trade")));
        }
    }

    private static void parseProfessionTrades(JsonObject root, Map<ResourceKey<VillagerProfession>, Int2ObjectMap<MutableTradePool>> professions) {
        JsonArray pools = GsonHelper.getAsJsonArray(root, "villagers", null);
        if (pools == null) {
            return;
        }

        for (JsonElement element : pools) {
            JsonObject json = GsonHelper.convertToJsonObject(element, "villager trade pool");
            ResourceKey<VillagerProfession> profession = parseProfession(GsonHelper.getAsString(json, "profession"));
            int level = Mth.clamp(GsonHelper.getAsInt(json, "level", 5), 1, 5);
            MutableTradePool pool = professions
                    .computeIfAbsent(profession, ignored -> new Int2ObjectOpenHashMap<>())
                    .computeIfAbsent(level, ignored -> new MutableTradePool(1));

            if (GsonHelper.getAsBoolean(json, "replace", false)) {
                pool.clear();
            }
            if (json.has("listings")) {
                pool.setListings(GsonHelper.getAsInt(json, "listings"));
            }

            JsonArray entries = GsonHelper.getAsJsonArray(json, "entries");
            for (JsonElement tradeElement : entries) {
                pool.add(parseProfessionTrade(GsonHelper.convertToJsonObject(tradeElement, "villager coupon trade")));
            }
        }
    }

    private static TradeEntry parseTrade(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type");
        int weight = Math.max(0, GsonHelper.getAsInt(json, "weight", 1));
        int count = Math.max(1, GsonHelper.getAsInt(json, "count", 1));
        int maxUses = Math.max(1, GsonHelper.getAsInt(json, "max_uses", 1));
        int xp = Math.max(0, GsonHelper.getAsInt(json, "xp", 2));
        float priceMultiplier = Math.max(0.0F, GsonHelper.getAsFloat(json, "price_multiplier", DEFAULT_PRICE_MULTIPLIER));

        return switch (type) {
            case "empty_coupon" -> new EmptyCouponTradeEntry(
                    weight,
                    emeraldCost(json, "emerald_cost", 5),
                    count,
                    maxUses,
                    xp,
                    priceMultiplier
            );
            case "coupon" -> new CouponTradeEntry(
                    weight,
                    emeraldCost(json, "emerald_cost", 16),
                    parseEffect(GsonHelper.getAsString(json, "effect")),
                    parseMode(GsonHelper.getAsString(json, "mode")),
                    maxUses,
                    xp,
                    priceMultiplier
            );
            case "random_coupon" -> new RandomCouponTradeEntry(
                    weight,
                    parseRarityCosts(json),
                    maxUses,
                    xp,
                    priceMultiplier
            );
            case "item" -> new ItemTradeEntry(
                    weight,
                    emeraldCost(json, "emerald_cost", 5),
                    parseItem(GsonHelper.getAsString(json, "item")),
                    count,
                    maxUses,
                    xp,
                    priceMultiplier
            );
            default -> throw new JsonParseException("Unknown coupon trade type " + type);
        };
    }

    private static TradeEntry parseProfessionTrade(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type");
        int weight = Math.max(0, GsonHelper.getAsInt(json, "weight", 1));
        int maxUses = Math.max(1, GsonHelper.getAsInt(json, "max_uses", 1));
        int xp = Math.max(0, GsonHelper.getAsInt(json, "xp", 20));
        CostRange cost = professionCouponCost(json);

        return switch (type) {
            case "coupon" -> new ProfessionCouponTradeEntry(
                    weight,
                    cost,
                    parseEffect(GsonHelper.getAsString(json, "effect")),
                    parseMode(GsonHelper.getAsString(json, "mode")),
                    maxUses,
                    xp
            );
            case "random_coupon" -> new ProfessionRandomCouponTradeEntry(
                    weight,
                    cost,
                    maxUses,
                    xp
            );
            default -> throw new JsonParseException("Unknown villager coupon trade type " + type);
        };
    }

    private static int emeraldCost(JsonObject json, String fieldName, int fallback) {
        return Math.clamp(GsonHelper.getAsInt(json, fieldName, fallback), 1, 64);
    }

    private static CostRange professionCouponCost(JsonObject json) {
        if (json.has("emerald_cost")) {
            int cost = Mth.clamp(GsonHelper.getAsInt(json, "emerald_cost"), MIN_PROFESSION_COUPON_COST, MAX_PROFESSION_COUPON_COST);
            return new CostRange(cost, cost);
        }

        int min = Mth.clamp(GsonHelper.getAsInt(json, "min_emerald_cost", MIN_PROFESSION_COUPON_COST), MIN_PROFESSION_COUPON_COST, MAX_PROFESSION_COUPON_COST);
        int max = Mth.clamp(GsonHelper.getAsInt(json, "max_emerald_cost", MAX_PROFESSION_COUPON_COST), MIN_PROFESSION_COUPON_COST, MAX_PROFESSION_COUPON_COST);
        return new CostRange(Math.min(min, max), Math.max(min, max));
    }

    private static Map<Rarity, Integer> parseRarityCosts(JsonObject json) {
        EnumMap<Rarity, Integer> costs = new EnumMap<>(Rarity.class);
        costs.put(Rarity.COMMON, 9);
        costs.put(Rarity.UNCOMMON, 16);
        costs.put(Rarity.RARE, 28);
        costs.put(Rarity.EPIC, 48);

        JsonObject configured = GsonHelper.getAsJsonObject(json, "costs", null);
        if (configured != null) {
            for (Rarity rarity : Rarity.values()) {
                costs.put(rarity, emeraldCost(configured, rarity.name().toLowerCase(Locale.ROOT), costs.get(rarity)));
            }
        }
        return Map.copyOf(costs);
    }

    private static Item parseItem(String name) {
        Identifier itemId = Identifier.parse(name);
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .filter(item -> item != Items.AIR)
                .orElseThrow(() -> new JsonParseException("Unknown item " + itemId));
    }

    private static ResourceKey<VillagerProfession> parseProfession(String name) {
        Identifier professionId = Identifier.parse(name);
        ResourceKey<VillagerProfession> professionKey = ResourceKey.create(Registries.VILLAGER_PROFESSION, professionId);
        if (professionKey.equals(VillagerProfession.NONE)
                || professionKey.equals(VillagerProfession.NITWIT)
                || BuiltInRegistries.VILLAGER_PROFESSION.getOptional(professionId).isEmpty()) {
            throw new JsonParseException("Unknown or invalid villager profession " + professionId);
        }
        return professionKey;
    }

    private static CouponEffectType parseEffect(String name) {
        return CouponEffectType.valueOf(name.toUpperCase(Locale.ROOT));
    }

    private static CouponMode parseMode(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "single_use", "once" -> CouponMode.SINGLE_USE;
            case "uses", "multi", "reusable" -> CouponMode.USES;
            case "timed" -> CouponMode.TIMED;
            default -> throw new JsonParseException("Unknown coupon mode " + name);
        };
    }

    private static Map<ResourceKey<VillagerProfession>, Int2ObjectMap<TradePool>> freezeProfessions(Map<ResourceKey<VillagerProfession>, Int2ObjectMap<MutableTradePool>> professions) {
        Map<ResourceKey<VillagerProfession>, Int2ObjectMap<TradePool>> frozen = new HashMap<>();
        professions.forEach((profession, pools) -> {
            Int2ObjectMap<TradePool> frozenPools = new Int2ObjectOpenHashMap<>();
            pools.forEach((level, pool) -> {
                TradePool tradePool = pool.freeze();
                if (!tradePool.entries().isEmpty()) {
                    frozenPools.put(level, tradePool);
                }
            });
            if (!frozenPools.isEmpty()) {
                frozen.put(profession, frozenPools);
            }
        });
        return Map.copyOf(frozen);
    }

    private record TradePool(int listings, List<TradeEntry> entries) {
    }

    private record CostRange(int min, int max) {
        private int roll(RandomSource random) {
            return min + random.nextInt(max - min + 1);
        }
    }

    private static final class MutableTradePool {
        private int listings;
        private final List<TradeEntry> entries = new ArrayList<>();

        private MutableTradePool(int listings) {
            this.listings = listings;
        }

        private void setListings(int listings) {
            this.listings = Math.clamp(listings, 0, 16);
        }

        private void add(TradeEntry entry) {
            if (entry.weight() > 0) {
                entries.add(entry);
            }
        }

        private void clear() {
            entries.clear();
        }

        private TradePool freeze() {
            return new TradePool(listings, List.copyOf(entries));
        }
    }

    private interface TradeEntry {
        int weight();

        MerchantOffer create(Entity trader, RandomSource random);
    }

    private record EmptyCouponTradeEntry(int weight, int emeraldCost, int count, int maxUses, int xp, float priceMultiplier) implements TradeEntry {
        @Override
        public MerchantOffer create(Entity trader, RandomSource random) {
            if (!CouponConfig.canRollEmptyCoupons()) {
                return null;
            }
            return offer(emeraldCost, new ItemStack(ModItems.EMPTY_COUPON.get(), count), maxUses, xp, priceMultiplier);
        }
    }

    private record CouponTradeEntry(int weight, int emeraldCost, CouponEffectType effect, CouponMode mode, int maxUses, int xp, float priceMultiplier) implements TradeEntry {
        @Override
        public MerchantOffer create(Entity trader, RandomSource random) {
            if (!CouponConfig.isCouponEnabled(effect, mode)) {
                return null;
            }
            return offer(emeraldCost, new ItemStack(ModItems.couponItem(effect, mode).get()), maxUses, xp, priceMultiplier);
        }
    }

    private record RandomCouponTradeEntry(int weight, Map<Rarity, Integer> costs, int maxUses, int xp, float priceMultiplier) implements TradeEntry {
        @Override
        public MerchantOffer create(Entity trader, RandomSource random) {
            if (!CouponConfig.areCouponsEnabled()) {
                return null;
            }

            Optional<DeferredItem<CouponItem>> coupon = ModItems.randomCoupon(random);
            if (coupon.isEmpty()) {
                return null;
            }

            CouponItem couponItem = coupon.get().get();
            int emeraldCost = costs.getOrDefault(ModItems.couponRarity(couponItem.effect(), couponItem.mode()), 16);
            return offer(emeraldCost, new ItemStack(couponItem), maxUses, xp, priceMultiplier);
        }
    }

    private record ProfessionCouponTradeEntry(int weight, CostRange cost, CouponEffectType effect, CouponMode mode, int maxUses, int xp) implements TradeEntry {
        @Override
        public MerchantOffer create(Entity trader, RandomSource random) {
            if (!CouponConfig.isCouponEnabled(effect, mode)) {
                return null;
            }
            return fixedPriceOffer(cost.roll(random), new ItemStack(ModItems.couponItem(effect, mode).get()), maxUses, xp);
        }
    }

    private record ProfessionRandomCouponTradeEntry(int weight, CostRange cost, int maxUses, int xp) implements TradeEntry {
        @Override
        public MerchantOffer create(Entity trader, RandomSource random) {
            if (!CouponConfig.areCouponsEnabled()) {
                return null;
            }

            Optional<DeferredItem<CouponItem>> coupon = ModItems.randomCoupon(random);
            if (coupon.isEmpty()) {
                return null;
            }

            return fixedPriceOffer(cost.roll(random), new ItemStack(coupon.get().get()), maxUses, xp);
        }
    }

    private record ItemTradeEntry(int weight, int emeraldCost, Item item, int count, int maxUses, int xp, float priceMultiplier) implements TradeEntry {
        @Override
        public MerchantOffer create(Entity trader, RandomSource random) {
            return offer(emeraldCost, new ItemStack(item, count), maxUses, xp, priceMultiplier);
        }
    }

    private static MerchantOffer offer(int emeraldCost, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeraldCost), result, maxUses, xp, priceMultiplier);
    }

    private static MerchantOffer fixedPriceOffer(int emeraldCost, ItemStack result, int maxUses, int xp) {
        return new FixedPriceMerchantOffer(new ItemCost(Items.EMERALD, Mth.clamp(emeraldCost, MIN_PROFESSION_COUPON_COST, MAX_PROFESSION_COUPON_COST)), Optional.empty(), result, 0, maxUses, xp);
    }

    private static final class FixedPriceMerchantOffer extends MerchantOffer {
        private FixedPriceMerchantOffer(ItemCost costA, Optional<ItemCost> costB, ItemStack result, int uses, int maxUses, int xp) {
            super(costA, costB, result, uses, maxUses, xp, 0.0F, 0);
        }

        @Override
        public ItemStack getCostA() {
            return getItemCostA().itemStack().copy();
        }

        @Override
        public void updateDemand() {
        }

        @Override
        public int getDemand() {
            return 0;
        }

        @Override
        public void addToSpecialPriceDiff(int diff) {
        }

        @Override
        public void resetSpecialPriceDiff() {
        }

        @Override
        public int getSpecialPriceDiff() {
            return 0;
        }

        @Override
        public void setSpecialPriceDiff(int specialPriceDiff) {
        }

        @Override
        public float getPriceMultiplier() {
            return 0.0F;
        }

        @Override
        public boolean satisfiedBy(ItemStack offerA, ItemStack offerB) {
            if (!getItemCostA().test(offerA) || offerA.getCount() < getItemCostA().count()) {
                return false;
            }
            Optional<ItemCost> costB = getItemCostB();
            return costB.map(itemCost -> itemCost.test(offerB) && offerB.getCount() >= itemCost.count()).orElseGet(offerB::isEmpty);
        }

        @Override
        public boolean take(ItemStack offerA, ItemStack offerB) {
            if (!satisfiedBy(offerA, offerB)) {
                return false;
            }

            offerA.shrink(getItemCostA().count());
            getItemCostB().ifPresent(itemCost -> offerB.shrink(itemCost.count()));
            return true;
        }

        @Override
        public MerchantOffer copy() {
            return new FixedPriceMerchantOffer(getItemCostA(), getItemCostB(), getResult().copy(), getUses(), getMaxUses(), getXp());
        }
    }

    private record PoolTradeListing(TradePool pool) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
            for (int attempt = 0; attempt < 8; attempt++) {
                TradeEntry entry = choose(pool.entries(), random);
                if (entry == null) {
                    return null;
                }

                MerchantOffer offer = entry.create(trader, random);
                if (offer != null) {
                    return offer;
                }
            }
            return null;
        }

        private static TradeEntry choose(List<TradeEntry> entries, RandomSource random) {
            int totalWeight = 0;
            for (TradeEntry entry : entries) {
                totalWeight += entry.weight();
            }

            if (totalWeight <= 0) {
                return null;
            }

            int selectedWeight = random.nextInt(totalWeight);
            for (TradeEntry entry : entries) {
                selectedWeight -= entry.weight();
                if (selectedWeight < 0) {
                    return entry;
                }
            }
            return null;
        }
    }
}
