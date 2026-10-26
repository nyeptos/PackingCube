package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The server resolves every source and destination from the current backpack menu. */
public record BagStowPayload(int containerId) implements CustomPacketPayload {
    public static final Type<BagStowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Packingcube.MODID, "bag_stow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BagStowPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BagStowPayload::containerId, BagStowPayload::new);

    @Override
    public Type<BagStowPayload> type() {
        return TYPE;
    }
}
