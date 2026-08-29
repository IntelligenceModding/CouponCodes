package de.doomedartemis.couponcodes.common.network;

import de.doomedartemis.couponcodes.CouponCodes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleCouponPouchAutoActivationPayload() implements CustomPacketPayload {
    public static final ToggleCouponPouchAutoActivationPayload INSTANCE = new ToggleCouponPouchAutoActivationPayload();
    public static final Type<ToggleCouponPouchAutoActivationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CouponCodes.MOD_ID, "toggle_coupon_pouch_auto_activation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCouponPouchAutoActivationPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
