# Coupon Codes Config

Coupon Codes has a server config for gameplay behavior and a client config for local rendering.

The server config is registered as a NeoForge server config. In a running world it is normally written as:

```text
<world>/serverconfig/coupon_codes-server.toml
```

The client config is normally written as:

```text
config/coupon_codes-client.toml
```

## Server Config Sections

| Section | Keys | What it controls |
| --- | --- | --- |
| `general` | `enableCoupons`, `enableEmptyCouponRolls`, `enableCouponPouches`, `enableCommands`, `showTimedCouponBossBar`, `discountValues` | Master switches, command availability, timed boss bars, and random discount percentages. |
| `dailyBoost` | `enableDailyBoosts`, `announceDailyBoosts`, `announcementMessage`, `categoryBoostChance`, `strengthMultiplier`, `useMultiplier`, `durationMultiplier` | Daily boosted effect/category selection and chat announcement. |
| `advancementRewards` | `enableAdvancementRewards`, `advancementRewardItem`, `advancementRewardCount` | Item reward given for Coupon Codes advancement completion. |
| `containers` | `allowCouponsInPouches`, `allowCouponsInShulkerBoxes`, `containerSearchDepth` | Whether coupon lookup can search pouches/shulker boxes and how deeply. |
| `timed` | `allowInventoryActivation`, `allowPouchActivation`, `allowDurationStacking`, `maxActiveCoupons` | Timed coupon activation and active timed coupon limits. |
| `feedback` | `playActivationFeedback`, `playUseFeedback`, `useFeedbackCooldownTicks`, `activationParticleCount`, `useParticleCount` | Sounds/particles and feedback cooldowns. |
| `values` | `multiUseMinUses`, `multiUseDefaultUses`, `multiUseMaxUses`, `timedMinSeconds`, `timedDefaultSeconds`, `timedMaxSeconds`, `multiUseCounts`, `timedSeconds`, `anvilMinimumExperienceCost`, `anvilMinimumMaterialCost`, `enchantingPercentPerRefundLevel`, `potionDurationExtensionTicks`, `consumeChanceCouponsOnFailedRoll`, `consumeDurabilityCouponsOnFailedRoll` | Fallback value ranges, effect-specific use/duration ranges, and effect-specific rules. |
| `rollWeights` | `common`, `uncommon`, `rare`, `epic` | Weights used when empty coupons, random coupon commands, or random coupon trades roll a coupon. |
| `enabledEffects` | One boolean per effect ID | Disables every mode for an effect. |
| `enabledModes` | `single_use`, `uses`, `timed` | Disables every coupon in a mode. |
| `enabledCoupons` | One boolean per exact effect/mode pair | Disables one exact coupon item behavior and excludes it from generated loot/trades. |

## Important Defaults

| Key | Default |
| --- | --- |
| `general.enableCoupons` | `true` |
| `general.enableEmptyCouponRolls` | `true` |
| `general.enableCouponPouches` | `true` |
| `general.enableCommands` | `true` |
| `general.showTimedCouponBossBar` | `true` |
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
| `timed.allowInventoryActivation` | `true` |
| `timed.allowPouchActivation` | `true` |
| `timed.allowDurationStacking` | `true` |
| `timed.maxActiveCoupons` | `1` |
| `feedback.playActivationFeedback` | `true` |
| `feedback.playUseFeedback` | `true` |
| `feedback.useFeedbackCooldownTicks` | `20` |
| `feedback.activationParticleCount` | `30` |
| `feedback.useParticleCount` | `12` |
| `values.multiUseMinUses` / `multiUseDefaultUses` / `multiUseMaxUses` | `2` / `3` / `5` fallback |
| `values.timedMinSeconds` / `timedDefaultSeconds` / `timedMaxSeconds` | `15` / `30` / `60` fallback |
| `values.anvilMinimumExperienceCost` | `1` |
| `values.anvilMinimumMaterialCost` | `1` |
| `values.enchantingPercentPerRefundLevel` | `25` |
| `values.potionDurationExtensionTicks` | `1` |
| `values.consumeChanceCouponsOnFailedRoll` | `true` |
| `values.consumeDurabilityCouponsOnFailedRoll` | `true` |
| `rollWeights.common` / `uncommon` / `rare` / `epic` | `100` / `40` / `12` / `3` |

## Effect-Specific Value Defaults

Reusable and timed coupons use effect-specific triples in the server config:

```text
values.multiUseCounts.<effect> = [min, default, max]
values.timedSeconds.<effect> = [min, default, max]
```

Random rolls choose between the effect's min and max. Commands that omit `<uses>` or `<seconds>` use the effect's default. Commands that provide `<uses>` or `<seconds>` are clamped to the target effect's min and max, so high-impact coupons such as death protection cannot be created with extremely long durations by accident.

| Effect | Reusable uses | Timed seconds |
| --- | --- | --- |
| `durability` | `[8, 12, 16]` | `[300, 600, 900]` |
| `food` | `[6, 9, 12]` | `[300, 480, 720]` |
| `arrow` | `[5, 8, 12]` | `[180, 300, 480]` |
| `bone_meal` | `[5, 8, 12]` | `[180, 300, 480]` |
| `fishing` | `[4, 6, 9]` | `[240, 420, 600]` |
| `elytra_glide` | `[4, 6, 9]` | `[240, 420, 600]` |
| `mending` | `[4, 6, 9]` | `[240, 420, 600]` |
| `rocket` | `[3, 5, 8]` | `[150, 240, 420]` |
| `ender_pearl` | `[3, 5, 8]` | `[150, 240, 420]` |
| `potion_duration` | `[3, 5, 8]` | `[150, 240, 420]` |
| `villager_trade` | `[3, 4, 6]` | `[120, 180, 300]` |
| `brewing_ingredient` | `[3, 4, 6]` | `[120, 180, 300]` |
| `anvil_experience` | `[2, 3, 5]` | `[90, 150, 240]` |
| `tool_repair` | `[2, 3, 5]` | `[90, 150, 240]` |
| `repair_material` | `[2, 3, 5]` | `[90, 150, 240]` |
| `enchanting_experience` | `[2, 3, 4]` | `[60, 90, 180]` |
| `smithing_template` | `[2, 3, 4]` | `[60, 90, 180]` |
| `fall_damage` | `[2, 3, 4]` | `[60, 90, 180]` |
| `villager_restock` | `[2, 3, 4]` | `[60, 90, 180]` |
| `totem` | `[2, 2, 3]` | `[30, 60, 120]` |
| `death_drop` | `[2, 2, 3]` | `[30, 60, 120]` |

## Daily Boost Announcement

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

Default message:

```text
&6[Coupons]&r &eDaily boost: &b{boost} &7(strength x{strength}, uses x{uses}, duration x{duration})
```

## Command Interactions

`general.enableCommands = false` disables the whole `/coupon_codes` command tree.

`general.enableCoupons = false` prevents coupon effects and blocks exact coupon generation through the give commands.

`enabledEffects`, `enabledModes`, and `enabledCoupons` are checked when giving exact, category, all, and random coupons.

Random coupon commands use `rollWeights.common`, `rollWeights.uncommon`, `rollWeights.rare`, and `rollWeights.epic`.

Exact command-created coupons use the discount, uses, and seconds supplied by the command. Reusable uses and timed seconds are clamped to the target effect's `values.multiUseCounts.<effect>` or `values.timedSeconds.<effect>` range.

If a command omits reusable uses or timed seconds, it uses the target effect's configured default. Category and all-coupon commands apply each effect's own default.

## Datapack Interactions

Server config gates are checked at runtime. If a coupon, mode, empty coupon roll, or pouch is disabled, datapack loot/trades that would create it are skipped.

Datapack `coupon_set`, `coupon`, `category_coupon_set`, and `category_coupon` entries are filtered by `enabledEffects`, `enabledModes`, and `enabledCoupons`.

Datapack `item` entries for `coupon_codes:empty_coupon` require `general.enableEmptyCouponRolls = true`.

Datapack `item` entries for coupon pouches require `general.enableCouponPouches = true`.

Coupon Codes advancement rewards are controlled by `advancementRewards.enableAdvancementRewards`, `advancementRewards.advancementRewardItem`, and `advancementRewards.advancementRewardCount`.

## Client Config

| Key | Default | Meaning |
| --- | --- | --- |
| `showTimedCouponBossBar` | `true` | Locally renders Coupon Codes timed-effect boss bars when the server sends them. |
| `showCouponIconOverlays` | `true` | Shows small effect icons over coupon item stacks. |
