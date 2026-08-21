# Coupon Codes Commands

All commands are under:

```text
/coupon_codes
```

Commands that inspect only yourself are available to normal players. Commands that inspect other players, give items, or clear timed coupons require gamemaster permission level.

## Common Arguments

`<targets>` is a Minecraft player selector or player name.

`<discount_percent>` must be `1` to `95`.

`<count>` is how many stacks/items/coupons to create. If the target inventory is full, items are dropped at the target.

Coupon effects:

```text
durability
enchanting_experience
anvil_experience
tool_repair
villager_trade
villager_restock
brewing_ingredient
arrow
food
potion_duration
mending
totem
smithing_template
repair_material
bone_meal
fishing
rocket
ender_pearl
elytra_glide
fall_damage
death_drop
```

Coupon categories:

```text
equipment
magic
trade
consumables
mobility
combat
```

Command modes:

| Mode | Meaning |
| --- | --- |
| `once` | Single-use coupon. |
| `multi` | Reusable coupon with a use count. |
| `timed` | Timed coupon with a duration in seconds. |

For `multi`, `<uses>` accepts `1` to `64`.

For `timed`, `<seconds>` accepts `1` to `3600`.

The final value is still clamped by the target coupon effect's configured range. For example, a durability timed coupon can use a much longer duration than a death protection timed coupon.

## Player Information Commands

Show today's daily coupon boost:

```text
/coupon_codes daily_boost
```

Shows the boosted effect or category and its strength, use, and duration multipliers.

List categories and the effects in each category:

```text
/coupon_codes categories
```

List all coupon effects and available modes:

```text
/coupon_codes effects
/coupon_codes effects <category>
```

Inspect your own coupon inventory:

```text
/coupon_codes inspect
```

Shows carried coupons, unrolled coupons, active timed coupons, category counts, and mode counts.

Inspect other players:

```text
/coupon_codes inspect <targets>
```

Requires gamemaster permission.

Show the best usable or active coupon for an effect:

```text
/coupon_codes best <effect>
/coupon_codes best <effect> <targets>
```

Without targets, checks yourself. With targets, requires gamemaster permission. The command reports the strongest carried or active coupon for that effect and its remaining uses or seconds.

Show active timed coupons:

```text
/coupon_codes active_timed
/coupon_codes active_timed <targets>
/coupon_codes active_timed category <category>
/coupon_codes active_timed category <category> <targets>
```

Without targets, checks yourself. With targets, requires gamemaster permission. The category form reports the strongest active timed coupon in that category.

## Give Exact Coupons

Give one or more exact coupon effects.

Single-use:

```text
/coupon_codes give <targets> <effect> once <discount_percent>
/coupon_codes give <targets> <effect> once <discount_percent> <count>
```

Reusable:

```text
/coupon_codes give <targets> <effect> multi <discount_percent>
/coupon_codes give <targets> <effect> multi <discount_percent> <uses>
/coupon_codes give <targets> <effect> multi <discount_percent> <uses> <count>
```

Timed:

```text
/coupon_codes give <targets> <effect> timed <discount_percent>
/coupon_codes give <targets> <effect> timed <discount_percent> <seconds>
/coupon_codes give <targets> <effect> timed <discount_percent> <seconds> <count>
```

If `<uses>` or `<seconds>` is omitted, the command uses the target effect's configured default reusable use count or timed duration.

Examples:

```text
/coupon_codes give @p durability once 25
/coupon_codes give @a villager_trade multi 50 3
/coupon_codes give Steve fall_damage timed 20 60 2
```

## Give Category Coupons

Give every available coupon effect in a category for one mode.

When `<uses>` or `<seconds>` is omitted, every effect in the category receives its own configured default. When a value is supplied, it is clamped separately for each effect.

Single-use:

```text
/coupon_codes give_category <targets> <category> once <discount_percent>
/coupon_codes give_category <targets> <category> once <discount_percent> <count>
```

Reusable:

```text
/coupon_codes give_category <targets> <category> multi <discount_percent>
/coupon_codes give_category <targets> <category> multi <discount_percent> <uses>
/coupon_codes give_category <targets> <category> multi <discount_percent> <uses> <count>
```

Timed:

```text
/coupon_codes give_category <targets> <category> timed <discount_percent>
/coupon_codes give_category <targets> <category> timed <discount_percent> <seconds>
/coupon_codes give_category <targets> <category> timed <discount_percent> <seconds> <count>
```

Example:

```text
/coupon_codes give_category @p equipment multi 25 3
```

That gives each available equipment multi coupon to the target.

## Give All Coupons

Give every available exact coupon:

```text
/coupon_codes give_all <targets> <discount_percent>
/coupon_codes give_all <targets> <discount_percent> <count>
```

`<count>` is limited to `1` to `64` here. It applies per exact coupon, not to the total number of items.

Example:

```text
/coupon_codes give_all @p 10
```

## Give Random Coupons

Give random initialized coupons:

```text
/coupon_codes give_random <targets>
/coupon_codes give_random <targets> <count>
/coupon_codes give_random category <category> <targets>
/coupon_codes give_random category <category> <targets> <count>
```

`<count>` is limited to `1` to `2304`.

Examples:

```text
/coupon_codes give_random @p
/coupon_codes give_random @a 5
/coupon_codes give_random category combat @p 3
```

## Give Utility Items

Give empty coupons:

```text
/coupon_codes give_empty <targets>
/coupon_codes give_empty <targets> <count>
```

`<count>` is limited to `1` to `2304`.

Give the base coupon pouch:

```text
/coupon_codes give_pouch <targets>
/coupon_codes give_pouch <targets> <count>
```

`<count>` is limited to `1` to `64`. This command gives `coupon_codes:coupon_pouch`. Use vanilla `/give` for colored pouch item IDs.

## Clear Active Timed Coupons

Clear all active timed coupons from targets:

```text
/coupon_codes clear_timed <targets>
```

Clear one exact effect:

```text
/coupon_codes clear_timed <targets> <effect>
```

Clear one category:

```text
/coupon_codes clear_timed <targets> category <category>
```

Examples:

```text
/coupon_codes clear_timed @a
/coupon_codes clear_timed @p fall_damage
/coupon_codes clear_timed Steve category mobility
```

