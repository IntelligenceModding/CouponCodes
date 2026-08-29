package de.doomedartemis.couponcodes.common.network;

import de.doomedartemis.couponcodes.CouponCodes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenCouponPouchPayload() implements CustomPacketPayload {
    public static final OpenCouponPouchPayload INSTANCE = new OpenCouponPouchPayload();
    public static final Type<OpenCouponPouchPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CouponCodes.MOD_ID, "open_coupon_pouch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCouponPouchPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
