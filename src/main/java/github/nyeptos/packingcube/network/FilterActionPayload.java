package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Filter editor request. Stable operation IDs are defined in {@link FilterActions}. */
public record FilterActionPayload(
        int containerId, int operation, int sampleSlot, String field, String value)
        implements CustomPacketPayload {
    public static final Type<FilterActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Packingcube.MODID, "filter_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    FilterActionPayload::containerId,
                    ByteBufCodecs.VAR_INT,
                    FilterActionPayload::operation,
                    ByteBufCodecs.VAR_INT,
                    FilterActionPayload::sampleSlot,
                    ByteBufCodecs.stringUtf8(16),
                    FilterActionPayload::field,
                    ByteBufCodecs.stringUtf8(256),
                    FilterActionPayload::value,
                    FilterActionPayload::new);

    @Override
    public Type<FilterActionPayload> type() {
        return TYPE;
    }
}
