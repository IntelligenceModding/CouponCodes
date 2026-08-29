package de.doomedartemis.couponcodes.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponDailyBoost;
import de.doomedartemis.couponcodes.common.coupon.CouponCategory;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

public final class CouponCommands {
    private static final int MAX_COMMAND_USES = Integer.MAX_VALUE;
    private static final int MAX_COMMAND_TIMED_SECONDS = Integer.MAX_VALUE / 20;

    private static final String[] CATEGORY_NAMES = Arrays.stream(CouponCategory.values())
            .map(CouponCategory::id)
            .toArray(String[]::new);
    private static final String[] EFFECT_NAMES = Arrays.stream(CouponEffectType.values())
            .map(CouponEffectType::commandName)
            .toArray(String[]::new);

    private CouponCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("couponcodes")
                .requires(source -> CouponConfig.areCommandsEnabled())
                .then(Commands.literal("dailyboost")
                        .executes(CouponCommands::showDailyBoost))
                .then(Commands.literal("categories")
                        .executes(CouponCommands::showCategories))
                .then(Commands.literal("effects")
                        .executes(context -> showEffects(context, null))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(CATEGORY_NAMES, builder))
                                .executes(context -> showEffects(context, parseCategoryArgument(context)))))
                .then(Commands.literal("inspect")
                        .executes(CouponCommands::inspectSelf)
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(CouponCommands::inspectPlayers)))
                .then(Commands.literal("best")
                        .then(bestCouponArguments()))
                .then(Commands.literal("activetimed")
                        .executes(CouponCommands::showOwnActiveTimedCoupon)
                        .then(Commands.literal("category")
                                .then(activeTimedCategoryArguments()))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(CouponCommands::showActiveTimedCoupons)))
                .then(Commands.literal("give")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(effectArguments()))
                .then(Commands.literal("givecategory")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(categoryGiveArguments()))
                .then(Commands.literal("giveall")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(allMode("once", CouponMode.SINGLE_USE, 1, 1))
                                .then(allMode("multi", CouponMode.USES, 1, MAX_COMMAND_USES))
                                .then(allMode("timed", CouponMode.TIMED, 1, MAX_COMMAND_TIMED_SECONDS))
                                .then(Commands.argument("discountpercent", IntegerArgumentType.integer(1, 95))
                                        .executes(context -> giveAllCoupons(
                                                context,
                                                IntegerArgumentType.getInteger(context, "discountpercent"),
                                                1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> giveAllCoupons(
                                                        context,
                                                        IntegerArgumentType.getInteger(context, "discountpercent"),
                                                        IntegerArgumentType.getInteger(context, "count")))))))
                .then(Commands.literal("giverandom")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("category")
                                .then(randomCategoryArguments()))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> giveRandomCoupons(context, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                                        .executes(context -> giveRandomCoupons(context, IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("giveempty")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> giveSimpleItem(context, ModItems.EMPTY_COUPON.get().getDefaultInstance(), "empty coupon", 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                                        .executes(context -> giveSimpleItem(context, ModItems.EMPTY_COUPON.get().getDefaultInstance(), "empty coupon", IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("givepouch")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> giveSimpleItem(context, ModItems.COUPON_POUCH.get().getDefaultInstance(), "coupon pouch", 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveSimpleItem(context, ModItems.COUPON_POUCH.get().getDefaultInstance(), "coupon pouch", IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("cleartimed")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(clearTimedArguments())));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> effectArguments() {
        var targets = Commands.argument("targets", EntityArgument.players());
        targets.then(allCouponsArguments());
        for (CouponEffectType effect : CouponEffectType.values()) {
            targets.then(effect(effect.commandName(), effect));
        }
        return targets;
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> allCouponsArguments() {
        return Commands.literal("all")
                .then(allMode("once", CouponMode.SINGLE_USE, 1, 1))
                .then(allMode("multi", CouponMode.USES, 1, MAX_COMMAND_USES))
                .then(allMode("timed", CouponMode.TIMED, 1, MAX_COMMAND_TIMED_SECONDS))
                .then(Commands.argument("discountpercent", IntegerArgumentType.integer(1, 95))
                        .executes(context -> giveAllCoupons(
                                context,
                                IntegerArgumentType.getInteger(context, "discountpercent"),
                                1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> giveAllCoupons(
                                        context,
                                        IntegerArgumentType.getInteger(context, "discountpercent"),
                                        IntegerArgumentType.getInteger(context, "count")))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> bestCouponArguments() {
        var effectArgument = Commands.argument("effect", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(EFFECT_NAMES, builder))
                .executes(context -> showOwnBestCoupon(context, parseEffectArgument(context)));
        effectArgument.then(Commands.argument("targets", EntityArgument.players())
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> showBestCoupons(context, parseEffectArgument(context))));
        return effectArgument;
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> categoryGiveArguments() {
        var targets = Commands.argument("targets", EntityArgument.players());
        for (CouponCategory category : CouponCategory.values()) {
            targets.then(category(category.id(), category));
        }
        return targets;
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> randomCategoryArguments() {
        return Commands.argument("category", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(CATEGORY_NAMES, builder))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> giveRandomCoupons(context, parseCategoryArgument(context), 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                                .executes(context -> giveRandomCoupons(context, parseCategoryArgument(context), IntegerArgumentType.getInteger(context, "count")))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> activeTimedCategoryArguments() {
        return Commands.argument("category", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(CATEGORY_NAMES, builder))
                .executes(context -> showOwnActiveTimedCoupon(context, parseCategoryArgument(context)))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> showActiveTimedCoupons(context, parseCategoryArgument(context))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> clearTimedArguments() {
        var targets = Commands.argument("targets", EntityArgument.players())
                .executes(context -> clearTimedCoupons(context, (CouponEffectType) null));
        for (CouponEffectType effect : CouponEffectType.values()) {
            targets.then(Commands.literal(effect.commandName())
                    .executes(context -> clearTimedCoupons(context, effect)));
        }
        targets.then(Commands.literal("category")
                .then(clearTimedCategoryArguments()));
        return targets;
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> clearTimedCategoryArguments() {
        return Commands.argument("category", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(CATEGORY_NAMES, builder))
                .executes(context -> clearTimedCoupons(context, parseCategoryArgument(context)));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> effect(String name, CouponEffectType effect) {
        return Commands.literal(name)
                .then(mode("once", effect, CouponMode.SINGLE_USE, 1, 1))
                .then(mode("multi", effect, CouponMode.USES, 1, MAX_COMMAND_USES))
                .then(mode("timed", effect, CouponMode.TIMED, 1, MAX_COMMAND_TIMED_SECONDS));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> category(String name, CouponCategory category) {
        return Commands.literal(name)
                .then(mode("once", category, CouponMode.SINGLE_USE, 1, 1))
                .then(mode("multi", category, CouponMode.USES, 1, MAX_COMMAND_USES))
                .then(mode("timed", category, CouponMode.TIMED, 1, MAX_COMMAND_TIMED_SECONDS));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> mode(
            String name,
            CouponEffectType effect,
            CouponMode mode,
            int minValue,
            int maxValue
    ) {
        var discountArgument = Commands.argument("discountpercent", IntegerArgumentType.integer(1, 95))
                .executes(context -> giveCoupons(
                        context,
                        effect,
                        mode,
                        IntegerArgumentType.getInteger(context, "discountpercent"),
                        defaultValue(effect, mode),
                        1));

        if (mode == CouponMode.SINGLE_USE) {
            discountArgument.then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                    .executes(context -> giveCoupons(
                            context,
                            effect,
                            mode,
                            IntegerArgumentType.getInteger(context, "discountpercent"),
                            defaultValue(effect, mode),
                            IntegerArgumentType.getInteger(context, "count"))));
        } else {
            String valueArgumentName = mode == CouponMode.TIMED ? "seconds" : "uses";
            discountArgument.then(Commands.argument(valueArgumentName, IntegerArgumentType.integer(minValue, maxValue))
                    .executes(context -> giveCoupons(
                            context,
                            effect,
                            mode,
                            IntegerArgumentType.getInteger(context, "discountpercent"),
                            IntegerArgumentType.getInteger(context, valueArgumentName),
                            1))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                            .executes(context -> giveCoupons(
                                    context,
                                    effect,
                                    mode,
                                    IntegerArgumentType.getInteger(context, "discountpercent"),
                                    IntegerArgumentType.getInteger(context, valueArgumentName),
                                    IntegerArgumentType.getInteger(context, "count")))));
        }

        return Commands.literal(name).then(discountArgument);
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> mode(
            String name,
            CouponCategory category,
            CouponMode mode,
            int minValue,
            int maxValue
    ) {
        var discountArgument = Commands.argument("discountpercent", IntegerArgumentType.integer(1, 95))
                .executes(context -> giveCategoryCoupons(
                        context,
                        category,
                        mode,
                        IntegerArgumentType.getInteger(context, "discountpercent"),
                        null,
                        1));

        if (mode == CouponMode.SINGLE_USE) {
            discountArgument.then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                    .executes(context -> giveCategoryCoupons(
                            context,
                            category,
                            mode,
                            IntegerArgumentType.getInteger(context, "discountpercent"),
                            null,
                            IntegerArgumentType.getInteger(context, "count"))));
        } else {
            String valueArgumentName = mode == CouponMode.TIMED ? "seconds" : "uses";
            discountArgument.then(Commands.argument(valueArgumentName, IntegerArgumentType.integer(minValue, maxValue))
                    .executes(context -> giveCategoryCoupons(
                            context,
                            category,
                            mode,
                            IntegerArgumentType.getInteger(context, "discountpercent"),
                            IntegerArgumentType.getInteger(context, valueArgumentName),
                            1))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                            .executes(context -> giveCategoryCoupons(
                                    context,
                                    category,
                                    mode,
                                    IntegerArgumentType.getInteger(context, "discountpercent"),
                                    IntegerArgumentType.getInteger(context, valueArgumentName),
                                    IntegerArgumentType.getInteger(context, "count")))));
        }

        return Commands.literal(name).then(discountArgument);
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> allMode(
            String name,
            CouponMode mode,
            int minValue,
            int maxValue
    ) {
        var discountArgument = Commands.argument("discountpercent", IntegerArgumentType.integer(1, 95))
                .executes(context -> giveAllCoupons(
                        context,
                        mode,
                        IntegerArgumentType.getInteger(context, "discountpercent"),
                        null,
                        1));

        if (mode == CouponMode.SINGLE_USE) {
            discountArgument.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                    .executes(context -> giveAllCoupons(
                            context,
                            mode,
                            IntegerArgumentType.getInteger(context, "discountpercent"),
                            null,
                            IntegerArgumentType.getInteger(context, "count"))));
        } else {
            String valueArgumentName = mode == CouponMode.TIMED ? "seconds" : "uses";
            discountArgument.then(Commands.argument(valueArgumentName, IntegerArgumentType.integer(minValue, maxValue))
                    .executes(context -> giveAllCoupons(
                            context,
                            mode,
                            IntegerArgumentType.getInteger(context, "discountpercent"),
                            IntegerArgumentType.getInteger(context, valueArgumentName),
                            1))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                            .executes(context -> giveAllCoupons(
                                    context,
                                    mode,
                                    IntegerArgumentType.getInteger(context, "discountpercent"),
                                    IntegerArgumentType.getInteger(context, valueArgumentName),
                                    IntegerArgumentType.getInteger(context, "count")))));
        }

        return Commands.literal(name).then(discountArgument);
    }

    private static int giveCoupons(CommandContext<CommandSourceStack> context, CouponEffectType effect, CouponMode mode, int discountPercent, int value, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!CouponConfig.areCommandsEnabled()) {
            context.getSource().sendFailure(Component.literal("Coupon Codes commands are disabled in the config."));
            return 0;
        }

        if (!CouponConfig.isCouponEnabled(effect, mode)) {
            context.getSource().sendFailure(Component.literal(format(effect, mode, discountPercent, value) + " coupon is disabled in the config."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            for (int i = 0; i < count; i++) {
                ItemStack coupon = new ItemStack(ModItems.couponItem(effect, mode).get());
                CouponData.set(coupon, mode, discountPercent, value, false);
                giveOrDrop(target, coupon);
            }
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + count + " " + format(effect, mode, discountPercent, value) + " coupon(s) to " + targets.size() + " player(s)."),
                true
        );
        return targets.size() * count;
    }

    private static int giveCategoryCoupons(CommandContext<CommandSourceStack> context, CouponCategory category, CouponMode mode, int discountPercent, Integer value, int count) throws CommandSyntaxException {
        if (!CouponConfig.areCommandsEnabled()) {
            context.getSource().sendFailure(Component.literal("Coupon Codes commands are disabled in the config."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int given = 0;

        for (ServerPlayer target : targets) {
            for (CouponEffectType effect : CouponEffectType.values()) {
                if (effect.category() != category || !CouponConfig.isCouponEnabled(effect, mode)) {
                    continue;
                }
                int couponValue = value == null
                        ? defaultValue(effect, mode)
                        : value;
                for (int i = 0; i < count; i++) {
                    ItemStack coupon = new ItemStack(ModItems.couponItem(effect, mode).get());
                    CouponData.set(coupon, mode, discountPercent, couponValue, false);
                    giveOrDrop(target, coupon);
                    given++;
                }
            }
        }

        if (given <= 0) {
            context.getSource().sendFailure(Component.literal("No enabled " + readableName(category) + " " + modeName(mode) + " coupons are available."));
            return 0;
        }

        int givenCount = given;
        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + givenCount + " " + readableName(category) + " " + modeName(mode) + " coupon(s) to " + targets.size() + " player(s)."),
                true
        );
        return given;
    }

    private static int giveAllCoupons(CommandContext<CommandSourceStack> context, CouponMode mode, int discountPercent, Integer value, int count) throws CommandSyntaxException {
        if (!CouponConfig.areCommandsEnabled()) {
            context.getSource().sendFailure(Component.literal("Coupon Codes commands are disabled in the config."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int given = 0;

        for (ServerPlayer target : targets) {
            for (CouponEffectType effect : CouponEffectType.values()) {
                if (!CouponConfig.isCouponEnabled(effect, mode)) {
                    continue;
                }
                int couponValue = value == null
                        ? defaultValue(effect, mode)
                        : value;
                for (int i = 0; i < count; i++) {
                    ItemStack coupon = new ItemStack(ModItems.couponItem(effect, mode).get());
                    CouponData.set(coupon, mode, discountPercent, couponValue, false);
                    giveOrDrop(target, coupon);
                    given++;
                }
            }
        }

        if (given <= 0) {
            context.getSource().sendFailure(Component.literal("No enabled " + modeName(mode) + " coupons are available."));
            return 0;
        }

        int givenCount = given;
        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + givenCount + " configured " + modeName(mode) + " coupon(s) to " + targets.size() + " player(s)."),
                true
        );
        return given;
    }

    private static int giveAllCoupons(CommandContext<CommandSourceStack> context, int discountPercent, int count) throws CommandSyntaxException {
        if (!CouponConfig.areCommandsEnabled()) {
            context.getSource().sendFailure(Component.literal("Coupon Codes commands are disabled in the config."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int given = 0;

        for (ServerPlayer target : targets) {
            for (CouponEffectType effect : CouponEffectType.values()) {
                for (CouponMode mode : CouponMode.values()) {
                    if (!CouponConfig.isCouponEnabled(effect, mode)) {
                        continue;
                    }
                    for (int i = 0; i < count; i++) {
                        ItemStack coupon = new ItemStack(ModItems.couponItem(effect, mode).get());
                        CouponData.set(coupon, mode, discountPercent, defaultValue(effect, mode), false);
                        giveOrDrop(target, coupon);
                        given++;
                    }
                }
            }
        }

        int givenCount = given;
        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + givenCount + " configured coupon(s) to " + targets.size() + " player(s)."),
                true
        );
        return given;
    }

    private static int clearTimedCoupons(CommandContext<CommandSourceStack> context, CouponEffectType effect) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!CouponConfig.areCommandsEnabled()) {
            context.getSource().sendFailure(Component.literal("Coupon Codes commands are disabled in the config."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int cleared = 0;

        for (ServerPlayer target : targets) {
            cleared += effect == null
                    ? CouponData.clearActiveTimedCoupons(target)
                    : CouponData.clearActiveTimedCoupons(target, effect);
        }

        int clearedCount = cleared;
        context.getSource().sendSuccess(
                () -> Component.literal("Cleared " + clearedCount + " active timed coupon(s)."),
                true
        );
        return cleared;
    }

    private static int clearTimedCoupons(CommandContext<CommandSourceStack> context, CouponCategory category) throws CommandSyntaxException {
        if (category == null) {
            context.getSource().sendFailure(Component.literal("Unknown coupon category."));
            return 0;
        }
        if (!CouponConfig.areCommandsEnabled()) {
            context.getSource().sendFailure(Component.literal("Coupon Codes commands are disabled in the config."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int cleared = 0;

        for (ServerPlayer target : targets) {
            cleared += CouponData.clearActiveTimedCoupons(target, category);
        }

        int clearedCount = cleared;
        context.getSource().sendSuccess(
                () -> Component.literal("Cleared " + clearedCount + " active " + readableName(category) + " timed coupon(s)."),
                true
        );
        return cleared;
    }

    private static int showCategories(CommandContext<CommandSourceStack> context) {
        for (CouponCategory category : CouponCategory.values()) {
            context.getSource().sendSuccess(
                    () -> Component.literal(readableName(category) + ": " + effectNames(category)),
                    false
            );
        }
        return CouponCategory.values().length;
    }

    private static int showEffects(CommandContext<CommandSourceStack> context, CouponCategory category) {
        if (category == null && context.getNodes().stream().anyMatch(node -> "category".equals(node.getNode().getName()))) {
            context.getSource().sendFailure(Component.literal("Unknown coupon category."));
            return 0;
        }

        int shown = 0;
        for (CouponEffectType effect : CouponEffectType.values()) {
            if (category != null && effect.category() != category) {
                continue;
            }

            context.getSource().sendSuccess(
                    () -> Component.literal(readableName(effect) + " [" + readableName(effect.category()) + "]: " + enabledModes(effect)),
                    false
            );
            shown++;
        }
        return shown;
    }

    private static int inspectSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        sendInspection(context.getSource(), player);
        return 1;
    }

    private static int inspectPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            sendInspection(context.getSource(), target);
        }
        return targets.size();
    }

    private static void sendInspection(CommandSourceStack source, ServerPlayer target) {
        CouponData.CouponInventoryStats stats = CouponData.inventoryStats(target);
        source.sendSuccess(
                () -> Component.literal(target.getName().getString() + " coupons: "
                        + stats.carriedCoupons() + " carried, "
                        + stats.uninitializedCoupons() + " unrolled, "
                        + stats.activeTimedCoupons() + " active timed."),
                false
        );
        source.sendSuccess(
                () -> Component.literal("Categories: " + categoryCounts(stats)),
                false
        );
        source.sendSuccess(
                () -> Component.literal("Modes: once " + stats.modeCounts().getOrDefault(CouponMode.SINGLE_USE, 0)
                        + ", multi " + stats.modeCounts().getOrDefault(CouponMode.USES, 0)
                        + ", timed " + stats.modeCounts().getOrDefault(CouponMode.TIMED, 0)),
                false
        );
    }

    private static int showOwnBestCoupon(CommandContext<CommandSourceStack> context, CouponEffectType effect) throws CommandSyntaxException {
        if (effect == null) {
            context.getSource().sendFailure(Component.literal("Unknown coupon effect."));
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayerOrException();
        showBestCoupon(context.getSource(), player, effect);
        return 1;
    }

    private static int showBestCoupons(CommandContext<CommandSourceStack> context, CouponEffectType effect) throws CommandSyntaxException {
        if (effect == null) {
            context.getSource().sendFailure(Component.literal("Unknown coupon effect."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            showBestCoupon(context.getSource(), target, effect);
        }
        return targets.size();
    }

    private static void showBestCoupon(CommandSourceStack source, ServerPlayer target, CouponEffectType effect) {
        CouponData.CarriedCoupon carriedCoupon = CouponData.findBestCarriedCoupon(target, effect);
        if (carriedCoupon == null) {
            source.sendSuccess(
                    () -> Component.literal(target.getName().getString() + " has no active or usable " + readableName(effect) + " coupon."),
                    false
            );
            return;
        }

        CouponItem coupon = carriedCoupon.coupon();
        ItemStack stack = carriedCoupon.stack();
        String remaining = coupon.mode() == CouponMode.TIMED
                ? CouponData.secondsRemaining(stack, coupon, target.level()) + "s"
                : CouponData.usesRemaining(stack, coupon, target.level()) + " use(s)";
        source.sendSuccess(
                () -> Component.literal(target.getName().getString() + "'s best " + readableName(effect) + " coupon is "
                        + CouponData.discountPercent(stack, coupon, target.level()) + "% "
                        + modeName(coupon.mode()) + " with " + remaining + " remaining."),
                false
        );
    }

    private static int showDailyBoost(CommandContext<CommandSourceStack> context) {
        if (!CouponConfig.areDailyBoostsEnabled()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("Daily coupon boosts are inactive."),
                    false
            );
            return 0;
        }

        CouponDailyBoost.Boost boost = CouponDailyBoost.boost(context.getSource().getLevel());
        if (boost == null) {
            context.getSource().sendSuccess(
                    () -> Component.literal("No coupon type is eligible for today's daily boost."),
                    false
            );
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Today's boosted " + boost.typeName() + " is " + boost.displayName()
                        + " (strength x" + CouponConfig.dailyBoostStrengthMultiplier()
                        + ", uses x" + CouponConfig.dailyBoostUseMultiplier()
                        + ", duration x" + CouponConfig.dailyBoostDurationMultiplier() + ")."),
                false
        );
        return 1;
    }

    private static int showOwnActiveTimedCoupon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        showActiveTimedCoupon(context.getSource(), player);
        return 1;
    }

    private static int showOwnActiveTimedCoupon(CommandContext<CommandSourceStack> context, CouponCategory category) throws CommandSyntaxException {
        if (category == null) {
            context.getSource().sendFailure(Component.literal("Unknown coupon category."));
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayerOrException();
        showActiveTimedCoupon(context.getSource(), player, category);
        return 1;
    }

    private static int showActiveTimedCoupons(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            showActiveTimedCoupon(context.getSource(), target);
        }
        return targets.size();
    }

    private static int showActiveTimedCoupons(CommandContext<CommandSourceStack> context, CouponCategory category) throws CommandSyntaxException {
        if (category == null) {
            context.getSource().sendFailure(Component.literal("Unknown coupon category."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            showActiveTimedCoupon(context.getSource(), target, category);
        }
        return targets.size();
    }

    private static void showActiveTimedCoupon(CommandSourceStack source, ServerPlayer target) {
        ItemStack stack = CouponData.findBestActiveTimedCoupon(target);
        if (stack.isEmpty() || !(stack.getItem() instanceof CouponItem coupon)) {
            source.sendSuccess(
                    () -> Component.literal(target.getName().getString() + " has no active timed coupon."),
                    false
            );
            return;
        }

        source.sendSuccess(
                () -> Component.literal(target.getName().getString() + "'s strongest active timed coupon is "
                        + CouponData.discountPercent(stack, coupon, target.level()) + "% "
                        + readableName(coupon.effect()) + " with "
                        + CouponData.secondsRemaining(stack, coupon, target.level()) + "s remaining."),
                false
        );
    }

    private static void showActiveTimedCoupon(CommandSourceStack source, ServerPlayer target, CouponCategory category) {
        ItemStack stack = CouponData.findBestActiveTimedCoupon(target, category);
        if (stack.isEmpty() || !(stack.getItem() instanceof CouponItem coupon)) {
            source.sendSuccess(
                    () -> Component.literal(target.getName().getString() + " has no active " + readableName(category) + " timed coupon."),
                    false
            );
            return;
        }

        source.sendSuccess(
                () -> Component.literal(target.getName().getString() + "'s strongest active " + readableName(category) + " timed coupon is "
                        + CouponData.discountPercent(stack, coupon, target.level()) + "% "
                        + readableName(coupon.effect()) + " with "
                        + CouponData.secondsRemaining(stack, coupon, target.level()) + "s remaining."),
                false
        );
    }

    private static int giveRandomCoupons(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        return giveRandomCoupons(context, null, count);
    }

    private static int giveRandomCoupons(CommandContext<CommandSourceStack> context, CouponCategory category, int count) throws CommandSyntaxException {
        if (category == null && context.getNodes().stream().anyMatch(node -> "category".equals(node.getNode().getName()))) {
            context.getSource().sendFailure(Component.literal("Unknown coupon category."));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int given = 0;

        for (ServerPlayer target : targets) {
            for (int i = 0; i < count; i++) {
                Optional<DeferredItem<CouponItem>> rolledCoupon = ModItems.randomCoupon(target.getRandom(), category);
                if (rolledCoupon.isEmpty()) {
                    continue;
                }

                ItemStack coupon = new ItemStack(rolledCoupon.get().get());
                if (coupon.getItem() instanceof CouponItem couponItem) {
                    CouponData.initializeIfNeeded(coupon, couponItem, target.getRandom());
                }
                giveOrDrop(target, coupon);
                given++;
            }
        }

        int givenCount = given;
        String categoryName = category == null ? "random" : "random " + readableName(category);
        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + givenCount + " " + categoryName + " coupon(s) to " + targets.size() + " player(s)."),
                true
        );
        return given;
    }

    private static int giveSimpleItem(CommandContext<CommandSourceStack> context, ItemStack stack, String itemName, int count) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            giveStackCount(target, stack, count);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + count + " " + itemName + "(s) to " + targets.size() + " player(s)."),
                true
        );
        return targets.size() * count;
    }

    private static String format(CouponEffectType effect, CouponMode mode, int discountPercent, int value) {
        String effectName = effect.commandName();
        String modeName = mode.commandName();
        if (mode == CouponMode.TIMED) {
            return discountPercent + "% " + effectName + " " + modeName + " for " + value + "s";
        }
        if (mode == CouponMode.USES) {
            return discountPercent + "% " + effectName + " " + modeName + " with " + value + " uses";
        }
        return discountPercent + "% " + effectName + " " + modeName;
    }

    private static CouponCategory parseCategoryArgument(CommandContext<CommandSourceStack> context) {
        try {
            return CouponCategory.valueOf(StringArgumentType.getString(context, "category").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static CouponEffectType parseEffectArgument(CommandContext<CommandSourceStack> context) {
        String value = StringArgumentType.getString(context, "effect");
        for (CouponEffectType effect : CouponEffectType.values()) {
            if (effect.commandName().equals(value)) {
                return effect;
            }
        }
        return null;
    }

    private static String modeName(CouponMode mode) {
        return mode.commandName();
    }

    private static int defaultValue(CouponEffectType effect, CouponMode mode) {
        return switch (mode) {
            case SINGLE_USE -> 1;
            case USES -> CouponConfig.multiUseDefaultUses(effect);
            case TIMED -> CouponConfig.timedDefaultSeconds(effect);
        };
    }

    private static void giveStackCount(ServerPlayer target, ItemStack stack, int count) {
        int remaining = count;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, stack.getMaxStackSize());
            giveOrDrop(target, stack.copyWithCount(stackSize));
            remaining -= stackSize;
        }
    }

    private static void giveOrDrop(ServerPlayer target, ItemStack stack) {
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
    }

    private static String effectNames(CouponCategory category) {
        StringBuilder names = new StringBuilder();
        for (CouponEffectType effect : CouponEffectType.values()) {
            if (effect.category() != category) {
                continue;
            }
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(readableName(effect));
        }
        return names.toString();
    }

    private static String enabledModes(CouponEffectType effect) {
        StringBuilder modes = new StringBuilder();
        for (CouponMode mode : CouponMode.values()) {
            if (!CouponConfig.isCouponEnabled(effect, mode)) {
                continue;
            }
            if (!modes.isEmpty()) {
                modes.append(", ");
            }
            modes.append(mode.commandName());
        }
        return modes.isEmpty() ? "disabled" : modes.toString();
    }

    private static String categoryCounts(CouponData.CouponInventoryStats stats) {
        StringBuilder counts = new StringBuilder();
        for (CouponCategory category : CouponCategory.values()) {
            if (!counts.isEmpty()) {
                counts.append(", ");
            }
            counts.append(readableName(category)).append(' ')
                    .append(stats.categoryCounts().getOrDefault(category, 0));
        }
        return counts.toString();
    }

    private static String readableName(CouponEffectType effect) {
        return effect.displayName();
    }

    private static String readableName(CouponCategory category) {
        return category.displayName();
    }
}
