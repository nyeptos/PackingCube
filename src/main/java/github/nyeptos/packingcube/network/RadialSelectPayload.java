package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RadialSelectPayload(int containerId, int moduleSlot) implements CustomPacketPayload {
    public static final Type<RadialSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Packingcube.MODID, "radial_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadialSelectPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RadialSelectPayload::containerId,
                    ByteBufCodecs.VAR_INT,
                    RadialSelectPayload::moduleSlot,
                    RadialSelectPayload::new);

    @Override
    public Type<RadialSelectPayload> type() {
        return TYPE;
    }
}
