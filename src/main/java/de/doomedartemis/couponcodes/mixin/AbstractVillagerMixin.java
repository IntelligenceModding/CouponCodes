package de.doomedartemis.couponcodes.mixin;

import de.doomedartemis.couponcodes.common.trade.CouponTradeDataManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    @Inject(method = "addOffersFromTradeSet", at = @At("TAIL"))
    private void couponcodes$addCouponTrades(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> tradeSet, CallbackInfo ci) {
        CouponTradeDataManager.addOffersForTradeSet(level, (AbstractVillager) (Object) this, offers, tradeSet);
    }
}
