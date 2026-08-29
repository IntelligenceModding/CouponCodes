package de.doomedartemis.couponcodes.client;

import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import com.mojang.blaze3d.platform.InputConstants;
import de.doomedartemis.couponcodes.common.network.SortCouponPouchPayload;
import de.doomedartemis.couponcodes.common.network.ToggleCouponPouchAutoActivationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class CouponPouchScreen extends AbstractContainerScreen<CouponPouchMenu> {
    private static final Identifier CONTAINER_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier AUTO_ACTIVATION_TAB =
            Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1");
    private static final Identifier SORT_TAB =
            Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2");
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;

    public CouponPouchScreen(CouponPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 114 + CouponPouchMenu.ROWS * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        extractTabTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.isEscape() || ClientModEvents.matchesOpenCouponPouchKey(keyEvent)) {
            onClose();
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseEvent, boolean doubleClick) {
        double mouseX = mouseEvent.x();
        double mouseY = mouseEvent.y();
        if (mouseEvent.button() == 0) {
            if (isHoveringTab(0, mouseX, mouseY)) {
                playTabClickSound();
                ClientPacketDistributor.sendToServer(ToggleCouponPouchAutoActivationPayload.INSTANCE);
                return true;
            }
            if (isHoveringTab(1, mouseX, mouseY)) {
                playTabClickSound();
                ClientPacketDistributor.sendToServer(SortCouponPouchPayload.INSTANCE);
                return true;
            }
        }

        return super.mouseClicked(mouseEvent, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int left = this.leftPos;
        int top = this.topPos;
        extractTabs(guiGraphics, left, top);
        int pouchHeight = CouponPouchMenu.ROWS * 18 + 17;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, left, top, 0, 0, imageWidth, pouchHeight, 256, 256);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, left, top + pouchHeight, 0, 126, imageWidth, 96, 256, 256);
    }

    private void extractTabs(GuiGraphicsExtractor guiGraphics, int left, int top) {
        extractTab(guiGraphics, AUTO_ACTIVATION_TAB, left + tabX(0), top - 28, autoActivationIcon());
        extractTab(guiGraphics, SORT_TAB, left + tabX(1), top - 28, new ItemStack(Items.BRUSH));
    }

    private void extractTab(GuiGraphicsExtractor guiGraphics, Identifier sprite, int x, int y, ItemStack icon) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TAB_WIDTH, TAB_HEIGHT);
        guiGraphics.nextStratum();
        guiGraphics.item(icon, x + 5, y + 9);
        guiGraphics.itemDecorations(font, icon, x + 5, y + 9);
    }

    private ItemStack autoActivationIcon() {
        return menu.isAutoActivationEnabled()
                ? new ItemStack(Items.CONCRETE.pick(DyeColor.GREEN))
                : new ItemStack(Items.CONCRETE.pick(DyeColor.RED));
    }

    private void extractTabTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (isHoveringTab(0, mouseX, mouseY)) {
            String state = menu.isAutoActivationEnabled() ? "on" : "off";
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("container.coupon_codes.coupon_pouch.auto_activation." + state + ".tooltip"), mouseX, mouseY);
        } else if (isHoveringTab(1, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("container.coupon_codes.coupon_pouch.sort.tooltip"), mouseX, mouseY);
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
