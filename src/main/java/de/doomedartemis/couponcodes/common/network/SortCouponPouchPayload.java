package de.doomedartemis.couponcodes.common.network;

import de.doomedartemis.couponcodes.CouponCodes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SortCouponPouchPayload() implements CustomPacketPayload {
    public static final SortCouponPouchPayload INSTANCE = new SortCouponPouchPayload();
    public static final Type<SortCouponPouchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CouponCodes.MOD_ID, "sort_coupon_pouch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortCouponPouchPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
