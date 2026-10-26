package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("6");
        registrar.playToServer(
                OpenEquippedBagPayload.TYPE,
                OpenEquippedBagPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> EquippedBagAccess.open(context.player())));
        registrar.playToServer(
                OpenBigBagPayload.TYPE,
                OpenBigBagPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> EquippedBagAccess.openDirect(context.player())));
        registrar.playToServer(
                BagNavigationPayload.TYPE,
                BagNavigationPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(
                                () ->
                                        EquippedBagAccess.navigate(
                                                context.player(),
                                                payload.containerId(),
                                                payload.radial())));
        registrar.playToServer(
                RadialSelectPayload.TYPE,
                RadialSelectPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(
                                () -> {
                                    var player = context.player();
                                    if (player.containerMenu instanceof BagRadialMenu menu
                                            && menu.containerId == payload.containerId()) {
                                        if (!menu.select(player, payload.moduleSlot()))
                                            player.closeContainer();
                                    }
                                }));
        registrar.playToServer(
                BagStowPayload.TYPE,
                BagStowPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(
                                () -> {
                                    var player = context.player();
                                    if (player.containerMenu instanceof BagMenu menu)
                                        menu.stowAll(player, payload.containerId());
                                }));
        registrar.playToServer(
                FilterActionPayload.TYPE,
                FilterActionPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(
                                () -> {
                                    var player = context.player();
                                    if (player.containerMenu instanceof BagMenu menu
                                            && menu.containerId == payload.containerId()) {
                                        menu.editFilter(
                                                player,
                                                payload.operation(),
                                                payload.sampleSlot(),
                                                payload.field(),
                                                payload.value());
                                    }
                                }));
    }
}
