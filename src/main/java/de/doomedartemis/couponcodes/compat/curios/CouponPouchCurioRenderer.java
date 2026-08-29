package de.doomedartemis.couponcodes.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

final class CouponPouchCurioRenderer implements ICurioRenderer {
    @Override
    public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int light,
            S renderState,
            RenderLayerParent<S, M> renderLayerParent,
            EntityRendererProvider.Context context,
            float yRotation,
            float xRotation
    ) {
        LivingEntity wearer = slotContext.entity();
        poseStack.pushPose();
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.body.translateAndRotate(poseStack);
        }
        if (renderState.hasPose(Pose.CROUCHING)) {
            poseStack.translate(0.0F, 0.1875F, 0.0F);
        }

        poseStack.translate(0.26D, 0.55D, 0.04D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(186.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);

        ItemStackRenderState itemRenderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(
                itemRenderState,
                stack,
                ItemDisplayContext.FIXED,
                wearer
        );
        itemRenderState.submit(poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
    }
}
