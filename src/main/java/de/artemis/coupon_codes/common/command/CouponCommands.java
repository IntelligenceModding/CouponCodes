package de.artemis.coupon_codes.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.artemis.coupon_codes.common.coupon.CouponData;
import de.artemis.coupon_codes.common.coupon.CouponEffectType;
import de.artemis.coupon_codes.common.coupon.CouponMode;
import de.artemis.coupon_codes.common.item.CouponItem;
import de.artemis.coupon_codes.common.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Collection;
import java.util.Locale;

public final class CouponCommands {
    private CouponCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("coupon_codes")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(effect("durability", CouponEffectType.DURABILITY))
                                .then(effect("enchanting_experience", CouponEffectType.ENCHANTING_EXPERIENCE)))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> effect(String name, CouponEffectType effect) {
        return Commands.literal(name)
                .then(mode("once", effect, CouponMode.SINGLE_USE, 1, 1))
                .then(mode("multi", effect, CouponMode.USES, 1, 64))
                .then(mode("timed", effect, CouponMode.TIMED, 1, 86400));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> mode(
            String name,
            CouponEffectType effect,
            CouponMode mode,
            int minValue,
            int maxValue
    ) {
        return Commands.literal(name)
                .then(Commands.argument("discount_percent", IntegerArgumentType.integer(1, 95))
                        .executes(context -> giveCoupons(context, effect, mode, IntegerArgumentType.getInteger(context, "discount_percent"), 1))
                        .then(Commands.argument(mode == CouponMode.TIMED ? "seconds" : "uses", IntegerArgumentType.integer(minValue, maxValue))
                                .executes(context -> giveCoupons(
                                        context,
                                        effect,
                                        mode,
                                        IntegerArgumentType.getInteger(context, "discount_percent"),
                                        IntegerArgumentType.getInteger(context, mode == CouponMode.TIMED ? "seconds" : "uses")))));
    }

    private static int giveCoupons(CommandContext<CommandSourceStack> context, CouponEffectType effect, CouponMode mode, int discountPercent, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            ItemStack coupon = new ItemStack(couponItem(effect, mode).get());
            CouponData.set(coupon, mode, discountPercent, value, false);

            if (!target.getInventory().add(coupon)) {
                target.drop(coupon, false);
            }
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Gave " + targets.size() + " " + format(effect, mode, discountPercent, value) + " coupon(s)."),
                true
        );
        return targets.size();
    }

    private static DeferredItem<CouponItem> couponItem(CouponEffectType effect, CouponMode mode) {
        return switch (effect) {
            case DURABILITY -> switch (mode) {
                case SINGLE_USE -> ModItems.DURABILITY_ONCE_COUPON;
                case USES -> ModItems.DURABILITY_MULTI_COUPON;
                case TIMED -> ModItems.DURABILITY_TIMED_COUPON;
            };
            case ENCHANTING_EXPERIENCE -> switch (mode) {
                case SINGLE_USE -> ModItems.ENCHANTING_EXPERIENCE_ONCE_COUPON;
                case USES -> ModItems.ENCHANTING_EXPERIENCE_MULTI_COUPON;
                case TIMED -> ModItems.ENCHANTING_EXPERIENCE_TIMED_COUPON;
            };
        };
    }

    private static String format(CouponEffectType effect, CouponMode mode, int discountPercent, int value) {
        String effectName = effect.name().toLowerCase(Locale.ROOT);
        String modeName = switch (mode) {
            case SINGLE_USE -> "once";
            case USES -> "multi";
            case TIMED -> "timed";
        };
        if (mode == CouponMode.TIMED) {
            return discountPercent + "% " + effectName + " " + modeName + " for " + value + "s";
        }
        if (mode == CouponMode.USES) {
            return discountPercent + "% " + effectName + " " + modeName + " with " + value + " uses";
        }
        return discountPercent + "% " + effectName + " " + modeName;
    }
}
