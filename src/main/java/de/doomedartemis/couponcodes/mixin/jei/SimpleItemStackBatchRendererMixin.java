package de.doomedartemis.couponcodes.mixin.jei;

import de.doomedartemis.couponcodes.common.item.CouponItem;
import mezz.jei.library.render.batch.SimpleItemStackBatchRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(value = SimpleItemStackBatchRenderer.class, remap = false)
public abstract class SimpleItemStackBatchRendererMixin {
    @Redirect(
            method = "renderBatch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V",
                    remap = true
            ),
            remap = false
    )
    private void couponcodes$renderFakeItemWithCouponDecorations(
            GuiGraphics guiGraphics,
            ItemStack stack,
            int x,
            int y
    ) {
        guiGraphics.renderFakeItem(stack, x, y);
        if (stack.getItem() instanceof CouponItem) {
            Font font = Minecraft.getInstance().font;
            guiGraphics.renderItemDecorations(font, stack, x, y);
        }
    }
}
