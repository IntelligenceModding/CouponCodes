package de.doomedartemis.couponcodes.common.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponCategory;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CouponLootDataManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "coupon_loot";
    private static final FileToIdConverter LISTER = FileToIdConverter.json(DIRECTORY);

    private static Map<ResourceLocation, LootProfile> lootTableProfiles = Map.of();
    private static Map<ResourceLocation, LootProfile> entityProfiles = Map.of();

    public CouponLootDataManager() {
    }

    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(ResourceLocation.fromNamespaceAndPath("coupon_codes", "coupon_loot"), new CouponLootDataManager());
    }

    public static LootProfile lootTableProfile(ResourceLocation lootTable) {
        return lootTableProfiles.get(lootTable);
    }

    public static LootProfile entityProfile(ResourceLocation entityType) {
        return entityProfiles.get(entityType);
    }

    public static List<ItemStack> generate(LootProfile profile, RandomSource random) {
        if (profile == null || !CouponConfig.areCouponsEnabled()) {
            return List.of();
        }

        List<ItemStack> generated = new ArrayList<>();
        for (LootRoll roll : profile.rolls()) {
            if (random.nextDouble() >= roll.chance()) {
                continue;
            }

            int rollCount = roll.minRolls() + random.nextInt(roll.maxRolls() - roll.minRolls() + 1);
            for (int i = 0; i < rollCount; i++) {
                LootEntry entry = choose(roll.entries(), random);
                if (entry == null) {
                    continue;
                }

                ItemStack stack = entry.create(random);
                if (!stack.isEmpty()) {
                    generated.add(stack);
                }
            }
        }

        return generated;
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> resources = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : LISTER.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation id = LISTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                resources.put(id, JsonParser.parseReader(reader));
            } catch (IOException | JsonParseException exception) {
                LOGGER.error("Couldn't read coupon loot data {}", id, exception);
            }
        }
        return resources;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, List<LootRoll>> loadedLootTables = new HashMap<>();
        Map<ResourceLocation, List<LootRoll>> loadedEntities = new HashMap<>();

        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> parseFile(entry.getKey(), entry.getValue(), loadedLootTables, loadedEntities));

        lootTableProfiles = freeze(loadedLootTables);
        entityProfiles = freeze(loadedEntities);
        LOGGER.info("Loaded {} coupon loot table profiles and {} coupon entity drop profiles", lootTableProfiles.size(), entityProfiles.size());
    }

    private static Map<ResourceLocation, LootProfile> freeze(Map<ResourceLocation, List<LootRoll>> profiles) {
        Map<ResourceLocation, LootProfile> frozen = new HashMap<>();
        profiles.forEach((target, rolls) -> {
            if (!rolls.isEmpty()) {
                frozen.put(target, new LootProfile(List.copyOf(rolls)));
            }
        });
        return Map.copyOf(frozen);
    }

    private static void parseFile(ResourceLocation fileId, JsonElement json, Map<ResourceLocation, List<LootRoll>> lootTables, Map<ResourceLocation, List<LootRoll>> entities) {
        try {
            JsonObject root = GsonHelper.convertToJsonObject(json, fileId.toString());
            boolean replace = GsonHelper.getAsBoolean(root, "replace", false);
            if (GsonHelper.isArrayNode(root, "profiles")) {
                for (JsonElement element : GsonHelper.getAsJsonArray(root, "profiles")) {
                    parseProfile(fileId, GsonHelper.convertToJsonObject(element, "profile"), replace, lootTables, entities);
                }
            } else {
                parseProfile(fileId, root, replace, lootTables, entities);
            }
        } catch (JsonParseException | IllegalArgumentException exception) {
            LOGGER.error("Couldn't parse coupon loot data {}", fileId, exception);
        }
    }

    private static void parseProfile(ResourceLocation fileId, JsonObject profile, boolean rootReplace, Map<ResourceLocation, List<LootRoll>> lootTables, Map<ResourceLocation, List<LootRoll>> entities) {
        boolean replace = GsonHelper.getAsBoolean(profile, "replace", rootReplace);
        List<LootRoll> rolls = parseRolls(profile);

        if (GsonHelper.isArrayNode(profile, "loot_tables")) {
            for (ResourceLocation lootTable : parseIds(GsonHelper.getAsJsonArray(profile, "loot_tables"), "loot_tables")) {
                applyProfile(lootTables, lootTable, rolls, replace);
            }
        }

        if (GsonHelper.isArrayNode(profile, "entities")) {
            for (ResourceLocation entity : parseIds(GsonHelper.getAsJsonArray(profile, "entities"), "entities")) {
                applyProfile(entities, entity, rolls, replace);
            }
        }

        if (!GsonHelper.isArrayNode(profile, "loot_tables") && !GsonHelper.isArrayNode(profile, "entities")) {
            throw new JsonParseException("Coupon loot profile in " + fileId + " needs loot_tables, entities, or both");
        }
    }

    private static void applyProfile(Map<ResourceLocation, List<LootRoll>> profiles, ResourceLocation target, List<LootRoll> rolls, boolean replace) {
        List<LootRoll> targetRolls = profiles.computeIfAbsent(target, ignored -> new ArrayList<>());
        if (replace) {
            targetRolls.clear();
        }
        targetRolls.addAll(rolls);
    }

    private static List<ResourceLocation> parseIds(JsonArray array, String fieldName) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (JsonElement element : array) {
            ids.add(ResourceLocation.parse(GsonHelper.convertToString(element, fieldName)));
        }
        return ids;
    }

    private static List<LootRoll> parseRolls(JsonObject profile) {
        JsonArray rolls = GsonHelper.getAsJsonArray(profile, "rolls");
        JsonArray profileEntries = GsonHelper.getAsJsonArray(profile, "entries", null);
        List<LootRoll> parsed = new ArrayList<>();
        for (JsonElement rollElement : rolls) {
            JsonObject roll = GsonHelper.convertToJsonObject(rollElement, "roll");
            double chance = parseChance(roll);
            int minRolls = Math.max(0, GsonHelper.getAsInt(roll, "min_rolls", GsonHelper.getAsInt(roll, "count", 1)));
            int maxRolls = Math.max(minRolls, GsonHelper.getAsInt(roll, "max_rolls", minRolls));
            JsonArray entryArray = GsonHelper.getAsJsonArray(roll, "entries", profileEntries);
            if (entryArray == null) {
                throw new JsonParseException("Coupon loot roll needs entries or profile-level entries");
            }
            List<LootEntry> entries = parseEntries(entryArray);
            if (!entries.isEmpty() && maxRolls > 0) {
                parsed.add(new LootRoll(chance, minRolls, maxRolls, List.copyOf(entries)));
            }
        }
        return List.copyOf(parsed);
    }

    private static double parseChance(JsonObject roll) {
        double chance = GsonHelper.getAsDouble(roll, "chance", -1.0D);
        if (chance < 0.0D) {
            chance = GsonHelper.getAsDouble(roll, "chance_percent", 100.0D) / 100.0D;
        }
        return Mth.clamp(chance, 0.0D, 1.0D);
    }

    private static List<LootEntry> parseEntries(JsonArray entries) {
        List<LootEntry> parsed = new ArrayList<>();
        for (JsonElement entryElement : entries) {
            JsonObject entry = GsonHelper.convertToJsonObject(entryElement, "entry");
            String type = GsonHelper.getAsString(entry, "type");
            int weight = Math.max(0, GsonHelper.getAsInt(entry, "weight", 1));
            if (weight <= 0) {
                continue;
            }

            if ("coupon_set".equals(type)) {
                CouponEffectType effect = parseEffect(GsonHelper.getAsString(entry, "effect"));
                parsed.add(new CouponEntry(effect, CouponMode.SINGLE_USE, weight * 4));
                parsed.add(new CouponEntry(effect, CouponMode.USES, weight * 2));
                parsed.add(new CouponEntry(effect, CouponMode.TIMED, weight));
            } else if ("coupon".equals(type)) {
                parsed.add(new CouponEntry(
                        parseEffect(GsonHelper.getAsString(entry, "effect")),
                        parseMode(GsonHelper.getAsString(entry, "mode")),
                        weight
                ));
            } else if ("category_coupon_set".equals(type)) {
                parsed.add(new CategoryCouponEntry(parseCategory(GsonHelper.getAsString(entry, "category")), null, weight));
            } else if ("category_coupon".equals(type)) {
                parsed.add(new CategoryCouponEntry(
                        parseCategory(GsonHelper.getAsString(entry, "category")),
                        parseMode(GsonHelper.getAsString(entry, "mode")),
                        weight
                ));
            } else if ("item".equals(type)) {
                ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(entry, "item"));
                Item item = BuiltInRegistries.ITEM.getOptional(itemId)
                        .filter(found -> found != Items.AIR)
                        .orElseThrow(() -> new JsonParseException("Unknown item " + itemId));
                int minCount = Math.max(1, GsonHelper.getAsInt(entry, "min_count", 1));
                int maxCount = Math.max(minCount, GsonHelper.getAsInt(entry, "max_count", minCount));
                parsed.add(new ItemEntry(item, minCount, maxCount, weight));
            } else {
                throw new JsonParseException("Unknown coupon loot entry type " + type);
            }
        }
        return parsed;
    }

    private static CouponEffectType parseEffect(String name) {
        return CouponEffectType.valueOf(name.toUpperCase(Locale.ROOT));
    }

    private static CouponCategory parseCategory(String name) {
        return CouponCategory.valueOf(name.toUpperCase(Locale.ROOT));
    }

    private static CouponMode parseMode(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "single_use", "once" -> CouponMode.SINGLE_USE;
            case "uses", "multi", "reusable" -> CouponMode.USES;
            case "timed" -> CouponMode.TIMED;
            default -> throw new JsonParseException("Unknown coupon mode " + name);
        };
    }

    private static Optional<CouponEntry> randomCategoryCoupon(CouponCategory category, CouponMode mode, RandomSource random) {
        List<LootEntry> entries = new ArrayList<>();
        for (CouponEffectType effect : CouponEffectType.values()) {
            if (effect.category() != category) {
                continue;
            }

            if (mode == null) {
                entries.add(new CouponEntry(effect, CouponMode.SINGLE_USE, 4));
                entries.add(new CouponEntry(effect, CouponMode.USES, 2));
                entries.add(new CouponEntry(effect, CouponMode.TIMED, 1));
            } else {
                entries.add(new CouponEntry(effect, mode, 1));
            }
        }
        return Optional.ofNullable((CouponEntry) choose(List.copyOf(entries), random));
    }

    private static LootEntry choose(List<LootEntry> entries, RandomSource random) {
        int totalWeight = 0;
        for (LootEntry entry : entries) {
            if (entry.enabled()) {
                totalWeight += entry.weight();
            }
        }

        if (totalWeight <= 0) {
            return null;
        }

        int selectedWeight = random.nextInt(totalWeight);
        for (LootEntry entry : entries) {
            if (!entry.enabled()) {
                continue;
            }

            selectedWeight -= entry.weight();
            if (selectedWeight < 0) {
                return entry;
            }
        }

        return null;
    }

    public record LootProfile(List<LootRoll> rolls) {
    }

    public record LootRoll(double chance, int minRolls, int maxRolls, List<LootEntry> entries) {
    }

    public interface LootEntry {
        int weight();

        boolean enabled();

        ItemStack create(RandomSource random);
    }

    public record CouponEntry(CouponEffectType effect, CouponMode mode, int weight) implements LootEntry {
        @Override
        public boolean enabled() {
            return CouponConfig.isCouponEnabled(effect, mode);
        }

        @Override
        public ItemStack create(RandomSource random) {
            return new ItemStack(ModItems.couponItem(effect, mode).get());
        }
    }

    public record CategoryCouponEntry(CouponCategory category, CouponMode mode, int weight) implements LootEntry {
        @Override
        public boolean enabled() {
            for (CouponEffectType effect : CouponEffectType.values()) {
                if (effect.category() != category) {
                    continue;
                }

                if (mode == null) {
                    for (CouponMode couponMode : CouponMode.values()) {
                        if (CouponConfig.isCouponEnabled(effect, couponMode)) {
                            return true;
                        }
                    }
                } else if (CouponConfig.isCouponEnabled(effect, mode)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public ItemStack create(RandomSource random) {
            Optional<CouponEntry> coupon = randomCategoryCoupon(category, mode, random);
            return coupon
                    .map(entry -> new ItemStack(ModItems.couponItem(entry.effect(), entry.mode()).get()))
                    .orElse(ItemStack.EMPTY);
        }
    }

    public record ItemEntry(Item item, int minCount, int maxCount, int weight) implements LootEntry {
        @Override
        public boolean enabled() {
            if (item == ModItems.EMPTY_COUPON.get()) {
                return CouponConfig.canRollEmptyCoupons();
            }
            if (item instanceof CouponPouchItem) {
                return CouponConfig.areCouponPouchesEnabled();
            }
            return true;
        }

        @Override
        public ItemStack create(RandomSource random) {
            int count = minCount + random.nextInt(maxCount - minCount + 1);
            return new ItemStack(item, count);
        }
    }
}
