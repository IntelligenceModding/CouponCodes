# Coupon Codes Commands

All commands are under:

```text
/couponcodes
```

Commands that inspect only yourself are available to normal players. Commands that inspect other players, give items, or clear timed coupons require gamemaster permission level.

## Common Arguments

`<targets>` is a Minecraft player selector or player name.

`<discountpercent>` must be `1` to `95`.

`<count>` is how many stacks/items/coupons to create. If the target inventory is full, items are dropped at the target.

Coupon effects:

```text
durability
enchantingexperience
anvilexperience
toolrepair
villagertrade
villagerrestock
arrow
food
potionduration
mending
totem
smithingtemplate
repairmaterial
bonemeal
fishing
rocket
enderpearl
elytraglide
falldamage
deathdrop
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

For `multi`, `<uses>` accepts `1` to `2147483647`.

For `timed`, `<seconds>` accepts `1` to `107374182`.

Explicit admin-supplied `<uses>` and `<seconds>` values are written directly to the coupon. Effect-specific configured ranges only control random rolls and omitted defaults.

## Player Information Commands

Show today's daily coupon boost:

```text
/couponcodes dailyboost
```

Shows the boosted effect or category and its strength, use, and duration multipliers.

List categories and the effects in each category:

```text
/couponcodes categories
```

List all coupon effects and available modes:

```text
/couponcodes effects
/couponcodes effects <category>
```

Inspect your own coupon inventory:

```text
/couponcodes inspect
```

Shows carried coupons, unrolled coupons, active timed coupons, category counts, and mode counts.

Inspect other players:

```text
/couponcodes inspect <targets>
```

Requires gamemaster permission.

Show the best usable or active coupon for an effect:

```text
/couponcodes best <effect>
/couponcodes best <effect> <targets>
```

Without targets, checks yourself. With targets, requires gamemaster permission. The command reports the strongest carried or active coupon for that effect and its remaining uses or seconds.

Show active timed coupons:

```text
/couponcodes activetimed
/couponcodes activetimed <targets>
/couponcodes activetimed category <category>
/couponcodes activetimed category <category> <targets>
```

Without targets, checks yourself. With targets, requires gamemaster permission. The category form reports the strongest active timed coupon in that category.

## Give Exact Coupons

Give one or more exact coupon effects.

Single-use:

```text
/couponcodes give <targets> <effect> once <discountpercent>
/couponcodes give <targets> <effect> once <discountpercent> <count>
```

Reusable:

```text
/couponcodes give <targets> <effect> multi <discountpercent>
/couponcodes give <targets> <effect> multi <discountpercent> <uses>
/couponcodes give <targets> <effect> multi <discountpercent> <uses> <count>
```

Timed:

```text
/couponcodes give <targets> <effect> timed <discountpercent>
/couponcodes give <targets> <effect> timed <discountpercent> <seconds>
/couponcodes give <targets> <effect> timed <discountpercent> <seconds> <count>
```

If `<uses>` or `<seconds>` is omitted, the command uses the target effect's configured default reusable use count or timed duration.

Examples:

```text
/couponcodes give @p durability once 25
/couponcodes give @a villagertrade multi 50 3
/couponcodes give Steve falldamage timed 20 60 2
```

## Give Category Coupons

Give every available coupon effect in a category for one mode.

When `<uses>` or `<seconds>` is omitted, every effect in the category receives its own configured default. When a value is supplied, every effect in the category receives that exact value.

Single-use:

```text
/couponcodes givecategory <targets> <category> once <discountpercent>
/couponcodes givecategory <targets> <category> once <discountpercent> <count>
```

Reusable:

```text
/couponcodes givecategory <targets> <category> multi <discountpercent>
/couponcodes givecategory <targets> <category> multi <discountpercent> <uses>
/couponcodes givecategory <targets> <category> multi <discountpercent> <uses> <count>
```

Timed:

```text
/couponcodes givecategory <targets> <category> timed <discountpercent>
/couponcodes givecategory <targets> <category> timed <discountpercent> <seconds>
/couponcodes givecategory <targets> <category> timed <discountpercent> <seconds> <count>
```

Example:

```text
/couponcodes givecategory @p equipment multi 25 3
```

That gives each available equipment multi coupon to the target.

## Give All Coupons

Give every available exact coupon:

```text
/couponcodes give <targets> all <discountpercent>
/couponcodes give <targets> all <discountpercent> <count>
/couponcodes giveall <targets> <discountpercent>
/couponcodes giveall <targets> <discountpercent> <count>
```

Give every available exact coupon for one mode:

```text
/couponcodes give <targets> all once <discountpercent>
/couponcodes give <targets> all once <discountpercent> <count>
/couponcodes give <targets> all multi <discountpercent>
/couponcodes give <targets> all multi <discountpercent> <uses>
/couponcodes give <targets> all multi <discountpercent> <uses> <count>
/couponcodes give <targets> all timed <discountpercent>
/couponcodes give <targets> all timed <discountpercent> <seconds>
/couponcodes give <targets> all timed <discountpercent> <seconds> <count>
/couponcodes giveall <targets> once <discountpercent>
/couponcodes giveall <targets> once <discountpercent> <count>
/couponcodes giveall <targets> multi <discountpercent>
/couponcodes giveall <targets> multi <discountpercent> <uses>
/couponcodes giveall <targets> multi <discountpercent> <uses> <count>
/couponcodes giveall <targets> timed <discountpercent>
/couponcodes giveall <targets> timed <discountpercent> <seconds>
/couponcodes giveall <targets> timed <discountpercent> <seconds> <count>
```

`/couponcodes give <targets> all ...` is an alias for `/couponcodes giveall ...`.

When `<uses>` or `<seconds>` is omitted from a mode-specific all command, every effect receives its own configured default.

`<count>` is limited to `1` to `64` here. It applies per exact coupon, not to the total number of items.

Example:

```text
/couponcodes give @p all 10
/couponcodes giveall @p 10
/couponcodes giveall @p timed 20 60
```

## Give Random Coupons

Give random initialized coupons:

```text
/couponcodes giverandom <targets>
/couponcodes giverandom <targets> <count>
/couponcodes giverandom category <category> <targets>
/couponcodes giverandom category <category> <targets> <count>
```

`<count>` is limited to `1` to `2304`.

Examples:

```text
/couponcodes giverandom @p
/couponcodes giverandom @a 5
/couponcodes giverandom category combat @p 3
```

## Give Utility Items

Give empty coupons:

```text
/couponcodes giveempty <targets>
/couponcodes giveempty <targets> <count>
```

`<count>` is limited to `1` to `2304`.

Give the base coupon pouch:

```text
/couponcodes givepouch <targets>
/couponcodes givepouch <targets> <count>
```

`<count>` is limited to `1` to `64`. This command gives `coupon_codes:coupon_pouch`. Use vanilla `/give` for colored pouch item IDs.

## Clear Active Timed Coupons

Clear all active timed coupons from targets:

```text
/couponcodes cleartimed <targets>
```

Clear one exact effect:

```text
/couponcodes cleartimed <targets> <effect>
```

Clear one category:

```text
/couponcodes cleartimed <targets> category <category>
```

Examples:

```text
/couponcodes cleartimed @a
/couponcodes cleartimed @p falldamage
/couponcodes cleartimed Steve category mobility
```

