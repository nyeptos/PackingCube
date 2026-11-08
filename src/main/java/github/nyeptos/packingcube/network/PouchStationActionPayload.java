package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.PackingCube;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One verb from the station screen: pick a pouch or press craft. */
public record PouchStationActionPayload(int containerId, int action, int value)
        implements CustomPacketPayload {
    public static final int ACTION_SELECT = 0;
    public static final int ACTION_CRAFT = 1;

    public static final Type<PouchStationActionPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            PackingCube.MODID, "pouch_station_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PouchStationActionPayload>
            STREAM_CODEC =
                    StreamCodec.composite(
                            ByteBufCodecs.VAR_INT,
                            PouchStationActionPayload::containerId,
                            ByteBufCodecs.VAR_INT,
                            PouchStationActionPayload::action,
                            ByteBufCodecs.VAR_INT,
                            PouchStationActionPayload::value,
                            PouchStationActionPayload::new);

    @Override
    public Type<PouchStationActionPayload> type() {
        return TYPE;
    }
}
