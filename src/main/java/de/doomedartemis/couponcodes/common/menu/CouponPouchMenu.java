package de.doomedartemis.couponcodes.common.menu;

import de.doomedartemis.couponcodes.common.advancement.CouponCriteria;
import de.doomedartemis.couponcodes.compat.curios.CuriosCompat;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponData;
import de.doomedartemis.couponcodes.common.coupon.CouponFeedback;
import de.doomedartemis.couponcodes.common.coupon.CouponMode;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import de.doomedartemis.couponcodes.common.item.EmptyCouponItem;
import de.doomedartemis.couponcodes.common.item.CouponPouchItem;
import de.doomedartemis.couponcodes.common.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class CouponPouchMenu extends AbstractContainerMenu {
    public static final int ROWS = 3;
    public static final int COLUMNS = 9;
    public static final int POUCH_SLOT_COUNT = ROWS * COLUMNS;

    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_SLOT_START = POUCH_SLOT_COUNT;
    private static final int HOTBAR_SLOT_START = PLAYER_SLOT_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int TOTAL_SLOT_COUNT = HOTBAR_SLOT_START + HOTBAR_SLOT_COUNT;

    private final ItemStack pouch;
    private final Container pouchContainer;
    private final boolean clientSideMenu;

    public static CouponPouchMenu create(int containerId, Inventory playerInventory, ItemStack pouch) {
        return new CouponPouchMenu(containerId, playerInventory, pouch, new PouchContainer(pouch), false);
    }

    public static CouponPouchMenu createClient(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        return new CouponPouchMenu(containerId, playerInventory, ItemStack.EMPTY, new SimpleContainer(POUCH_SLOT_COUNT), true);
    }

    public boolean isOpenFor(ItemStack stack) {
        return !clientSideMenu && stack == pouch;
    }

    public List<ItemStack> pouchItems() {
        List<ItemStack> stacks = new ArrayList<>(POUCH_SLOT_COUNT);
        for (int slot = 0; slot < POUCH_SLOT_COUNT; slot++) {
            stacks.add(pouchContainer.getItem(slot));
        }
        return stacks;
    }

    public void saveContents() {
        if (!clientSideMenu) {
            saveContainer(pouch, pouchContainer);
        }
    }

    private CouponPouchMenu(int containerId, Inventory playerInventory, ItemStack pouch, Container pouchContainer, boolean clientSideMenu) {
        super(ModMenus.COUPON_POUCH.get(), containerId);
        checkContainerSize(pouchContainer, POUCH_SLOT_COUNT);
        this.pouch = pouch;
        this.pouchContainer = pouchContainer;
        this.clientSideMenu = clientSideMenu;

        addPouchSlots(pouchContainer);
        addPlayerInventorySlots(playerInventory);
        pouchContainer.startOpen(playerInventory.player);
        if (!clientSideMenu) {
            CouponPouchItem.setOpen(pouch, true);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return clientSideMenu || (CouponConfig.areCouponPouchesEnabled() && isOpenPouchStillCarried(player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < POUCH_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, TOTAL_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isAllowedCoupon(stack)) {
            if (!moveItemStackTo(stack, 0, POUCH_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isRightClickTimedCouponActivation(slotId, button, clickType)) {
            if (!clientSideMenu && activateTimedCouponInPouch(slotId, player)) {
                return;
            }
            if (clientSideMenu) {
                return;
            }
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pouchContainer.stopOpen(player);
        if (!clientSideMenu) {
            saveContainer(pouch, pouchContainer);
            CouponPouchItem.setOpen(pouch, false);
            if (player instanceof ServerPlayer serverPlayer) {
                CouponCriteria.triggerPouchStocked(serverPlayer, countStoredCoupons());
            }
        }
    }

    private void addPouchSlots(Container container) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new CouponSlot(container, column + row * COLUMNS, 8 + column * 18, 18 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private boolean isOpenPouchStillCarried(Player player) {
        return !pouch.isEmpty()
                && pouch.getItem() instanceof CouponPouchItem
                && (player.getInventory().contains(pouch) || CuriosCompat.isEquipped(player, pouch));
    }

    private boolean isRightClickTimedCouponActivation(int slotId, int button, ClickType clickType) {
        if (slotId < 0
                || slotId >= POUCH_SLOT_COUNT
                || button != 1
                || clickType != ClickType.PICKUP
                || !getCarried().isEmpty()
                || !CouponConfig.allowTimedCouponPouchActivation()) {
            return false;
        }

        ItemStack stack = slots.get(slotId).getItem();
        if (!(stack.getItem() instanceof CouponItem coupon) || coupon.mode() != CouponMode.TIMED) {
            return false;
        }

        return !CouponData.isTimedActive(stack)
                && (!CouponData.isInitialized(stack) || !CouponData.isExpired(stack, coupon));
    }

    private boolean activateTimedCouponInPouch(int slotId, Player player) {
        ItemStack stack = slots.get(slotId).getItem();
        if (!(stack.getItem() instanceof CouponItem coupon)) {
            return false;
        }

        CouponData.initializeIfNeeded(stack, coupon, player.getRandom());
        if (!CouponData.activateTimed(player, stack, coupon)) {
            return false;
        }

        CouponFeedback.playActivation(player, coupon);
        slots.get(slotId).setChanged();
        saveContainer(pouch, pouchContainer);
        broadcastChanges();
        return true;
    }

    private static void loadContainer(ItemStack pouch, Container container) {
        ItemContainerContents contents = pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stacks = NonNullList.withSize(POUCH_SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(stacks);
        for (int slot = 0; slot < POUCH_SLOT_COUNT; slot++) {
            container.setItem(slot, stacks.get(slot));
        }
    }

    private static void saveContainer(ItemStack pouch, Container container) {
        if (pouch.isEmpty()) {
            return;
        }

        NonNullList<ItemStack> stacks = NonNullList.withSize(POUCH_SLOT_COUNT, ItemStack.EMPTY);
        for (int slot = 0; slot < POUCH_SLOT_COUNT; slot++) {
            stacks.set(slot, container.getItem(slot));
        }
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
    }

    private static boolean isAllowedCoupon(ItemStack stack) {
        return stack.isEmpty()
                || (CouponConfig.allowCouponsInPouches() && (stack.getItem() instanceof CouponItem || stack.getItem() instanceof EmptyCouponItem));
    }

    private int countStoredCoupons() {
        int count = 0;
        for (int slot = 0; slot < POUCH_SLOT_COUNT; slot++) {
            ItemStack stack = pouchContainer.getItem(slot);
            if (stack.getItem() instanceof CouponItem || stack.getItem() instanceof EmptyCouponItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static class PouchContainer extends SimpleContainer {
        private final ItemStack pouch;
        private boolean loading;

        PouchContainer(ItemStack pouch) {
            super(POUCH_SLOT_COUNT);
            this.pouch = pouch;
            this.loading = true;
            loadContainer(pouch, this);
            this.loading = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) {
                saveContainer(pouch, this);
            }
        }
    }

    private static class CouponSlot extends Slot {
        CouponSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isAllowedCoupon(stack);
        }
    }
}
