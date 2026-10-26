package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Scoped to the currently open bag, so stale/repeated requests cannot affect its replacement. */
public record BagNavigationPayload(int containerId, boolean radial) implements CustomPacketPayload {
    public static final Type<BagNavigationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Packingcube.MODID, "bag_navigation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BagNavigationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    BagNavigationPayload::containerId,
                    ByteBufCodecs.BOOL,
                    BagNavigationPayload::radial,
                    BagNavigationPayload::new);

    @Override
    public Type<BagNavigationPayload> type() {
        return TYPE;
    }
}
