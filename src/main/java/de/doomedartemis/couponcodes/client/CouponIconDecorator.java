package de.doomedartemis.couponcodes.client;

import de.doomedartemis.couponcodes.common.coupon.CouponEffectType;
import de.doomedartemis.couponcodes.common.item.CouponItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.IItemDecorator;

public class CouponIconDecorator implements IItemDecorator {
    private static final float ICON_SCALE = 0.625F;
    private static final int ICON_OFFSET_X = 6;
    private static final int ICON_OFFSET_Y = 0;

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (!ClientConfig.showCouponIconOverlays()) {
            return false;
        }

        if (!(stack.getItem() instanceof CouponItem coupon)) {
            return false;
        }

        guiGraphics.nextStratum();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(xOffset + ICON_OFFSET_X, yOffset + ICON_OFFSET_Y);
        guiGraphics.pose().scale(ICON_SCALE, ICON_SCALE);
        guiGraphics.renderItem(new ItemStack(icon(coupon.effect())), 0, 0);
        guiGraphics.pose().popMatrix();
        return true;
    }

    private static Item icon(CouponEffectType effect) {
        return switch (effect) {
            case DURABILITY -> Items.DIAMOND_PICKAXE;
            case ENCHANTING_EXPERIENCE -> Items.EXPERIENCE_BOTTLE;
            case ANVIL_EXPERIENCE -> Items.ANVIL;
            case TOOL_REPAIR -> Items.GRINDSTONE;
            case VILLAGER_TRADE -> Items.EMERALD;
            case VILLAGER_RESTOCK -> Items.CLOCK;
            case ARROW -> Items.ARROW;
            case FOOD -> Items.BREAD;
            case POTION_DURATION -> Items.POTION;
            case MENDING -> Items.ENCHANTED_BOOK;
            case TOTEM -> Items.TOTEM_OF_UNDYING;
            case SMITHING_TEMPLATE -> Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
            case REPAIR_MATERIAL -> Items.DIAMOND;
            case BONE_MEAL -> Items.BONE_MEAL;
            case FISHING -> Items.FISHING_ROD;
            case ROCKET -> Items.FIREWORK_ROCKET;
            case ENDER_PEARL -> Items.ENDER_PEARL;
            case ELYTRA_GLIDE -> Items.ELYTRA;
            case FALL_DAMAGE -> Items.FEATHER;
            case DEATH_DROP -> Items.CHEST;
        };
    }
}
