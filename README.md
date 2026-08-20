
Coupon Codes
=======

Coupon Codes is a NeoForge mod that adds collectible coupons for discounts, refunds, timed bonuses, coupon pouches, configurable loot drops, trader offers, and advancements.

Development
==========

Open the project in IntelliJ IDEA or Eclipse and use the Gradle tasks from this repository. If dependencies are missing, run `gradlew --refresh-dependencies`; use `gradlew clean` only to clear generated build output.

Coupon Loot Datapacks
==========

Coupon chest loot and enemy drops are controlled by JSON files in `data/<namespace>/coupon_loot/*.json`.
Files can contain one profile or a `profiles` array. Add new files to append more drops, or override the built-in
defaults by replacing:

- `data/coupon_codes/coupon_loot/default_chests.json`
- `data/coupon_codes/coupon_loot/default_entities.json`

Example:

```json
{
  "profiles": [
    {
      "loot_tables": ["minecraft:chests/simple_dungeon"],
      "entries": [
        {"type": "coupon_set", "effect": "durability", "weight": 10},
        {"type": "coupon", "effect": "death_drop", "mode": "once", "weight": 1},
        {"type": "category_coupon_set", "category": "equipment", "weight": 3},
        {"type": "category_coupon", "category": "combat", "mode": "uses", "weight": 2},
        {"type": "item", "item": "coupon_codes:empty_coupon", "weight": 4, "min_count": 1, "max_count": 2}
      ],
      "rolls": [
        {"chance": 0.12, "count": 1},
        {"chance_percent": 2, "min_rolls": 1, "max_rolls": 2}
      ]
    },
    {
      "entities": ["minecraft:evoker"],
      "entries": [
        {"type": "coupon", "effect": "totem", "mode": "once", "weight": 10},
        {"type": "coupon", "effect": "death_drop", "mode": "uses", "weight": 2}
      ],
      "rolls": [{"chance": 0.45}]
    }
  ]
}
```

Fields:

- `loot_tables`: vanilla or modded loot table ids to modify.
- `entities`: entity type ids that can drop coupons on death.
- `replace`: when true, clears earlier coupon rolls for the listed targets before adding this profile.
- `rolls`: each roll has `chance` from `0.0` to `1.0`, or `chance_percent`; `count`, `min_rolls`, and `max_rolls` control how many entries are picked when the roll succeeds.
- `entries`: weighted choices. Use `coupon_set` for all three modes of an effect, `coupon` for one exact coupon, `category_coupon_set` for a random coupon from one category, `category_coupon` for a random coupon from one category and mode, or `item` for empty coupons and pouches.
- Coupon modes accept `once`, `single_use`, `uses`, `multi`, `reusable`, or `timed`.
- Coupon categories accept `equipment`, `magic`, `trade`, `consumables`, `mobility`, or `combat`.

Coupon Commands
==========

Player-accessible commands:

- `/coupon_codes daily_boost`: shows today's boosted coupon type or category.
- `/coupon_codes categories`: lists coupon categories and their effects.
- `/coupon_codes effects [category]`: lists coupon effects and currently enabled modes.
- `/coupon_codes inspect`: summarizes your carried coupons, unrolled coupons, active timed coupons, categories, and modes.
- `/coupon_codes best <effect>`: shows your strongest usable coupon for one effect.
- `/coupon_codes active_timed`: shows your strongest active timed coupon.
- `/coupon_codes active_timed category <category>`: shows your strongest active timed coupon in one category.

Admin commands:

- `/coupon_codes inspect <targets>`: summarizes carried coupons for one or more players.
- `/coupon_codes best <effect> <targets>`: checks a player's strongest usable coupon for one effect.
- `/coupon_codes give <targets> <effect> <once|multi|timed> <discount_percent> ...`: gives exact coupons.
- `/coupon_codes give_category <targets> <category> <once|multi|timed> <discount_percent> ...`: gives every enabled coupon in a category and mode.
- `/coupon_codes give_all <targets> <discount_percent> [count]`: gives every enabled coupon.
- `/coupon_codes give_random <targets> [count]`: gives random enabled coupons.
- `/coupon_codes give_random category <category> <targets> [count]`: gives random enabled coupons from one category.
- `/coupon_codes give_empty <targets> [count]`: gives empty coupons.
- `/coupon_codes give_pouch <targets> [count]`: gives coupon pouches.
- `/coupon_codes clear_timed <targets> [effect]`: clears active timed coupons, optionally by effect.
- `/coupon_codes clear_timed <targets> category <category>`: clears active timed coupons by category.

Coupon Trade Datapacks
==========

Wandering trader coupon offers are controlled by JSON files in `data/<namespace>/coupon_trades/*.json`.
Villager profession coupon offers use the same folder. Override the built-in defaults by replacing:

- `data/coupon_codes/coupon_trades/wandering_trader.json`
- `data/coupon_codes/coupon_trades/villager_professions.json`

Example:

```json
{
  "generic_listings": 1,
  "rare_listings": 1,
  "generic": [
    {
      "type": "empty_coupon",
      "weight": 1,
      "emerald_cost": 5,
      "count": 1,
      "max_uses": 4,
      "xp": 2
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
      "xp": 8
    },
    {
      "type": "coupon",
      "effect": "totem",
      "mode": "once",
      "weight": 1,
      "emerald_cost": 40,
      "max_uses": 1,
      "xp": 10
    }
  ],
  "villagers": [
    {
      "profession": "minecraft:librarian",
      "level": 5,
      "listings": 1,
      "entries": [
        {"type": "coupon", "effect": "enchanting_experience", "mode": "uses", "weight": 4},
        {"type": "coupon", "effect": "anvil_experience", "mode": "uses", "weight": 2}
      ]
    }
  ]
}
```

Fields:

- `generic` and `rare`: offers added to the wandering trader's generic and rare trade pools.
- `generic_listings` and `rare_listings`: how many coupon trade entries are added to those pools.
- `villagers`: profession trade pools. Each entry uses `profession`, villager `level` from 1-5, `listings`, and weighted coupon `entries`.
- `replace`, `replace_generic`, `replace_rare`, `replace_professions`: clears earlier configured coupon trades before adding this file.
- Trade `type`: `empty_coupon`, `random_coupon`, `coupon`, or `item`.
- `weight`: relative chance inside the coupon trade pool.
- `emerald_cost`, `count`, `max_uses`, `xp`, and `price_multiplier`: standard trade values.
- `random_coupon` uses `costs.common`, `costs.uncommon`, `costs.rare`, and `costs.epic`.
- Villager profession coupon trades support `coupon` and `random_coupon`; their emerald cost is always clamped to 48-64, defaults to a random 48-64, and ignores villager discounts from curing, Hero of the Village, demand, or special prices.
