package de.doomedartemis.couponcodes.common.item;

import de.doomedartemis.couponcodes.common.advancement.CouponCriteria;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

public class CouponPouchItem extends Item {
    public static final int SLOT_COUNT = 27;
    private static final CustomModelData OPEN_MODEL_DATA = new CustomModelData(1);
    private static final String AUTO_ACTIVATION_KEY = "AutoActivation";

    public CouponPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!CouponConfig.areCouponPouchesEnabled()) {
            tooltip.add(Component.translatable("item.coupon_codes.coupon_pouch.disabled").withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(Component.translatable("item.coupon_codes.coupon_pouch.count", occupiedSlots(stack), SLOT_COUNT)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.coupon_codes.coupon_pouch.auto_activation." + (isAutoActivationEnabled(stack) ? "on" : "off"))
                .withStyle(isAutoActivationEnabled(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable("item.coupon_codes.coupon_pouch.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!CouponConfig.areCouponPouchesEnabled()) {
            return InteractionResultHolder.fail(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, stack);
        }

        return InteractionResultHolder.success(stack);
    }

    public static boolean open(ServerPlayer player, ItemStack stack) {
        if (!CouponConfig.areCouponPouchesEnabled() || !(stack.getItem() instanceof CouponPouchItem)) {
            return false;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> CouponPouchMenu.create(containerId, inventory, stack),
                Component.translatable("container.coupon_codes.coupon_pouch")
        ));
        playOpenSound(player);
        CouponCriteria.triggerPouchOpened(player);
        return true;
    }

    public static void setOpen(ItemStack stack, boolean open) {
        if (open) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, OPEN_MODEL_DATA);
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    public static boolean isAutoActivationEnabled(ItemStack stack) {
        return !tag(stack).contains(AUTO_ACTIVATION_KEY) || tag(stack).getBoolean(AUTO_ACTIVATION_KEY);
    }

    public static void setAutoActivationEnabled(ItemStack stack, boolean enabled) {
        CompoundTag tag = tag(stack);
        tag.putBoolean(AUTO_ACTIVATION_KEY, enabled);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static boolean toggleAutoActivation(ItemStack stack) {
        boolean enabled = !isAutoActivationEnabled(stack);
        setAutoActivationEnabled(stack, enabled);
        return enabled;
    }

    public static void playCloseSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.65F, 0.92F);
    }

    private static void playOpenSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.65F, 1.08F);
    }

    private static int occupiedSlots(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents == ItemContainerContents.EMPTY) {
            return 0;
        }

        NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(stacks);
        int count = 0;
        for (ItemStack storedStack : stacks) {
            if (!storedStack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
