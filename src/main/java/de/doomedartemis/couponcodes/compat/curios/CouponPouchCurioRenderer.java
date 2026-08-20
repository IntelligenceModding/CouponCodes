package de.doomedartemis.couponcodes.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

final class CouponPouchCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource buffer,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        LivingEntity wearer = slotContext.entity();
        poseStack.pushPose();
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.body.translateAndRotate(poseStack);
        } else {
            ICurioRenderer.rotateIfSneaking(poseStack, wearer);
        }
        ICurioRenderer.translateIfSneaking(poseStack, wearer);

        poseStack.translate(0.26D, 0.55D, 0.04D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(186.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                wearer.level(),
                wearer.getId()
        );
        poseStack.popPose();
    }
}
