package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Direct management screen request, independent of the radial-menu key. */
public record OpenBigBagPayload() implements CustomPacketPayload {
    public static final Type<OpenBigBagPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Packingcube.MODID, "open_big_bag"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBigBagPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenBigBagPayload());

    @Override
    public Type<OpenBigBagPayload> type() {
        return TYPE;
    }
}
