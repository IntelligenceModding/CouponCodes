package de.doomedartemis.couponcodes.client;

import de.doomedartemis.couponcodes.common.menu.CouponPouchMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CouponPouchScreen extends AbstractContainerScreen<CouponPouchMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

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
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        int pouchHeight = CouponPouchMenu.ROWS * 18 + 17;
        guiGraphics.blit(CONTAINER_BACKGROUND, left, top, 0, 0, imageWidth, pouchHeight);
        guiGraphics.blit(CONTAINER_BACKGROUND, left, top + pouchHeight, 0, 126, imageWidth, 96);
    }
}
