# Coupon Codes Datapacks and Config

This document explains the data-driven parts of Coupon Codes: custom datapack JSON, normal Minecraft data files shipped by the mod, item/effect IDs, and the server/client config files that control behavior.

## Where Files Go

Coupon Codes uses the mod id `coupon_codes`.

In a datapack, custom Coupon Codes files go under:

```text
data/<your_namespace>/coupon_loot/*.json
data/<your_namespace>/coupon_trades/*.json
```

The mod also ships normal Minecraft/NeoForge data under:

```text
data/coupon_codes/recipe/
data/coupon_codes/advancement/
data/coupon_codes/tags/item/
data/coupon_codes/curios/entities/
data/coupon_codes/loot_modifiers/
data/neoforge/loot_modifiers/
```

Use `/reload` after changing datapack JSON. Invalid custom JSON is logged and skipped.

## IDs

Coupon modes:

| Mode | Command name | Datapack aliases | Meaning |
| --- | --- | --- | --- |
| `single_use` | `once` | `single_use`, `once` | One coupon use. |
| `uses` | `multi` | `uses`, `multi`, `reusable` | A coupon with multiple uses. |
| `timed` | `timed` | `timed` | Activates for a duration. |

Coupon categories:

| Category | Effects |
| --- | --- |
| `equipment` | `durability`, `anvil_experience`, `tool_repair`, `smithing_template`, `repair_material` |
| `magic` | `enchanting_experience`, `brewing_ingredient`, `potion_duration`, `mending` |
| `trade` | `villager_trade`, `villager_restock` |
| `consumables` | `food`, `bone_meal`, `fishing` |
| `mobility` | `rocket`, `ender_pearl`, `elytra_glide`, `fall_damage` |
| `combat` | `arrow`, `totem`, `death_drop` |

Coupon item IDs follow this pattern:

```text
coupon_codes:<effect>_<mode_command_name>_coupon
```

Examples:

```text
coupon_codes:durability_once_coupon
coupon_codes:durability_multi_coupon
coupon_codes:durability_timed_coupon
coupon_codes:villager_trade_multi_coupon
```

Other item IDs:

```text
coupon_codes:empty_coupon
coupon_codes:coupon_pouch
coupon_codes:white_coupon_pouch
coupon_codes:orange_coupon_pouch
coupon_codes:magenta_coupon_pouch
coupon_codes:light_blue_coupon_pouch
coupon_codes:yellow_coupon_pouch
coupon_codes:lime_coupon_pouch
coupon_codes:pink_coupon_pouch
coupon_codes:gray_coupon_pouch
coupon_codes:light_gray_coupon_pouch
coupon_codes:cyan_coupon_pouch
coupon_codes:purple_coupon_pouch
coupon_codes:blue_coupon_pouch
coupon_codes:brown_coupon_pouch
coupon_codes:green_coupon_pouch
coupon_codes:red_coupon_pouch
coupon_codes:black_coupon_pouch
```

## Coupon Loot Datapack Files

Files in `data/<namespace>/coupon_loot/*.json` define extra coupon drops for loot tables and entity deaths.

Chest/container loot is applied by the NeoForge global loot modifier `coupon_codes:vanilla_chest_coupons`. Entity drops are applied during the living drops event. Both systems read the same `coupon_loot` profile format.

### Basic Structure

You can define one profile per file:

```json
{
  "loot_tables": ["minecraft:chests/simple_dungeon"],
  "entries": [
    {"type": "item", "item": "coupon_codes:empty_coupon", "weight": 8},
    {"type": "coupon_set", "effect": "durability", "weight": 12}
  ],
  "rolls": [
    {"chance": 0.10}
  ]
}
```

Or multiple profiles:

```json
{
  "profiles": [
    {
      "loot_tables": ["minecraft:chests/desert_pyramid"],
      "entries": [{"type": "coupon_set", "effect": "fall_damage", "weight": 10}],
      "rolls": [{"chance_percent": 15}]
    },
    {
      "entities": ["minecraft:evoker"],
      "entries": [{"type": "coupon", "effect": "totem", "mode": "once", "weight": 10}],
      "rolls": [{"chance": 0.45}]
    }
  ]
}
```

### Profile Fields

| Field | Required | Meaning |
| --- | --- | --- |
| `loot_tables` | Required unless `entities` exists | Loot table IDs to attach this profile to. |
| `entities` | Required unless `loot_tables` exists | Entity type IDs that can drop these entries on death. |
| `entries` | Required unless every roll has its own entries | Weighted pool used by rolls that do not define their own entries. |
| `rolls` | Yes | One or more roll definitions. |
| `replace` | No | Clears existing rolls for the target before adding this profile. Can be set on the file root or individual profile. |

If two datapack files target the same loot table or entity and `replace` is false, their rolls are appended.

### Roll Fields

| Field | Default | Meaning |
| --- | --- | --- |
| `chance` | `1.0` when `chance_percent` is omitted | Decimal chance from `0.0` to `1.0`. Values are clamped. |
| `chance_percent` | `100.0` | Percent chance. Used only when `chance` is not present. |
| `count` | `1` | Fixed number of entries to choose if min/max are not set. |
| `min_rolls` | `count` or `1` | Minimum number of entries to choose when the roll succeeds. |
| `max_rolls` | `min_rolls` | Maximum number of entries to choose when the roll succeeds. |
| `entries` | Profile-level `entries` | Roll-specific weighted entries. |

Each successful roll chooses `min_rolls` to `max_rolls` entries from its weighted pool.

### Entry Types

`coupon_set` adds all three modes for one effect. Internally, its weight is split as once `4x`, multi `2x`, timed `1x`.

```json
{"type": "coupon_set", "effect": "durability", "weight": 10}
```

`coupon` adds one exact coupon.

```json
{"type": "coupon", "effect": "totem", "mode": "uses", "weight": 3}
```

`category_coupon_set` picks any enabled coupon in a category and uses the same once/multi/timed weighting as `coupon_set`.

```json
{"type": "category_coupon_set", "category": "combat", "weight": 5}
```

`category_coupon` picks any enabled coupon in a category for one mode.

```json
{"type": "category_coupon", "category": "mobility", "mode": "timed", "weight": 2}
```

`item` adds a normal item stack. This is how empty coupons and pouches are added.

```json
{"type": "item", "item": "coupon_codes:empty_coupon", "min_count": 1, "max_count": 2, "weight": 8}
```

All entry weights are relative. Entries with weight `0` are ignored. Disabled coupons, disabled empty coupon rolls, and disabled pouches are automatically excluded.

## Coupon Trade Datapack Files

Files in `data/<namespace>/coupon_trades/*.json` add wandering trader and villager profession offers.

### Wandering Trader Example

```json
{
  "generic_listings": 1,
  "rare_listings": 1,
  "generic": [
    {
      "type": "empty_coupon",
      "weight": 1,
      "emerald_cost": 16,
      "count": 1,
      "max_uses": 1,
      "xp": 2,
      "price_multiplier": 0.05
    }
  ],
  "rare": [
    {
      "type": "random_coupon",
      "weight": 1,
      "costs": {
        "common": 9,
        "uncommon": 16,
        "rare": 28,
        "epic": 48
      },
      "max_uses": 1,
      "xp": 8,
      "price_multiplier": 0.05
    }
  ]
}
```

### Villager Profession Example

```json
{
  "villagers": [
    {
      "profession": "minecraft:armorer",
      "level": 5,
      "listings": 1,
      "entries": [
        {"type": "coupon", "effect": "durability", "mode": "uses", "weight": 4},
        {"type": "coupon", "effect": "repair_material", "mode": "uses", "weight": 3}
      ]
    }
  ]
}
```

### Trade File Fields

| Field | Meaning |
| --- | --- |
| `replace` | Clears generic, rare, and villager trade pools before this file is applied. |
| `replace_generic` | Clears only wandering trader generic entries. |
| `replace_rare` | Clears only wandering trader rare entries. |
| `replace_professions` | Clears only villager profession entries. |
| `generic_listings` | Number of generic wandering trader coupon listings to add, clamped `0` to `16`. |
| `rare_listings` | Number of rare wandering trader coupon listings to add, clamped `0` to `16`. |
| `generic` | Weighted generic wandering trader entries. |
| `rare` | Weighted rare wandering trader entries. |
| `villagers` | Villager profession trade pools. |

### Wandering Trader Entry Types

| Type | Fields |
| --- | --- |
| `empty_coupon` | `weight`, `emerald_cost`, `count`, `max_uses`, `xp`, `price_multiplier` |
| `coupon` | `weight`, `emerald_cost`, `effect`, `mode`, `max_uses`, `xp`, `price_multiplier` |
| `random_coupon` | `weight`, optional `costs`, `max_uses`, `xp`, `price_multiplier` |
| `item` | `weight`, `emerald_cost`, `item`, `count`, `max_uses`, `xp`, `price_multiplier` |

Defaults: `weight` 1, `count` 1, `max_uses` 1, `xp` 2 for wandering trader entries, `price_multiplier` 0.05. Emerald costs are clamped to `1` to `64`.

`random_coupon.costs` can set emerald costs by rarity:

```json
{
  "common": 9,
  "uncommon": 16,
  "rare": 28,
  "epic": 48
}
```

### Villager Pool Fields

| Field | Meaning |
| --- | --- |
| `profession` | Villager profession ID. `minecraft:none` and `minecraft:nitwit` are invalid. |
| `level` | Villager level, clamped `1` to `5`. Defaults to `5`. |
| `listings` | Number of listings to add at that level, clamped `0` to `16`. |
| `replace` | Clears the existing pool for this profession and level before adding entries. |
| `entries` | Weighted profession entries. |

Villager profession entries support:

```json
{"type": "coupon", "effect": "durability", "mode": "uses", "weight": 4}
{"type": "random_coupon", "weight": 1}
```

Profession coupon costs use `emerald_cost` or a range:

```json
{"type": "coupon", "effect": "totem", "mode": "once", "emerald_cost": 64}
{"type": "random_coupon", "min_emerald_cost": 48, "max_emerald_cost": 64}
```

Profession coupon costs are clamped to `48` to `64`. Profession trade defaults are `max_uses` 1 and `xp` 20.

## Other Datapack Data

### Recipes

`data/coupon_codes/recipe/coupon_pouch.json` defines the base pouch recipe: string, leather, and paper.

`data/coupon_codes/recipe/coupon_pouch_dyeing/*.json` defines special dyeing recipes for all vanilla dye colors.

### Tags

`data/coupon_codes/tags/item/coupon_pouches.json` contains every pouch item. Use this tag when another datapack or mod integration needs to recognize pouches.

`data/curios/tags/item/belt.json` includes Coupon Codes pouches in the Curios belt item tag.

### Curios Entity Slot Data

`data/coupon_codes/curios/entities/player.json` declares that players can use the `belt` Curios slot for coupon pouches when Curios is installed.

### Advancements

`data/coupon_codes/advancement/*.json` defines Coupon Codes advancements. Their reward behavior is not stored directly in the advancement JSON; the server config can award a configured item whenever a Coupon Codes advancement is completed.

### NeoForge Loot Modifier Data

`data/neoforge/loot_modifiers/global_loot_modifiers.json` registers the mod's global loot modifier list.

`data/coupon_codes/loot_modifiers/vanilla_chest_coupons.json` defines the `coupon_codes:vanilla_chest_coupons` modifier. It asks `CouponLootDataManager` for extra loot based on the queried loot table.

If a datapack replaces NeoForge global loot modifiers, keep `coupon_codes:vanilla_chest_coupons` unless you intentionally want to disable Coupon Codes chest loot.

## Config Files

The server config is registered as a NeoForge server config and controls gameplay. In a running world it is normally written as:

```text
<world>/serverconfig/coupon_codes-server.toml
```

The client config controls local rendering and is normally written as:

```text
config/coupon_codes-client.toml
```

Server config gates are checked at runtime. If a coupon, mode, empty coupon roll, or pouch is disabled, datapack loot/trades that would create it are skipped.

### Server Config Sections

| Section | Keys | What it controls |
| --- | --- | --- |
| `general` | `enableCoupons`, `enableEmptyCouponRolls`, `enableCouponPouches`, `enableCommands`, `showTimedCouponBossBar`, `discountValues` | Master switches, command availability, timed boss bars, and random discount percentages. |
| `dailyBoost` | `enableDailyBoosts`, `announceDailyBoosts`, `announcementMessage`, `categoryBoostChance`, `strengthMultiplier`, `useMultiplier`, `durationMultiplier` | Daily boosted effect/category selection and chat announcement. |
| `advancementRewards` | `enableAdvancementRewards`, `advancementRewardItem`, `advancementRewardCount` | Item reward given for Coupon Codes advancement completion. |
| `containers` | `allowCouponsInPouches`, `allowCouponsInShulkerBoxes`, `containerSearchDepth` | Whether coupon lookup can search pouches/shulker boxes and how deeply. |
| `timed` | `allowInventoryActivation`, `allowPouchActivation`, `allowDurationStacking`, `maxActiveCoupons` | Timed coupon activation and active timed coupon limits. |
| `feedback` | `playActivationFeedback`, `playUseFeedback`, `useFeedbackCooldownTicks`, `activationParticleCount`, `useParticleCount` | Sounds/particles and feedback cooldowns. |
| `values` | `multiUseMinUses`, `multiUseDefaultUses`, `multiUseMaxUses`, `timedMinSeconds`, `timedDefaultSeconds`, `timedMaxSeconds`, `anvilMinimumExperienceCost`, `anvilMinimumMaterialCost`, `enchantingPercentPerRefundLevel`, `potionDurationExtensionTicks`, `consumeChanceCouponsOnFailedRoll`, `consumeDurabilityCouponsOnFailedRoll` | Random value ranges and effect-specific rules. |
| `rollWeights` | `common`, `uncommon`, `rare`, `epic` | Weights used when empty coupons or random coupon commands/trades roll a coupon. |
| `enabledEffects` | One boolean per effect ID | Disables every mode for an effect. |
| `enabledModes` | `single_use`, `uses`, `timed` | Disables every coupon in a mode. |
| `enabledCoupons` | One boolean per exact effect/mode pair | Disables one exact coupon item behavior and excludes it from generated loot/trades. |

### Important Defaults

| Key | Default |
| --- | --- |
| `general.discountValues` | `[10, 20, 25, 50]` |
| `dailyBoost.categoryBoostChance` | `10` |
| `dailyBoost.strengthMultiplier` | `2` |
| `dailyBoost.useMultiplier` | `2` |
| `dailyBoost.durationMultiplier` | `2` |
| `advancementRewards.advancementRewardItem` | `coupon_codes:empty_coupon` |
| `advancementRewards.advancementRewardCount` | `1` |
| `containers.allowCouponsInPouches` | `true` |
| `containers.allowCouponsInShulkerBoxes` | `false` |
| `containers.containerSearchDepth` | `1` |
| `timed.maxActiveCoupons` | `1` |
| `values.multiUseMinUses` / `multiUseDefaultUses` / `multiUseMaxUses` | `2` / `3` / `5` |
| `values.timedMinSeconds` / `timedDefaultSeconds` / `timedMaxSeconds` | `15` / `30` / `60` |
| `rollWeights.common` / `uncommon` / `rare` / `epic` | `100` / `40` / `12` / `3` |

### Daily Boost Announcement Placeholders

`dailyBoost.announcementMessage` supports Minecraft-style `&` formatting codes and these placeholders:

```text
{effect}
{boost}
{boost_type}
{category}
{strength}
{uses}
{duration}
```

### Client Config

| Key | Default | Meaning |
| --- | --- | --- |
| `showTimedCouponBossBar` | `true` | Locally renders Coupon Codes timed-effect boss bars when the server sends them. |
| `showCouponIconOverlays` | `true` | Shows small effect icons over coupon item stacks. |

