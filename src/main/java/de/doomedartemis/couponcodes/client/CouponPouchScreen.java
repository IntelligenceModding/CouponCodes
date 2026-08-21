package de.doomedartemis.couponcodes.client;

import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import com.mojang.blaze3d.platform.InputConstants;
import de.doomedartemis.couponcodes.common.network.SortCouponPouchPayload;
import de.doomedartemis.couponcodes.common.network.ToggleCouponPouchAutoActivationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class CouponPouchScreen extends AbstractContainerScreen<CouponPouchMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation AUTO_ACTIVATION_TAB =
            ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1");
    private static final ResourceLocation SORT_TAB =
            ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2");
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;

    public CouponPouchScreen(CouponPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageHeight = 114 + CouponPouchMenu.ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderTabTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || ClientModEvents.matchesOpenCouponPouchKey(keyCode, scanCode)) {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isHoveringTab(0, mouseX, mouseY)) {
                playTabClickSound();
                PacketDistributor.sendToServer(ToggleCouponPouchAutoActivationPayload.INSTANCE);
                return true;
            }
            if (isHoveringTab(1, mouseX, mouseY)) {
                playTabClickSound();
                PacketDistributor.sendToServer(SortCouponPouchPayload.INSTANCE);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        renderTabs(guiGraphics, left, top);
        int pouchHeight = CouponPouchMenu.ROWS * 18 + 17;
        guiGraphics.blit(CONTAINER_BACKGROUND, left, top, 0, 0, imageWidth, pouchHeight);
        guiGraphics.blit(CONTAINER_BACKGROUND, left, top + pouchHeight, 0, 126, imageWidth, 96);
    }

    private void renderTabs(GuiGraphics guiGraphics, int left, int top) {
        renderTab(guiGraphics, AUTO_ACTIVATION_TAB, left + tabX(0), top - 28, autoActivationIcon());
        renderTab(guiGraphics, SORT_TAB, left + tabX(1), top - 28, new ItemStack(Items.BRUSH));
    }

    private void renderTab(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, ItemStack icon) {
        guiGraphics.blitSprite(sprite, x, y, TAB_WIDTH, TAB_HEIGHT);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        guiGraphics.renderItem(icon, x + 5, y + 9);
        guiGraphics.renderItemDecorations(font, icon, x + 5, y + 9);
        guiGraphics.pose().popPose();
    }

    private ItemStack autoActivationIcon() {
        return menu.isAutoActivationEnabled()
                ? new ItemStack(Items.GREEN_CONCRETE)
                : new ItemStack(Items.RED_CONCRETE);
    }

    private void renderTabTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHoveringTab(0, mouseX, mouseY)) {
            String state = menu.isAutoActivationEnabled() ? "on" : "off";
            guiGraphics.renderTooltip(font, Component.translatable("container.coupon_codes.coupon_pouch.auto_activation." + state + ".tooltip"), mouseX, mouseY);
        } else if (isHoveringTab(1, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("container.coupon_codes.coupon_pouch.sort.tooltip"), mouseX, mouseY);
        }
    }

    private boolean isHoveringTab(int index, double mouseX, double mouseY) {
        int x = leftPos + tabX(index);
        int y = topPos - 28;
        return mouseX >= x && mouseX <= x + TAB_WIDTH && mouseY >= y && mouseY <= y + TAB_HEIGHT;
    }

    private int tabX(int index) {
        return 27 * index;
    }

    private void playTabClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
