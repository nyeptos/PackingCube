package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.PackingCube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenEquippedBagPayload() implements CustomPacketPayload {
    public static final Type<OpenEquippedBagPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, "open_equipped_bag"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEquippedBagPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenEquippedBagPayload());

    @Override
    public Type<OpenEquippedBagPayload> type() {
        return TYPE;
    }
}
