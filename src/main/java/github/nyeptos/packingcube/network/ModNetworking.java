package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.menu.PouchStationMenu;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("6");
        registerOpening(registrar);
        registerRadial(registrar);
        registerBagActions(registrar);
        registerPouchStation(registrar);
    }

    /** The pouch station screen only asks; every check happens on the server side. */
    private static void registerPouchStation(PayloadRegistrar registrar) {
        registrar.playToServer(
                PouchStationActionPayload.TYPE,
                PouchStationActionPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(
                                () -> {
                                    var player = context.player();
                                    if (!(player.containerMenu instanceof PouchStationMenu menu))
                                        return;
                                    if (menu.containerId != payload.containerId()) return;
                                    switch (payload.action()) {
                                        case PouchStationActionPayload.ACTION_SELECT ->
                                                menu.select(payload.value());
                                        case PouchStationActionPayload.ACTION_CRAFT ->
                                                menu.craft(player);
                                        default -> {}
                                    }
                                }));
    }

    /** Opening the equipped bag, the big bag, and the moves between the two screens. */
    private static void registerOpening(PayloadRegistrar registrar) {
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
    }

    /** The radial slot pick. A refused selection closes the screen. */
    private static void registerRadial(PayloadRegistrar registrar) {
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
    }

    /** Stowing everything and the filter actions the open bag screen sends. */
    private static void registerBagActions(PayloadRegistrar registrar) {
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
